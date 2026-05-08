package com.instantaccessprovider.arcpublisher.dto;

import com.instantaccessprovider.arcpublisher.domain.model.JobAction;
import com.instantaccessprovider.arcpublisher.domain.model.JobTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * DTO entrant pour la création d'un job de provisioning via REST.
 */
public class CreateJobRequest {

    @NotNull
    private JobAction action;

    @NotNull
    private JobTargetType targetType;

    @NotBlank
    private String identityId;

    @NotBlank
    private String identityName;

    @NotBlank
    private String targetSystem;

    private String entitlementId;
    private String entitlementName;
    private String scope;
    private Map<String, String> attributes;

    @NotNull
    private OffsetDateTime executeAt;

    private String correlationId;

    public JobAction getAction() { return action; }
    public void setAction(JobAction action) { this.action = action; }
    public JobTargetType getTargetType() { return targetType; }
    public void setTargetType(JobTargetType targetType) { this.targetType = targetType; }
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
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    public OffsetDateTime getExecuteAt() { return executeAt; }
    public void setExecuteAt(OffsetDateTime executeAt) { this.executeAt = executeAt; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
}
