// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.domain.model;

/**
 * Type d'objet ciblé par le job — détermine le topic Kafka de publication.
 */
public enum JobTargetType {
    IDENTITY,
    ASSIGNMENT,
    ENTITLEMENT
}
