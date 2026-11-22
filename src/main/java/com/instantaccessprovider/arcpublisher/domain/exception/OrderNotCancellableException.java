// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.exception;

import com.instantaccessprovider.arcpublisher.domain.model.OrderStatus;

import java.util.UUID;

/** L'ordre ne peut plus être annulé : il est déjà en cours d'exécution ou terminé. */
public class OrderNotCancellableException extends RuntimeException {

    public OrderNotCancellableException(UUID id, OrderStatus status) {
        super("Order " + id + " cannot be cancelled, status is " + status);
    }
}
