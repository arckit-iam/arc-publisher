// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.model;

/**
 * Cycle de vie d'un order de provisioning.
 */
public enum OrderStatus {
    PENDING,
    EXECUTING,
    DONE,
    FAILED,
    CANCELLED
}
