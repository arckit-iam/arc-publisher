// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.consume;

/** Message de la file d'ordres illisible ou invalide : inutile de le rejouer, il part en DLT. */
public class InvalidOrderMessageException extends RuntimeException {

    public InvalidOrderMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidOrderMessageException(String message) {
        super(message);
    }
}
