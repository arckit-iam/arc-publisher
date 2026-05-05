package com.instantaccessprovider.arcpublisher.domain.port;

import com.instantaccessprovider.arcpublisher.domain.model.Job;

/**
 * Port de sortie — publication des events de provisioning sur Kafka.
 * Implémenté par KafkaProvisioningPublisher dans l'infrastructure.
 */
public interface ProvisioningEventPort {

    /**
     * Publie un event de provisioning correspondant au job fourni.
     *
     * @param job le job dont l'heure d'exécution est atteinte
     */
    void publish(Job job);
}
