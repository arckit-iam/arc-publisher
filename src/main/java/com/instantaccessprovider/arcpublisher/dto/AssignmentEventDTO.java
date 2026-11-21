// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Event publié sur arc.provisioning.assignment.
 * Format ISO arc.sync.assignment + champs de provisioning (system, jobId, correlationId).
 */
public class AssignmentEventDTO {

    private UUID jobId;
    private String correlationId;
    private String system;

    // Champs ISO arc.sync.assignment
    private OffsetDateTime timestamp;
    private String eventType;
    private String source;
    private int sourceEventId;
    private Identity identity;
    private Entitlement entitlement;
    private Scope scope;
    private List<ChangedAttribute> changedAttributes;

    public static class Identity {
        private String name;
        private String displayName;
        private String externalId;
        private String type;
        private String system;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
    }

    public static class Entitlement {
        private String name;
        private String displayName;
        private String externalId;
        private String type;
        private String system;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }
    }

    public static class Scope {
        private List<Dimension> dimensions;
        public List<Dimension> getDimensions() { return dimensions; }
        public void setDimensions(List<Dimension> dimensions) { this.dimensions = dimensions; }

        public static class Dimension {
            private String type;
            private String value;
            private String label;
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public String getValue() { return value; }
            public void setValue(String value) { this.value = value; }
            public String getLabel() { return label; }
            public void setLabel(String label) { this.label = label; }
        }
    }

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
    public Identity getIdentity() { return identity; }
    public void setIdentity(Identity identity) { this.identity = identity; }
    public Entitlement getEntitlement() { return entitlement; }
    public void setEntitlement(Entitlement entitlement) { this.entitlement = entitlement; }
    public Scope getScope() { return scope; }
    public void setScope(Scope scope) { this.scope = scope; }
    public List<ChangedAttribute> getChangedAttributes() { return changedAttributes; }
    public void setChangedAttributes(List<ChangedAttribute> changedAttributes) { this.changedAttributes = changedAttributes; }
}
