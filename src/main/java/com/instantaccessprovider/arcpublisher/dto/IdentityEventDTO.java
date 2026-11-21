// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event publié sur arc.provisioning.identity.
 * Format ISO arc.sync.identity + champs de provisioning (system, jobId, correlationId).
 */
public class IdentityEventDTO {

    private UUID jobId;
    private String correlationId;
    private String system;

    // Champs ISO arc.sync.identity
    private OffsetDateTime timestamp;
    private String eventType;
    private String source;
    private int sourceEventId;
    private String name;
    private String displayName;
    private String externalId;
    private String type;
    private List<ChangedAttribute> changedAttributes;

    public static class ChangedAttribute {
        private String name;
        private String oldValue;
        private String newValue;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }
        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }
    }

    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getSystem() { return system; }
    public void setSystem(String system) { this.system = system; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public int getSourceEventId() { return sourceEventId; }
    public void setSourceEventId(int sourceEventId) { this.sourceEventId = sourceEventId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public List<ChangedAttribute> getChangedAttributes() { return changedAttributes; }
    public void setChangedAttributes(List<ChangedAttribute> changedAttributes) { this.changedAttributes = changedAttributes; }
}
