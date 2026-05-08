package com.instantaccessprovider.arcpublisher.infrastructure.persistence;

import com.instantaccessprovider.arcpublisher.domain.model.JobStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entité JPA pour la table provisioning_jobs.
 * Séparée du modèle domaine Job — pas de dépendance croisée.
 */
@Entity
@Table(name = "provisioning_jobs")
public class JobEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String targetType;

    @Column(nullable = false)
    private String identityId;

    @Column(nullable = false)
    private String identityName;

    @Column(nullable = false)
    private String targetSystem;

    private String entitlementId;
    private String entitlementName;
    private String scope;

    @ElementCollection(fetch = jakarta.persistence.FetchType.EAGER)
    @CollectionTable(name = "provisioning_job_attributes",
                     joinColumns = @JoinColumn(name = "job_id"))
    @MapKeyColumn(name = "attr_key")
    @Column(name = "attr_value")
    private Map<String, String> attributes;

    @Column(nullable = false)
    private OffsetDateTime executeAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
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
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
}
