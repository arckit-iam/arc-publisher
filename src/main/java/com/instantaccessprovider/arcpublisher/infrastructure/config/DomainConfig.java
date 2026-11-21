// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.config;

import com.instantaccessprovider.arcpublisher.domain.port.JobRepository;
import com.instantaccessprovider.arcpublisher.domain.port.ProvisioningEventPort;
import com.instantaccessprovider.arcpublisher.domain.service.JobSchedulerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class DomainConfig {

    @Bean
    public JobSchedulerService jobSchedulerService(JobRepository jobRepository,
                                                    ProvisioningEventPort provisioningEventPort) {
        return new JobSchedulerService(jobRepository, provisioningEventPort);
    }
}
