// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.port;

import com.instantaccessprovider.arcpublisher.domain.model.Order;
import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de sortie — persistance des orders de provisioning.
 * Implémenté par l'adapter JPA dans l'infrastructure.
 */
public interface OrderRepository {

    /** Persiste un nouveau order. */
    Order save(Order order);

    /** Récupère un order par son identifiant. */
    Optional<Order> findById(UUID id);

    /**
     * Récupère tous les orders dans un statut donné dont l'heure d'exécution
     * est inférieure ou égale à {@code now}.
     */
    List<Order> findDueOrders(OffsetDateTime now);

    /** Récupère tous les orders. */
    List<Order> findAll();

    /** Met à jour le statut d'un order. */
    void updateStatus(UUID id, OrderStatus status);

    /**
     * Change le statut seulement si l'ordre est encore dans le statut attendu (opération atomique).
     * Garantit qu'une annulation et l'exécution ne se marchent pas dessus.
     *
     * @return {@code true} si la transition a eu lieu
     */
    boolean transition(UUID id, OrderStatus expected, OrderStatus next);
}
