package com.instantaccessprovider.arcpublisher.dto;

import com.instantaccessprovider.arcpublisher.domain.model.JobAction;
import com.instantaccessprovider.arcpublisher.domain.model.JobStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event publié sur le topic Kafka arc.provisioning.
 * Ce DTO est le contrat d'interface avec les services consommateurs (AD, LDAP, apps cibles).
 */
public class ProvisioningEventDTO {

    private UUID jobId;
    private JobAction action;
    private String identityId;
    private String identityName;
    private String targetSystem;
    private String entitlementId;
    private String entitlementName;
    private String scope;
    private Map<String, String> attributes;
    private OffsetDateTime executeAt;
    private String correlationId;

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public JobAction getAction() { return action; }
    public void setAction(JobAction action) { this.action = action; }
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
