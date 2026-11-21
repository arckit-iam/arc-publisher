# ARC Publisher

Microservice IAM de planification et publication d'événements de provisioning.

Reçoit des **jobs de provisioning** (via REST), les persiste en PostgreSQL, et les publie sur le topic Kafka `arc.provisioning` à l'heure prévue. Les services techniques consommateurs (AD, LDAP, applications cibles) écoutent ce topic pour exécuter les opérations concrètes.

## Flux

```
Service métier (ex : profile) → POST /jobs → arc-publisher (PostgreSQL)
                                    ↓ (à executeAt, via NOTIFY/LISTEN PostgreSQL)
                             Kafka topic: arc.provisioning
                                    ↓
                    Services techniques (AD, LDAP, SaaS apps...)
```

## Clients

| Service | Usage |
|---|---|
| [`profile`](https://github.com/arckit-iam/product-manager) (product-manager) | Crée ou supprime le groupe AD associé à un profil produit : envoie un job `CREATE` ou `DELETE` de type `ENTITLEMENT` via `POST /jobs`. L'événement est publié sur `arc.provisioning.entitlement` et consommé par `arc-agent-ad`. |

Exemple de flux :

```
profile (POST /products/{productName}/profiles)
        │  POST /jobs  (action=CREATE, targetType=ENTITLEMENT)
        ▼
arc-publisher ──► Kafka : arc.provisioning.entitlement
                          │
                          ▼
                   arc-agent-ad ──► Active Directory (création du groupe)
```

## Architecture hexagonale

```
domain/
  model/    Job, JobAction (PROVISION/DEPROVISION/ASSIGN/UNASSIGN/MODIFY), JobStatus
  port/     JobRepository, ProvisioningEventPort
  service/  JobSchedulerService — logique de déclenchement, sans dépendance Spring

infrastructure/
  adapter/  JobRepositoryImpl (JPA), KafkaProvisioningPublisher, SchedulerAdapter (@Scheduled),
            PgNotifyListener (PostgreSQL LISTEN/NOTIFY — réveil instantané)
  config/   KafkaProducerConfig, DomainConfig
  persistence/ JobEntity, SpringJobRepository

application/
  service/  JobApplicationService
  api/      JobController (REST)

dto/        CreateJobRequest, ProvisioningEventDTO
```

## API REST

| Méthode | Endpoint      | Description                    |
|---------|---------------|--------------------------------|
| POST    | `/jobs`       | Crée un job de provisioning    |
| DELETE  | `/jobs/{id}`  | Annule un job (si PENDING)     |

### Exemple de payload POST /jobs

```json
{
  "action": "PROVISION",
  "identityId": "S-1-5-21-123456-789",
  "identityName": "jdupont",
  "targetSystem": "active-directory",
  "entitlementId": "CN=GRP-FIN,OU=Groups,DC=corp,DC=local",
  "entitlementName": "GRP-FIN",
  "scope": "EMEA",
  "attributes": {
    "ou": "Finance",
    "manager": "jmartin"
  },
  "executeAt": "2026-06-01T08:00:00+02:00",
  "correlationId": "uuid-source-event"
}
```

## Event Kafka publié (arc.provisioning)

```json
{
  "jobId": "uuid",
  "action": "PROVISION",
  "identityId": "S-1-5-21-123456-789",
  "identityName": "jdupont",
  "targetSystem": "active-directory",
  "entitlementId": "CN=GRP-FIN,...",
  "entitlementName": "GRP-FIN",
  "scope": "EMEA",
  "attributes": { "ou": "Finance" },
  "executeAt": "2026-06-01T08:00:00+02:00",
  "correlationId": "uuid-source-event"
}
```

## Déclenchement des jobs

Deux mécanismes combinés pour une latence minimale :

1. **PostgreSQL NOTIFY/LISTEN** — un trigger DB émet `pg_notify('job_created', ...)` à chaque insertion. `PgNotifyListener` réveille immédiatement le scheduler (~0ms de latence).
2. **@Scheduled(1s)** — filet de sécurité en cas de perte de connexion NOTIFY.

## Configuration (variables d'environnement)

| Variable                    | Valeur par défaut                                    | Description                    |
|-----------------------------|------------------------------------------------------|--------------------------------|
| `SPRING_DATASOURCE_URL`     | `jdbc:postgresql://localhost:5432/arc_publisher`     | URL JDBC PostgreSQL            |
| `SPRING_DATASOURCE_USERNAME`| `arc`                                                | Utilisateur PostgreSQL         |
| `SPRING_DATASOURCE_PASSWORD`| `arc`                                                | Mot de passe (Secret K8s)      |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092`                                | Broker Kafka                   |
| `ARC_TOPICS_PROVISIONING`   | `arc.provisioning`                                   | Topic Kafka cible              |
| `SERVER_PORT`               | `8081`                                               | Port HTTP                      |

## Initialisation de la base de données

Exécuter le script SQL avant le premier démarrage :

```bash
psql -U arc -d arc_publisher -f src/main/resources/db/init.sql
```

Ce script crée :
- La table `provisioning_jobs`
- La table `provisioning_job_attributes` (attributs clé/valeur)
- Un index sur `(status, execute_at)` pour les requêtes de jobs dus
- Le trigger PostgreSQL NOTIFY sur insertion

Sur Kubernetes, ce script est embarqué dans un ConfigMap du dépôt `arc-kubeorchestrator` (`arc-publisher/postgres/configmap-init.yaml`) et exécuté uniquement à la première création de la base. Le fichier du projet reste la source de vérité : toute modification du schéma doit y être faite puis recopiée dans le ConfigMap.

## Tests

```bash
mvn test
```

Les tests unitaires du domaine (`JobSchedulerServiceTest`) sont 100% isolés de Spring, JPA et Kafka — ils utilisent des mocks Mockito.

## Déploiement (Kubernetes / FluxCD)

Le projet utilise Helm + FluxCD pour le déploiement continu. Le chart Helm et les manifestes FluxCD sont dans le dépôt [arc-kubeorchestrator](https://github.com/arckit-iam/arc-kubeorchestrator) (`arc-publisher/` et `flux/arc-publisher/`).

**Secret requis en amont dans le cluster :**

```bash
kubectl create secret generic arc-publisher-db-secret \
  --from-literal=password=<mot-de-passe-postgres>
```

## Pipeline CI/CD

| Workflow           | Déclencheur              | Action                              |
|--------------------|--------------------------|-------------------------------------|
| `build.yml`        | Push tag `vX.Y.Z`        | Build Docker → image `-dev`         |
| `promote.yml`      | Pre-release GitHub       | Promote `-dev` → `-rc`              |
| `promote.yml`      | Release GitHub           | Promote `-rc` → `stable` + deploy  |
| `bump-helmrelease.yml` | Release GitHub       | PR auto pour bumper le tag FluxCD  |

Maintenant un commit auto est créé au build pour deploy dev

## Documentation

La documentation d'architecture de la plateforme est dans le dépôt [arc-docs](https://github.com/arckit-iam/arc-docs) :

- [Publication et messaging](https://github.com/arckit-iam/arc-docs/blob/main/docs/05-publication-messaging.md) : topologie des topics et schémas d'événements.
- [Architecture globale](https://github.com/arckit-iam/arc-docs/blob/main/docs/02-architecture-globale.md).

Le déploiement (Helm, Flux, CI/CD) est documenté dans [arc-kubeorchestrator](https://github.com/arckit-iam/arc-kubeorchestrator/tree/main/docs).

## Licence

Copyright © 2026 Dédale Solutions

Licensed under the EUPL

Ce projet est distribué sous la licence [EUPL-1.2](LICENSE) (European Union Public Licence v. 1.2).
