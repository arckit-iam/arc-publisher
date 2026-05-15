package com.instantaccessprovider.arcpublisher.application.service;

import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.domain.model.JobStatus;
import com.instantaccessprovider.arcpublisher.domain.port.JobRepository;
import com.instantaccessprovider.arcpublisher.dto.CreateJobRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service applicatif : orchestration des cas d'usage entrants.
 * Reçoit les requêtes REST et délègue au domaine.
 */
@Service
public class JobApplicationService {

    private final JobRepository jobRepository;

    public JobApplicationService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Crée et persiste un nouveau job de provisioning.
     * PostgreSQL émettra automatiquement un NOTIFY via le trigger DB.
     */
    public Job createJob(CreateJobRequest request) {
        Job job = new Job(
                UUID.randomUUID(),
                request.getTargetType(),
                request.getAction(),
                request.getIdentityId(),
                request.getIdentityName(),
                request.getTargetSystem(),
                request.getEntitlementId(),
                request.getEntitlementName(),
                request.getScope(),
                request.getSource(),
                request.getAttributes(),
                request.getExecuteAt(),
                OffsetDateTime.now(),
                request.getCorrelationId(),
                JobStatus.PENDING
        );
        return jobRepository.save(job);
    }

    public Job getById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
    }

    public List<Job> getAll() {
        return jobRepository.findAll();
    }

    /**
     * Annule un job encore en attente.
     */
    public void cancelJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        job.cancel();
        jobRepository.updateStatus(id, job.getStatus());
    }
}
