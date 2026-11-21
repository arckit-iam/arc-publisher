// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.model;

/**
 * Cycle de vie d'un job de provisioning.
 */
public enum JobStatus {
    PENDING,
    EXECUTING,
    DONE,
    FAILED,
    CANCELLED
}
