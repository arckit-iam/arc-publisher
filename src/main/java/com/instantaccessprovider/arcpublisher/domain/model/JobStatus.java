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
