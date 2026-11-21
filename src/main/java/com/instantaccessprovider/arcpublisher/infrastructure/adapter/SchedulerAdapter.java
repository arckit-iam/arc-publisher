// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package com.instantaccessprovider.arcpublisher.infrastructure.adapter;

import com.instantaccessprovider.arcpublisher.domain.service.JobSchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adapter de déclenchement du scheduler.
 * Combine :
 * - un polling toutes les secondes (@Scheduled) comme filet de sécurité
 * - un réveil immédiat via PostgreSQL NOTIFY/LISTEN (PgNotifyListener)
 */
@Component
public class SchedulerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SchedulerAdapter.class);

    private final JobSchedulerService jobSchedulerService;

    public SchedulerAdapter(JobSchedulerService jobSchedulerService) {
        this.jobSchedulerService = jobSchedulerService;
    }

    /**
     * Vérifie toutes les secondes si des jobs sont dus.
     * Filet de sécurité si le NOTIFY PostgreSQL est manqué.
     */
    @Scheduled(fixedDelay = 1000)
    public void tick() {
        try {
            jobSchedulerService.processDueJobs();
        } catch (Exception e) {
            log.error("Error processing due jobs", e);
        }
    }
}
