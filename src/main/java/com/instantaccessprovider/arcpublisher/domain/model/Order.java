// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Agrégat central du domaine arc-publisher.
 * Représente un order de provisioning planifié à exécuter à un instant T.
 * Aucune dépendance vers l'infrastructure ou les frameworks.
 */
public class Order {

    private final UUID id;
    private final OrderTargetType targetType;
    private final OrderAction action;
    private final String identityId;
    private final String identityName;
    private final String targetSystem;
    private final String entitlementId;
    private final String entitlementName;
    private final String scope;
    private final String accountType;
    private final String source;
    private final Map<String, String> attributes;
    private final OffsetDateTime executeAt;
    private final OffsetDateTime createdAt;
    private final String correlationId;
    private OrderStatus status;

    public Order(UUID id,
               OrderTargetType targetType,
               OrderAction action,
               String identityId,
               String identityName,
               String targetSystem,
               String entitlementId,
               String entitlementName,
               String scope,
               String accountType,
               String source,
               Map<String, String> attributes,
               OffsetDateTime executeAt,
               OffsetDateTime createdAt,
               String correlationId,
               OrderStatus status) {
        this.id = id;
        this.targetType = targetType;
        this.action = action;
        this.identityId = identityId;
        this.identityName = identityName;
        this.targetSystem = targetSystem;
        this.entitlementId = entitlementId;
        this.entitlementName = entitlementName;
        this.scope = scope;
        this.accountType = accountType;
        this.source = source;
        this.attributes = attributes;
        this.executeAt = executeAt;
        this.createdAt = createdAt;
        this.correlationId = correlationId;
        this.status = status;
    }

    /** Marque le order comme étant en cours d'exécution. */
    public void markExecuting() {
        this.status = OrderStatus.EXECUTING;
    }

    /** Marque le order comme terminé avec succès. */
    public void markDone() {
        this.status = OrderStatus.DONE;
    }

    /** Marque le order en échec. */
    public void markFailed() {
        this.status = OrderStatus.FAILED;
    }

    /** Annule le order si encore en attente. */
    public void cancel() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    /** Indique si le order est prêt à être exécuté maintenant. */
    public boolean isDue(OffsetDateTime now) {
        return this.status == OrderStatus.PENDING && !this.executeAt.isAfter(now);
    }

    public UUID getId() { return id; }
    public OrderTargetType getTargetType() { return targetType; }
    public OrderAction getAction() { return action; }
    public String getIdentityId() { return identityId; }
    public String getIdentityName() { return identityName; }
    public String getTargetSystem() { return targetSystem; }
    public String getEntitlementId() { return entitlementId; }
    public String getEntitlementName() { return entitlementName; }
    public String getScope() { return scope; }
    public String getAccountType() { return accountType; }
    public String getSource() { return source; }
    public Map<String, String> getAttributes() { return attributes; }
    public OffsetDateTime getExecuteAt() { return executeAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getCorrelationId() { return correlationId; }
    public OrderStatus getStatus() { return status; }
}
