// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.port;

import com.instantaccessprovider.arcpublisher.domain.model.Order;

/**
 * Port de sortie — publication des events de provisioning sur Kafka.
 * Implémenté par KafkaProvisioningPublisher dans l'infrastructure.
 */
public interface ProvisioningEventPort {

    /**
     * Publie un event de provisioning correspondant au order fourni.
     *
     * @param order le order dont l'heure d'exécution est atteinte
     */
    void publish(Order order);
}
