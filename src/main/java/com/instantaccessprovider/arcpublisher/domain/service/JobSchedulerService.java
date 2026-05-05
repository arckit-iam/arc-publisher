package com.instantaccessprovider.arcpublisher.domain.service;

import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.domain.port.JobRepository;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service domaine : traite les jobs arrivés à échéance.
 * Aucune dépendance vers l'infrastructure ou les frameworks.
 */
public class JobSchedulerService {

    private final JobRepository jobRepository;
    private final ProvisioningEventPort provisioningEventPort;

    public JobSchedulerService(JobRepository jobRepository,
                               ProvisioningEventPort provisioningEventPort) {
        this.jobRepository = jobRepository;
        this.provisioningEventPort = provisioningEventPort;
    }

    /**
     * Récupère tous les jobs dus, les publie sur Kafka et met à jour leur statut.
     * Appelé par le scheduler (toutes les secondes ou sur NOTIFY PostgreSQL).
     */
    public void processDueJobs() {
        List<Job> dueJobs = jobRepository.findDueJobs(OffsetDateTime.now());

        for (Job job : dueJobs) {
            try {
                job.markExecuting();
                jobRepository.updateStatus(job.getId(), job.getStatus());

                provisioningEventPort.publish(job);

                job.markDone();
                jobRepository.updateStatus(job.getId(), job.getStatus());
            } catch (Exception e) {
                job.markFailed();
                jobRepository.updateStatus(job.getId(), job.getStatus());
            }
        }
    }
}
