# ARC Publisher

Microservice de planification et de publication de commandes de provisioning de la plateforme [Instant Access Provider](https://github.com/arckit-iam/InstantAccessPublisher).

Il reçoit des **jobs de provisioning** via REST, les persiste dans PostgreSQL, puis les publie sur Kafka à l'heure prévue. Les agents (`arc-agent-ad`, etc.) consomment ces commandes et les exécutent sur les systèmes cibles.

## Flux

```text
Service métier (ex : profile) ──► POST /jobs ──► arc-publisher (PostgreSQL)
                                                     │  à executeAt (NOTIFY/LISTEN + scheduler)
                                                     ▼
                                   Kafka : arc.provisioning.{identity|assignment|entitlement}
                                                     │
                                                     ▼
                                   Agents (arc-agent-ad, arc-agent-{cible}...)
```

Le topic de publication dépend du champ `targetType` du job :

| `targetType` | Topic par défaut |
|---|---|
| `IDENTITY` | `arc.provisioning.identity` |
| `ASSIGNMENT` | `arc.provisioning.assignment` |
| `ENTITLEMENT` | `arc.provisioning.entitlement` |

La clé du message Kafka est `<identityName>_<targetSystem>`.

## Clients

| Service | Usage |
|---|---|
| [`profile`](https://github.com/arckit-iam/product-manager) (product-manager) | Crée ou supprime le groupe AD associé à un profil produit : envoie un job `CREATE` ou `DELETE` de type `ENTITLEMENT` via `POST /jobs`. L'événement est publié sur `arc.provisioning.entitlement` et consommé par `arc-agent-ad`. |

```text
profile (POST /products/{productName}/profiles)
        │  POST /jobs  (action=CREATE, targetType=ENTITLEMENT)
        ▼
arc-publisher ──► Kafka : arc.provisioning.entitlement
                          │
                          ▼
                   arc-agent-ad ──► Active Directory (création du groupe)
```

## Architecture hexagonale

```text
domain/
  model/    Job, JobAction (CREATE / MODIFY / DELETE), JobTargetType (IDENTITY / ASSIGNMENT / ENTITLEMENT),
            JobStatus (PENDING / EXECUTING / DONE / FAILED / CANCELLED)
  port/     JobRepository, ProvisioningEventPort
  service/  JobSchedulerService : logique de déclenchement, sans dépendance Spring

infrastructure/
  adapter/      JobRepositoryImpl (JPA), KafkaProvisioningPublisher, SchedulerAdapter (@Scheduled),
                PgNotifyListener (PostgreSQL LISTEN/NOTIFY : réveil instantané)
  config/       KafkaProducerConfig, DomainConfig
  persistence/  JobEntity, SpringJobRepository

application/
  service/  JobApplicationService
  api/      JobController (REST)

dto/        CreateJobRequest, IdentityEventDTO, AssignmentEventDTO, EntitlementEventDTO
```

## API REST

Toutes les routes exigent un jeton `Authorization: Bearer <jwt>` émis par [arc-auth](https://github.com/arckit-iam/arc-auth), sauf `/actuator/health` (public, sondes). Sans jeton valide : `401`. Avec un rôle insuffisant : `403`.

| Méthode | Endpoint | Rôle requis | Description |
|---|---|---|---|
| `POST` | `/jobs` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Crée un job de provisioning (retourne son identifiant) |
| `GET` | `/jobs` | `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` | Liste les jobs |
| `GET` | `/jobs/{id}` | `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` | Retourne un job |
| `DELETE` | `/jobs/{id}` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Annule un job (si `PENDING`) |

Le jeton est validé localement (signature via les clés publiques d'arc-auth, `iss`, `aud`, expiration) ; les rôles sont lus dans le claim `roles`.

### Exemple de payload `POST /jobs`

```json
{
  "action": "CREATE",
  "targetType": "ENTITLEMENT",
  "identityId": "S-1-5-21-123456-789",
  "identityName": "jdupont",
  "targetSystem": "ad://corp.example.com",
  "entitlementId": "CN=GRP-FIN,OU=Groups,DC=corp,DC=example,DC=com",
  "entitlementName": "GRP-FIN",
  "scope": "EMEA",
  "source": "product-manager",
  "attributes": {
    "ou": "Finance"
  },
  "executeAt": "2026-06-01T08:00:00+02:00",
  "correlationId": "uuid-source-event"
}
```

Champs obligatoires : `action`, `targetType`, `identityId`, `identityName`, `targetSystem`, `executeAt`.

## Événements Kafka publiés

Chaque topic porte un DTO dédié (`IdentityEventDTO`, `AssignmentEventDTO`, `EntitlementEventDTO`). Tous contiennent `jobId`, `correlationId`, `system`, `timestamp`, `eventType`, `source`, `changedAttributes` ; le détail de l'objet varie selon le type (identité, droit, ou couple identité/droit avec `scope`).

## Déclenchement des jobs

Deux mécanismes combinés pour une latence minimale :

1. **PostgreSQL NOTIFY/LISTEN** : un trigger émet `pg_notify('job_created', ...)` à chaque insertion. `PgNotifyListener` réveille immédiatement le scheduler.
2. **`@Scheduled` (1 s)** : filet de sécurité en cas de perte de la connexion NOTIFY.

## Configuration (variables d'environnement)

| Variable | Valeur par défaut | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/arc_publisher` | URL JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `arc` | Utilisateur PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | `arc` | Mot de passe (secret Kubernetes en production) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Broker Kafka |
| `ARC_TOPICS_PROVISIONING_IDENTITY` | `arc.provisioning.identity` | Topic des identités |
| `ARC_TOPICS_PROVISIONING_ASSIGNMENT` | `arc.provisioning.assignment` | Topic des affectations |
| `ARC_TOPICS_PROVISIONING_ENTITLEMENT` | `arc.provisioning.entitlement` | Topic des droits |
| `SERVER_PORT` | `8081` | Port HTTP |
| `ARC_SECURITY_ISSUER_URI` | aucune (obligatoire) | Valeur attendue du claim `iss` (doit égaler `ARC_AUTH_JWT_ISSUER` d'arc-auth) |
| `ARC_SECURITY_JWK_SET_URI` | `http://localhost:8082/.well-known/jwks.json` | Clés publiques d'arc-auth (récupérées à la demande, mises en cache) |
| `ARC_SECURITY_AUDIENCE` | `arc` | Valeur attendue du claim `aud` |

Les valeurs par défaut (`arc` / `arc`) sont réservées au développement local.

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

Sur Kubernetes, ce script est embarqué dans un ConfigMap du dépôt GitOps privé de déploiement et exécuté uniquement à la première création de la base. Le fichier du projet reste la source de vérité : toute modification du schéma doit y être faite puis recopiée dans le ConfigMap.

## Développement

Prérequis : JDK 25 et Maven 3.9.

```bash
mvn test
```

Les tests unitaires du domaine (`JobSchedulerServiceTest`) sont isolés de Spring, JPA et Kafka : ils utilisent des mocks Mockito.

## Déploiement (Kubernetes / FluxCD)

Le déploiement GitOps (Helm, FluxCD) est géré dans un dépôt privé. L'intégration et la mise en production de la plateforme sont proposées par Dédale Solutions en **service payant** : pour en discuter, contactez-nous à [etiennsalle@gmail.com](mailto:etiennsalle@gmail.com).

Secret requis en amont dans le cluster :

```bash
kubectl create secret generic arc-publisher-db-secret \
  --from-literal=username=arc \
  --from-literal=password=<mot-de-passe-postgres>
```

## Pipeline CI/CD

| Workflow | Déclencheur | Action |
|---|---|---|
| `build.yml` | Push d'un tag `vX.Y.Z` | Tests, build Docker, image `-dev` |
| `promote.yml` | Pre-release GitHub | Promotion `-dev` vers `-rc` |
| `promote.yml` | Release GitHub | Promotion `-rc` vers stable et déploiement |

## Documentation

- [arc-docs](https://github.com/arckit-iam/arc-docs) : architecture, modèle abstrait IAM, publication et messaging.

## Contribuer et sécurité

Voir [CONTRIBUTING.md](CONTRIBUTING.md) et [SECURITY.md](SECURITY.md).

## Licence

Copyright © 2026 Dédale Solutions

Ce projet est distribué sous la licence [EUPL-1.2](LICENSE) (European Union Public Licence v. 1.2).
