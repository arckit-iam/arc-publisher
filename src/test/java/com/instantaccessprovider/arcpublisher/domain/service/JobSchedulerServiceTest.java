package com.instantaccessprovider.arcpublisher.domain.service;

import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.domain.model.JobAction;
import com.instantaccessprovider.arcpublisher.domain.model.JobStatus;
import com.instantaccessprovider.arcpublisher.domain.model.JobTargetType;
import com.instantaccessprovider.arcpublisher.domain.port.JobRepository;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class JobSchedulerServiceTest {

    private JobRepository jobRepository;
    private ProvisioningEventPort provisioningEventPort;
    private JobSchedulerService service;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        provisioningEventPort = mock(ProvisioningEventPort.class);
        service = new JobSchedulerService(jobRepository, provisioningEventPort);
    }

    private Job createJob(JobStatus status, OffsetDateTime executeAt) {
        return new Job(
                UUID.randomUUID(),
                JobTargetType.IDENTITY,
                JobAction.CREATE,
                "S-1-5-21-123",
                "jdupont",
                "active-directory",
                null, null, null, null, null,
                executeAt,
                OffsetDateTime.now().minusMinutes(1),
                "corr-001",
                status
        );
    }

    @Test
    void processDueJobs_publishesAndMarksDone_whenJobIsDue() {
        Job job = createJob(JobStatus.PENDING, OffsetDateTime.now().minusSeconds(10));
        when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));

        service.processDueJobs();

        verify(provisioningEventPort).publish(job);
        assertThat(job.getStatus()).isEqualTo(JobStatus.DONE);
        verify(jobRepository, atLeastOnce()).updateStatus(eq(job.getId()), any());
    }

    @Test
    void processDueJobs_marksFailedOnPublishError() {
        Job job = createJob(JobStatus.PENDING, OffsetDateTime.now().minusSeconds(5));
        when(jobRepository.findDueJobs(any())).thenReturn(List.of(job));
        doThrow(new RuntimeException("Kafka down")).when(provisioningEventPort).publish(job);

        service.processDueJobs();

        assertThat(job.getStatus()).isEqualTo(JobStatus.FAILED);
    }

    @Test
    void processDueJobs_doesNothing_whenNoDueJobs() {
        when(jobRepository.findDueJobs(any())).thenReturn(List.of());

        service.processDueJobs();

        verifyNoInteractions(provisioningEventPort);
    }

    @Test
    void job_isDue_returnsFalse_whenExecuteAtIsInFuture() {
        Job job = createJob(JobStatus.PENDING, OffsetDateTime.now().plusHours(1));
        assertThat(job.isDue(OffsetDateTime.now())).isFalse();
    }

    @Test
    void job_cancel_throwsException_whenNotPending() {
        Job job = createJob(JobStatus.DONE, OffsetDateTime.now());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, job::cancel);
    }
}
