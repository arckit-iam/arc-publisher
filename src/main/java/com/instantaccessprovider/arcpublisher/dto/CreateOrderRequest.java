// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.dto;

import com.instantaccessprovider.arcpublisher.domain.model.OrderAction;
import com.instantaccessprovider.arcpublisher.domain.model.OrderTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO entrant pour la création d'un order de provisioning via REST.
 */
public class CreateOrderRequest {

    /**
     * Identifiant optionnel fourni par l'émetteur (UUID). Permet d'annuler l'ordre plus tard
     * sans aller-retour et rend la soumission idempotente. Généré par le publisher si absent.
     */
    private UUID id;

    @NotNull
    private OrderAction action;

    @NotNull
    private OrderTargetType targetType;

    @NotBlank
    private String identityId;

    @NotBlank
    private String identityName;

    @NotBlank
    private String targetSystem;

    private String entitlementId;
    private String entitlementName;
    private String scope;
    /** Type métier du compte (ex. standard, admin) ; l'agent détermine l'emplacement cible. */
    private String accountType;
    private String source;
    private Map<String, String> attributes;

    @NotNull
    private OffsetDateTime executeAt;

    private String correlationId;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public OrderAction getAction() { return action; }
    public void setAction(OrderAction action) { this.action = action; }
    public OrderTargetType getTargetType() { return targetType; }
    public void setTargetType(OrderTargetType targetType) { this.targetType = targetType; }
    public String getIdentityId() { return identityId; }
    public void setIdentityId(String identityId) { this.identityId = identityId; }
    public String getIdentityName() { return identityName; }
    public void setIdentityName(String identityName) { this.identityName = identityName; }
    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }
    public String getEntitlementId() { return entitlementId; }
    public void setEntitlementId(String entitlementId) { this.entitlementId = entitlementId; }
    public String getEntitlementName() { return entitlementName; }
    public void setEntitlementName(String entitlementName) { this.entitlementName = entitlementName; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    public OffsetDateTime getExecuteAt() { return executeAt; }
    public void setExecuteAt(OffsetDateTime executeAt) { this.executeAt = executeAt; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
