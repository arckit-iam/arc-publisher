// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Agrégat central du domaine arc-publisher.
 * Représente un job de provisioning planifié à exécuter à un instant T.
 * Aucune dépendance vers l'infrastructure ou les frameworks.
 */
public class Job {

    private final UUID id;
    private final JobTargetType targetType;
    private final JobAction action;
    private final String identityId;
    private final String identityName;
    private final String targetSystem;
    private final String entitlementId;
    private final String entitlementName;
    private final String scope;
    private final String source;
    private final Map<String, String> attributes;
    private final OffsetDateTime executeAt;
    private final OffsetDateTime createdAt;
    private final String correlationId;
    private JobStatus status;

    public Job(UUID id,
               JobTargetType targetType,
               JobAction action,
               String identityId,
               String identityName,
               String targetSystem,
               String entitlementId,
               String entitlementName,
               String scope,
               String source,
               Map<String, String> attributes,
               OffsetDateTime executeAt,
               OffsetDateTime createdAt,
               String correlationId,
               JobStatus status) {
        this.id = id;
        this.targetType = targetType;
        this.action = action;
        this.identityId = identityId;
        this.identityName = identityName;
        this.targetSystem = targetSystem;
        this.entitlementId = entitlementId;
        this.entitlementName = entitlementName;
        this.scope = scope;
        this.source = source;
        this.attributes = attributes;
        this.executeAt = executeAt;
        this.createdAt = createdAt;
        this.correlationId = correlationId;
        this.status = status;
    }

    /** Marque le job comme étant en cours d'exécution. */
    public void markExecuting() {
        this.status = JobStatus.EXECUTING;
    }

    /** Marque le job comme terminé avec succès. */
    public void markDone() {
        this.status = JobStatus.DONE;
    }

    /** Marque le job en échec. */
    public void markFailed() {
        this.status = JobStatus.FAILED;
    }

    /** Annule le job si encore en attente. */
    public void cancel() {
        if (this.status != JobStatus.PENDING) {
            throw new IllegalStateException("Only PENDING jobs can be cancelled");
        }
        this.status = JobStatus.CANCELLED;
    }

    /** Indique si le job est prêt à être exécuté maintenant. */
    public boolean isDue(OffsetDateTime now) {
        return this.status == JobStatus.PENDING && !this.executeAt.isAfter(now);
    }

    public UUID getId() { return id; }
    public JobTargetType getTargetType() { return targetType; }
    public JobAction getAction() { return action; }
    public String getIdentityId() { return identityId; }
    public String getIdentityName() { return identityName; }
    public String getTargetSystem() { return targetSystem; }
    public String getEntitlementId() { return entitlementId; }
    public String getEntitlementName() { return entitlementName; }
    public String getScope() { return scope; }
    public String getSource() { return source; }
    public Map<String, String> getAttributes() { return attributes; }
    public OffsetDateTime getExecuteAt() { return executeAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCorrelationId() { return correlationId; }
    public JobStatus getStatus() { return status; }
}
