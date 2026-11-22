// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.service;

import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;
import com.instantaccessprovider.arcpublisher.domain.port.OrderRepository;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service domaine : traite les orders arrivés à échéance.
 * Aucune dépendance vers l'infrastructure ou les frameworks.
 */
public class OrderSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(OrderSchedulerService.class);

    private final OrderRepository orderRepository;
    private final ProvisioningEventPort provisioningEventPort;

    public OrderSchedulerService(OrderRepository orderRepository,
                               ProvisioningEventPort provisioningEventPort) {
        this.orderRepository = orderRepository;
        this.provisioningEventPort = provisioningEventPort;
    }

    /**
     * Récupère tous les orders dus, les publie sur Kafka et met à jour leur statut.
     * Appelé par le scheduler (toutes les secondes ou sur NOTIFY PostgreSQL).
     */
    public void processDueOrders() {
        List<Order> dueOrders = orderRepository.findDueOrders(OffsetDateTime.now());

        for (Order order : dueOrders) {
            try {
                // Transition atomique : si le manager a annulé l'ordre entre-temps, on n'exécute rien.
                if (!orderRepository.transition(order.getId(), OrderStatus.PENDING, OrderStatus.EXECUTING)) {
                    log.info("Order skipped (no longer pending): orderId={}", order.getId());
                    continue;
                }
                order.markExecuting();

                provisioningEventPort.publish(order);

                order.markDone();
                orderRepository.updateStatus(order.getId(), order.getStatus());
            } catch (Exception e) {
                log.error("Order processing failed: orderId={} action={} error={}",
                        order.getId(), order.getAction(), e.getMessage(), e);
                order.markFailed();
                orderRepository.updateStatus(order.getId(), order.getStatus());
            }
        }
    }
}
