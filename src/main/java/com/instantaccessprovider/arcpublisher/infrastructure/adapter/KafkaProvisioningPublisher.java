package com.instantaccessprovider.arcpublisher.infrastructure.adapter;

import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.domain.model.JobAction;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;
import com.instantaccessprovider.arcpublisher.dto.AssignmentEventDTO;
import com.instantaccessprovider.arcpublisher.dto.EntitlementEventDTO;
import com.instantaccessprovider.arcpublisher.dto.IdentityEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class KafkaProvisioningPublisher implements ProvisioningEventPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaProvisioningPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String identityTopic;
    private final String assignmentTopic;
    private final String entitlementTopic;

    public KafkaProvisioningPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                      @Value("${arc.topics.provisioning.identity}") String identityTopic,
                                      @Value("${arc.topics.provisioning.assignment}") String assignmentTopic,
                                      @Value("${arc.topics.provisioning.entitlement}") String entitlementTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.identityTopic = identityTopic;
        this.assignmentTopic = assignmentTopic;
        this.entitlementTopic = entitlementTopic;
    }

    @Override
    public void publish(Job job) {
        Object event;
        String topic;
        String key = job.getIdentityName() + "_" + job.getTargetSystem();

        switch (job.getAction()) {
            case ASSIGN, UNASSIGN -> {
                event = toAssignmentDTO(job);
                topic = assignmentTopic;
            }
            case CREATE_ENTITLEMENT, DELETE_ENTITLEMENT -> {
                event = toEntitlementDTO(job);
                topic = entitlementTopic;
            }
            default -> {
                event = toIdentityDTO(job);
                topic = identityTopic;
            }
        }

        try {
            kafkaTemplate.send(topic, key, event).get();
            log.info("Published provisioning event: jobId={} action={} topic={} target={}",
                    job.getId(), job.getAction(), topic, job.getTargetSystem());
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish provisioning event for jobId=" + job.getId(), e);
        }
    }

    private IdentityEventDTO toIdentityDTO(Job job) {
        IdentityEventDTO dto = new IdentityEventDTO();
        dto.setJobId(job.getId());
        dto.setCorrelationId(job.getCorrelationId());
        dto.setSystem(job.getTargetSystem());
        dto.setId(job.getId().toString());
        dto.setTimestamp(job.getExecuteAt());
        dto.setEventType(job.getAction().name());
        dto.setSource(job.getTargetSystem());
        dto.setName(job.getIdentityId());
        dto.setDisplayName(job.getIdentityName());
        dto.setExternalId(job.getIdentityId());
        dto.setType("user");
        dto.setChangedAttributes(toChangedAttributes(job.getAttributes()));
        return dto;
    }

    private AssignmentEventDTO toAssignmentDTO(Job job) {
        AssignmentEventDTO dto = new AssignmentEventDTO();
        dto.setJobId(job.getId());
        dto.setCorrelationId(job.getCorrelationId());
        dto.setSystem(job.getTargetSystem());
        dto.setId(job.getId().toString());
        dto.setTimestamp(job.getExecuteAt());
        dto.setEventType(job.getAction().name());
        dto.setSource(job.getTargetSystem());

        AssignmentEventDTO.Identity identity = new AssignmentEventDTO.Identity();
        identity.setName(job.getIdentityId());
        identity.setDisplayName(job.getIdentityName());
        identity.setExternalId(job.getIdentityId());
        identity.setType("user");
        identity.setSystem(job.getTargetSystem());
        dto.setIdentity(identity);

        AssignmentEventDTO.Entitlement entitlement = new AssignmentEventDTO.Entitlement();
        entitlement.setName(job.getEntitlementId());
        entitlement.setDisplayName(job.getEntitlementName());
        entitlement.setExternalId(job.getEntitlementId());
        entitlement.setType("security-group");
        entitlement.setSystem(job.getTargetSystem());
        dto.setEntitlement(entitlement);

        if (job.getScope() != null) {
            AssignmentEventDTO.Scope scope = new AssignmentEventDTO.Scope();
            AssignmentEventDTO.Scope.Dimension dim = new AssignmentEventDTO.Scope.Dimension();
            dim.setType("ou");
            dim.setValue(job.getScope());
            scope.setDimensions(List.of(dim));
            dto.setScope(scope);
        }

        dto.setChangedAttributes(toChangedAttributes(job.getAttributes()));
        return dto;
    }

    private EntitlementEventDTO toEntitlementDTO(Job job) {
        EntitlementEventDTO dto = new EntitlementEventDTO();
        dto.setJobId(job.getId());
        dto.setCorrelationId(job.getCorrelationId());
        dto.setSystem(job.getTargetSystem());
        dto.setId(job.getId().toString());
        dto.setTimestamp(job.getExecuteAt());
        dto.setEventType(job.getAction().name());
        dto.setSource(job.getTargetSystem());
        dto.setName(job.getEntitlementId());
        dto.setDisplayName(job.getEntitlementName());
        dto.setExternalId(job.getEntitlementId());
        dto.setType("security-group");
        dto.setChangedAttributes(toChangedAttributes(job.getAttributes()));
        return dto;
    }

    private List<IdentityEventDTO.ChangedAttribute> toChangedAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) return List.of();
        return attributes.entrySet().stream().map(e -> {
            IdentityEventDTO.ChangedAttribute attr = new IdentityEventDTO.ChangedAttribute();
            attr.setName(e.getKey());
            attr.setNewValue(e.getValue());
            return attr;
        }).collect(Collectors.toList());
    }
}

