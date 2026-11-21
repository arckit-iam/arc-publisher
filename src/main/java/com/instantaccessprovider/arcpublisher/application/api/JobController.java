// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.application.api;

import com.instantaccessprovider.arcpublisher.application.service.JobApplicationService;
import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.dto.CreateJobRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobApplicationService jobApplicationService;

    public JobController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    /**
     * Crée un nouveau job de provisioning planifié.
     * POST /jobs
     */
    @PostMapping
    public ResponseEntity<UUID> createJob(@Valid @RequestBody CreateJobRequest request) {
        Job job = jobApplicationService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(job.getId());
    }

    @GetMapping
    public ResponseEntity<List<Job>> getAllJobs() {
        return ResponseEntity.ok(jobApplicationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable UUID id) {
        return ResponseEntity.ok(jobApplicationService.getById(id));
    }

    /**
     * Annule un job encore en attente.
     * DELETE /jobs/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelJob(@PathVariable UUID id) {
        jobApplicationService.cancelJob(id);
        return ResponseEntity.noContent().build();
    }
}
