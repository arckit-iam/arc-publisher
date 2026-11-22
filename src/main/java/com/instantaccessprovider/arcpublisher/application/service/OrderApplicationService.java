// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.application.service;

import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotCancellableException;
import com.instantaccessprovider.arcpublisher.domain.exception.OrderNotFoundException;
import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;
import com.instantaccessprovider.arcpublisher.domain.port.OrderRepository;
import com.instantaccessprovider.arcpublisher.dto.CreateOrderRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service applicatif : orchestration des cas d'usage entrants.
 * Reçoit les requêtes REST et délègue au domaine.
 */
@Service
public class OrderApplicationService {

    private final OrderRepository orderRepository;

    public OrderApplicationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Enregistre un ordre de provisioning. Si l'appelant fournit un identifiant déjà connu,
     * l'ordre existant est retourné (rejeu idempotent, par exemple après une relivraison Kafka).
     * PostgreSQL émettra automatiquement un NOTIFY via le trigger DB.
     */
    public Order createOrder(CreateOrderRequest request) {
        if (request.getId() != null) {
            var existing = orderRepository.findById(request.getId());
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        Order order = new Order(
                request.getId() != null ? request.getId() : UUID.randomUUID(),
                request.getTargetType(),
                request.getAction(),
                request.getIdentityId(),
                request.getIdentityName(),
                request.getTargetSystem(),
                request.getEntitlementId(),
                request.getEntitlementName(),
                request.getScope(),
                request.getAccountType(),
                request.getSource(),
                request.getAttributes(),
                request.getExecuteAt(),
                OffsetDateTime.now(),
                request.getCorrelationId(),
                OrderStatus.PENDING
        );
        return orderRepository.save(order);
    }

    public Order getById(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    /**
     * Annule un ordre encore en attente. Idempotent : annuler un ordre déjà annulé est sans effet.
     *
     * @throws OrderNotFoundException       si l'ordre n'existe pas
     * @throws OrderNotCancellableException si l'ordre est déjà en cours d'exécution ou terminé
     */
    public void cancelOrder(UUID id) {
        Order order = getById(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        if (!orderRepository.transition(id, OrderStatus.PENDING, OrderStatus.CANCELLED)) {
            OrderStatus current = orderRepository.findById(id).map(Order::getStatus).orElse(order.getStatus());
            if (current != OrderStatus.CANCELLED) {
                throw new OrderNotCancellableException(id, current);
            }
        }
    }
}
