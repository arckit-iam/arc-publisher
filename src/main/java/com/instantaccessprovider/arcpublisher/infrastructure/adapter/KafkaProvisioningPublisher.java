package com.instantaccessprovider.arcpublisher.infrastructure.adapter;

import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;
import com.instantaccessprovider.arcpublisher.dto.ProvisioningEventDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
@Component
public class KafkaProvisioningPublisher implements ProvisioningEventPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaProvisioningPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public KafkaProvisioningPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                      @Value("${arc.topics.provisioning}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(Job job) {
        ProvisioningEventDTO event = toDTO(job);
        String key = job.getIdentityName() + "_" + job.getTargetSystem();
        try {
            kafkaTemplate.send(topic, key, event).get();
            log.info("Published provisioning event: jobId={} action={} target={}",
                    job.getId(), job.getAction(), job.getTargetSystem());
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish provisioning event for jobId=" + job.getId(), e);
        }
    }

    private ProvisioningEventDTO toDTO(Job job) {
        ProvisioningEventDTO dto = new ProvisioningEventDTO();
        dto.setJobId(job.getId());
        dto.setAction(job.getAction());
        dto.setIdentityId(job.getIdentityId());
        dto.setIdentityName(job.getIdentityName());
        dto.setTargetSystem(job.getTargetSystem());
        dto.setEntitlementId(job.getEntitlementId());
        dto.setEntitlementName(job.getEntitlementName());
        dto.setScope(job.getScope());
        dto.setAttributes(job.getAttributes());
        dto.setExecuteAt(job.getExecuteAt());
        dto.setCorrelationId(job.getCorrelationId());
        return dto;
    }
}
