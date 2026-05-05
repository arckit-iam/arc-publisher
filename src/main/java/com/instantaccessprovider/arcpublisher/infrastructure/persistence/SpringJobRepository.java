package com.instantaccessprovider.arcpublisher.infrastructure.persistence;

import com.instantaccessprovider.arcpublisher.domain.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SpringJobRepository extends JpaRepository<JobEntity, UUID> {

    @Query("SELECT j FROM JobEntity j WHERE j.status = 'PENDING' AND j.executeAt <= :now")
    List<JobEntity> findDueJobs(@Param("now") OffsetDateTime now);
}
