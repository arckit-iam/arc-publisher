# .copilot-instructions.md

## Contexte du projet
Ce projet suit l'architecture hexagonale (Hexagonal Architecture / Ports & Adapters). Le code est organisé pour séparer la logique métier (domaine) des détails d'implémentation (infrastructure, frameworks, etc.).

## Rôle du microservice
**arc-publisher** planifie et publie des événements de provisioning IAM.
- Reçoit des jobs via REST (POST /jobs)
- Persiste les jobs en PostgreSQL
- Déclenche leur exécution à l'heure prévue (PostgreSQL NOTIFY/LISTEN + @Scheduled)
- Publie un event sur le topic Kafka `arc.provisioning`

## Structure
```
domain/
  model/    Job, JobAction, JobStatus
  port/     JobRepository, ProvisioningEventPort
  service/  JobSchedulerService

infrastructure/
  adapter/  JobRepositoryImpl, KafkaProvisioningPublisher, SchedulerAdapter, PgNotifyListener
  config/   KafkaProducerConfig, DomainConfig
  persistence/ JobEntity, SpringJobRepository

application/
  service/  JobApplicationService
  api/      JobController

dto/        CreateJobRequest, ProvisioningEventDTO
```

## Event Kafka publié (arc.provisioning)
```json
{
  "jobId": "uuid",
  "action": "PROVISION|DEPROVISION|ASSIGN|UNASSIGN|MODIFY",
  "identityId": "SID ou DN",
  "identityName": "sAMAccountName",
  "targetSystem": "active-directory",
  "entitlementId": "optionnel",
  "entitlementName": "optionnel",
  "scope": "optionnel",
  "attributes": { "clé": "valeur" },
  "executeAt": "ISO8601",
  "correlationId": "uuid"
}
```

## Règles
- Le domaine (model/, port/, service/) ne dépend jamais de Spring, JPA ou Kafka
- Les adapters implémentent les ports du domaine
- Les tests unitaires du domaine utilisent des mocks, sans Spring
- Les apis sont en rest.
