# ARC Publisher

Microservice de planification et de publication de commandes de provisioning de la plateforme [Instant Access Provider](https://github.com/arckit-iam/InstantAccessPublisher).

Il reçoit des **orders de provisioning** (par REST et/ou par une file Kafka alimentée par les managers), les persiste dans PostgreSQL, puis les publie sur Kafka à l'heure prévue. Les agents (`arc-agent-ad`, etc.) consomment ces commandes et les exécutent sur les systèmes cibles.

## Flux

```text
Service métier (ex : profile) ──► POST /orders  ┐  (mode api)
Managers (identity / product)  ──► arc.orders   ┤  (mode kafka)
                                                ▼
                                      arc-publisher (PostgreSQL)
                                                     │  à executeAt (NOTIFY/LISTEN + scheduler)
                                                     ▼
                                   Kafka : arc.provisioning.{identity|assignment|entitlement}
                                                     │
                                                     ▼
                                   Agents (arc-agent-ad, arc-agent-{cible}...)
```

Le topic de publication dépend du champ `targetType` du order :

| `targetType` | Topic par défaut |
|---|---|
| `IDENTITY` | `arc.provisioning.identity` |
| `ASSIGNMENT` | `arc.provisioning.assignment` |
| `ENTITLEMENT` | `arc.provisioning.entitlement` |

La clé du message Kafka est `<identityName>_<targetSystem>`.

## Modes de réception des orders

`ARC_PUBLISHER_MODE` choisit la ou les portes d'entrée (la persistance et le scheduler sont communs) :

| Mode | Comportement |
|---|---|
| `api` (défaut) | Seule l'API REST `/orders` accepte les orders. |
| `kafka` | Seul le topic d'orders (`arc.orders`) est consommé ; l'API REST est désactivée. |
| `both` | Les deux. |

### File d'orders (`arc.orders`)

Les managers publient des messages JSON à plat, la clé Kafka étant l'identifiant de l'order (UUID généré par le manager) :

- `operation: "SUBMIT"` : mêmes champs que `POST /orders` (voir ci-dessous) plus `id`. L'ordre est idempotent : rejouer un message ne crée pas de doublon.
- `operation: "CANCEL"` : seuls `operation` et `id`. Annule l'order s'il est encore `PENDING` ; sans effet s'il est déjà traité ou inconnu.

Un message invalide est écarté vers `arc.orders.DLT`. L'ordre est respecté par clé : un `CANCEL` reçu avant l'exécution (`executeAt` dans le futur) empêche la publication.

Les managers ne connaissent pas l'emplacement cible des comptes : ils envoient `accountType` (ex : `standard`, `admin`) et `scope`, et l'agent résout lui-même la branche (OU) à partir de sa configuration.

## Clients

| Service | Usage |
|---|---|
| [`profile`](https://github.com/arckit-iam/product-manager) (product-manager) | Crée ou supprime le groupe AD associé à un profil produit : envoie un order `CREATE` ou `DELETE` de type `ENTITLEMENT` via `POST /orders`. L'événement est publié sur `arc.provisioning.entitlement` et consommé par `arc-agent-ad`. |

```text
profile (POST /products/{productName}/profiles)
        │  POST /orders  (action=CREATE, targetType=ENTITLEMENT)
        ▼
arc-publisher ──► Kafka : arc.provisioning.entitlement
                          │
                          ▼
                   arc-agent-ad ──► Active Directory (création du groupe)
```

## Architecture hexagonale

```text
domain/
  model/    Order, OrderAction (CREATE / MODIFY / DELETE), OrderTargetType (IDENTITY / ASSIGNMENT / ENTITLEMENT),
            OrderStatus (PENDING / EXECUTING / DONE / FAILED / CANCELLED)
  port/     OrderRepository, ProvisioningEventPort
  service/  OrderSchedulerService : logique de déclenchement, sans dépendance Spring

infrastructure/
  adapter/      OrderRepositoryImpl (JPA), KafkaProvisioningPublisher, SchedulerAdapter (@Scheduled),
                PgNotifyListener (PostgreSQL LISTEN/NOTIFY : réveil instantané)
  config/       KafkaProducerConfig, DomainConfig
  persistence/  OrderEntity, SpringOrderRepository

application/
  service/  OrderApplicationService
  api/      OrderController (REST)

dto/        CreateOrderRequest, IdentityEventDTO, AssignmentEventDTO, EntitlementEventDTO
```

## API REST

Toutes les routes exigent un jeton `Authorization: Bearer <jwt>` émis par [arc-auth](https://github.com/arckit-iam/arc-auth), sauf `/actuator/health` (public, sondes). Sans jeton valide : `401`. Avec un rôle insuffisant : `403`.

| Méthode | Endpoint | Rôle requis | Description |
|---|---|---|---|
| `POST` | `/orders` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Crée un order de provisioning (retourne son identifiant) |
| `GET` | `/orders` | `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` | Liste les orders |
| `GET` | `/orders/{id}` | `ARC_READER`, `ARC_PUBLISHER` ou `ARC_ADMIN` | Retourne un order |
| `DELETE` | `/orders/{id}` | `ARC_PUBLISHER` ou `ARC_ADMIN` | Annule un order (si `PENDING`) |

Le jeton est validé localement (signature via les clés publiques d'arc-auth, `iss`, `aud`, expiration) ; les rôles sont lus dans le claim `roles`.

### Exemple de payload `POST /orders`

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

Chaque topic porte un DTO dédié (`IdentityEventDTO`, `AssignmentEventDTO`, `EntitlementEventDTO`). Tous contiennent `orderId`, `correlationId`, `system`, `timestamp`, `eventType`, `source`, `changedAttributes` ; le détail de l'objet varie selon le type (identité, droit, ou couple identité/droit avec `scope`).

## Déclenchement des orders

Deux mécanismes combinés pour une latence minimale :

1. **PostgreSQL NOTIFY/LISTEN** : un trigger émet `pg_notify('order_created', ...)` à chaque insertion. `PgNotifyListener` réveille immédiatement le scheduler.
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
| `ARC_PUBLISHER_MODE` | `api` | Réception des orders : `api`, `kafka` ou `both` |
| `ARC_ORDERS_TOPIC` | `arc.orders` | File d'orders consommée (modes `kafka` et `both`) |
| `ARC_ORDERS_GROUP_ID` | `arc-publisher` | Groupe de consommation de la file d'orders |
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
- La table `provisioning_orders`
- La table `provisioning_order_attributes` (attributs clé/valeur)
- Un index sur `(status, execute_at)` pour les requêtes de orders dus
- Le trigger PostgreSQL NOTIFY sur insertion

Sur Kubernetes, ce script est embarqué dans un ConfigMap du dépôt GitOps privé de déploiement et exécuté uniquement à la première création de la base. Le fichier du projet reste la source de vérité : toute modification du schéma doit y être faite puis recopiée dans le ConfigMap.

## Développement

Prérequis : JDK 25 et Maven 3.9.

```bash
mvn test
```

Les tests unitaires du domaine (`OrderSchedulerServiceTest`) sont isolés de Spring, JPA et Kafka : ils utilisent des mocks Mockito.

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
| `promote.yml` | Release GitHub | Promotion `-rc` vers stable |

## Documentation

- [arc-docs](https://github.com/arckit-iam/arc-docs) : architecture, modèle abstrait IAM, publication et messaging.

## Contribuer et sécurité

Voir [CONTRIBUTING.md](CONTRIBUTING.md) et [SECURITY.md](SECURITY.md).

## Licence

Copyright © 2026 Dédale Solutions

Ce projet est distribué sous la licence [EUPL-1.2](LICENSE) (European Union Public Licence v. 1.2).
