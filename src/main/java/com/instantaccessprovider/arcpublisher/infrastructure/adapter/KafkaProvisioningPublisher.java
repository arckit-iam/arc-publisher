// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.adapter;

import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.domain.model.OrderTargetType;
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
    public void publish(Order order) {
        Object event;
        String topic;
        String key = order.getIdentityName() + "_" + order.getTargetSystem();

        switch (order.getTargetType()) {
            case ASSIGNMENT -> {
                event = toAssignmentDTO(order);
                topic = assignmentTopic;
            }
            case ENTITLEMENT -> {
                event = toEntitlementDTO(order);
                topic = entitlementTopic;
            }
            default -> {
                event = toIdentityDTO(order);
                topic = identityTopic;
            }
        }

        try {
            kafkaTemplate.send(topic, key, event).get();
            log.info("Published provisioning event: orderId={} action={} topic={} target={}",
                    order.getId(), order.getAction(), topic, order.getTargetSystem());
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish provisioning event for orderId=" + order.getId(), e);
        }
    }

    private IdentityEventDTO toIdentityDTO(Order order) {
        IdentityEventDTO dto = new IdentityEventDTO();
        dto.setOrderId(order.getId());
        dto.setCorrelationId(order.getCorrelationId());
        dto.setSystem(order.getTargetSystem());
        dto.setTimestamp(order.getExecuteAt());
        dto.setEventType(order.getAction().name());
        dto.setSource(order.getTargetSystem());
        dto.setName(order.getIdentityId());
        dto.setDisplayName(order.getIdentityName());
        dto.setExternalId(order.getIdentityId());
        dto.setType("user");
        dto.setAccountType(order.getAccountType());
        dto.setChangedAttributes(toChangedAttributes(order.getAttributes()));
        return dto;
    }

    private AssignmentEventDTO toAssignmentDTO(Order order) {
        AssignmentEventDTO dto = new AssignmentEventDTO();
        dto.setOrderId(order.getId());
        dto.setCorrelationId(order.getCorrelationId());
        dto.setSystem(order.getTargetSystem());
        dto.setTimestamp(order.getExecuteAt());
        dto.setEventType(order.getAction().name());
        dto.setSource(order.getTargetSystem());

        AssignmentEventDTO.Identity identity = new AssignmentEventDTO.Identity();
        identity.setName(order.getIdentityId());
        identity.setDisplayName(order.getIdentityName());
        identity.setExternalId(order.getIdentityId());
        identity.setType("user");
        identity.setAccountType(order.getAccountType());
        identity.setSystem(order.getTargetSystem());
        dto.setIdentity(identity);

        AssignmentEventDTO.Entitlement entitlement = new AssignmentEventDTO.Entitlement();
        entitlement.setName(order.getEntitlementId());
        entitlement.setDisplayName(order.getEntitlementName());
        entitlement.setExternalId(order.getEntitlementId());
        entitlement.setType("security-group");
        entitlement.setSystem(order.getTargetSystem());
        dto.setEntitlement(entitlement);

        if (order.getScope() != null) {
            AssignmentEventDTO.Scope scope = new AssignmentEventDTO.Scope();
            AssignmentEventDTO.Scope.Dimension dim = new AssignmentEventDTO.Scope.Dimension();
            dim.setType("ou");
            dim.setValue(order.getScope());
            scope.setDimensions(List.of(dim));
            dto.setScope(scope);
        }

        dto.setChangedAttributes(toAssignmentChangedAttributes(order.getAttributes()));
        return dto;
    }

    private EntitlementEventDTO toEntitlementDTO(Order order) {
        EntitlementEventDTO dto = new EntitlementEventDTO();
        dto.setOrderId(order.getId());
        dto.setCorrelationId(order.getCorrelationId());
        dto.setSystem(order.getTargetSystem());
        dto.setTimestamp(order.getExecuteAt());
        dto.setEventType(order.getAction().name());
        dto.setSource(order.getSource() != null ? order.getSource() : order.getTargetSystem());
        dto.setName(order.getEntitlementId());
        dto.setDisplayName(order.getEntitlementName());
        dto.setExternalId(order.getEntitlementId());
        dto.setType("security-group");
        dto.setScope(order.getScope());
        dto.setChangedAttributes(toEntitlementChangedAttributes(order.getAttributes()));
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

    private List<AssignmentEventDTO.ChangedAttribute> toAssignmentChangedAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) return List.of();
        return attributes.entrySet().stream().map(e -> {
            AssignmentEventDTO.ChangedAttribute attr = new AssignmentEventDTO.ChangedAttribute();
            attr.setName(e.getKey());
            attr.setNewValue(e.getValue());
            return attr;
        }).collect(Collectors.toList());
    }

    private List<EntitlementEventDTO.ChangedAttribute> toEntitlementChangedAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) return List.of();
        return attributes.entrySet().stream().map(e -> {
            EntitlementEventDTO.ChangedAttribute attr = new EntitlementEventDTO.ChangedAttribute();
            attr.setName(e.getKey());
            attr.setNewValue(e.getValue());
            return attr;
        }).collect(Collectors.toList());
    }
}

