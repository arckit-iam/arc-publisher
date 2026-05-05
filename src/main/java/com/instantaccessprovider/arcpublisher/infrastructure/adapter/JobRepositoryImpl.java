package com.instantaccessprovider.arcpublisher.infrastructure.adapter;

import com.instantaccessprovider.arcpublisher.domain.model.Job;
import com.instantaccessprovider.arcpublisher.domain.model.JobAction;
import com.instantaccessprovider.arcpublisher.domain.model.JobStatus;
import com.instantaccessprovider.arcpublisher.domain.port.JobRepository;
import com.instantaccessprovider.arcpublisher.infrastructure.persistence.JobEntity;
import com.instantaccessprovider.arcpublisher.infrastructure.persistence.SpringJobRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JobRepositoryImpl implements JobRepository {

    private final SpringJobRepository springJobRepository;

    public JobRepositoryImpl(SpringJobRepository springJobRepository) {
        this.springJobRepository = springJobRepository;
    }

    @Override
    public Job save(Job job) {
        JobEntity entity = toEntity(job);
        springJobRepository.save(entity);
        return job;
    }

    @Override
    public Optional<Job> findById(UUID id) {
        return springJobRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Job> findDueJobs(OffsetDateTime now) {
        return springJobRepository.findDueJobs(now)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void updateStatus(UUID id, JobStatus status) {
        springJobRepository.findById(id).ifPresent(entity -> {
            entity.setStatus(status);
            springJobRepository.save(entity);
        });
    }

    private JobEntity toEntity(Job job) {
        JobEntity e = new JobEntity();
        e.setId(job.getId());
        e.setAction(job.getAction().name());
        e.setIdentityId(job.getIdentityId());
        e.setIdentityName(job.getIdentityName());
        e.setTargetSystem(job.getTargetSystem());
        e.setEntitlementId(job.getEntitlementId());
        e.setEntitlementName(job.getEntitlementName());
        e.setScope(job.getScope());
        e.setAttributes(job.getAttributes());
        e.setExecuteAt(job.getExecuteAt());
        e.setCreatedAt(job.getCreatedAt());
        e.setCorrelationId(job.getCorrelationId());
        e.setStatus(job.getStatus());
        return e;
    }

    private Job toDomain(JobEntity e) {
        return new Job(
                e.getId(),
                JobAction.valueOf(e.getAction()),
                e.getIdentityId(),
                e.getIdentityName(),
                e.getTargetSystem(),
                e.getEntitlementId(),
                e.getEntitlementName(),
                e.getScope(),
                e.getAttributes(),
                e.getExecuteAt(),
                e.getCreatedAt(),
                e.getCorrelationId(),
                e.getStatus()
        );
    }
}
