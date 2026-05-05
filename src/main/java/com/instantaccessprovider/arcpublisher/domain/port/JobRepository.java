package com.instantaccessprovider.arcpublisher.domain.port;

import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.domain.model.JobStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port de sortie — persistance des jobs de provisioning.
 * Implémenté par l'adapter JPA dans l'infrastructure.
 */
public interface JobRepository {

    /** Persiste un nouveau job. */
    Job save(Job job);

    /** Récupère un job par son identifiant. */
    Optional<Job> findById(UUID id);

    /**
     * Récupère tous les jobs dans un statut donné dont l'heure d'exécution
     * est inférieure ou égale à {@code now}.
     */
    List<Job> findDueJobs(OffsetDateTime now);

    /** Met à jour le statut d'un job. */
    void updateStatus(UUID id, JobStatus status);
}
