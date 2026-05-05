package com.instantaccessprovider.arcpublisher.infrastructure.adapter;

import com.instantaccessprovider.arcpublisher.domain.service.JobSchedulerService;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Écoute les notifications PostgreSQL NOTIFY sur le canal "job_created".
 * Réveille le scheduler immédiatement dès qu'un job est créé (latence ~0ms).
 */
@Component
public class PgNotifyListener {

    private static final Logger log = LoggerFactory.getLogger(PgNotifyListener.class);
    private static final String CHANNEL = "job_created";

    private final DataSource dataSource;
    private final JobSchedulerService jobSchedulerService;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running = true;

    public PgNotifyListener(DataSource dataSource, JobSchedulerService jobSchedulerService) {
        this.dataSource = dataSource;
        this.jobSchedulerService = jobSchedulerService;
    }

    @PostConstruct
    public void start() {
        executor.execute(this::listenLoop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    private void listenLoop() {
        while (running) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                stmt.execute("LISTEN " + CHANNEL);
                PGConnection pgConn = conn.unwrap(PGConnection.class);
                log.info("PgNotifyListener: LISTEN {} started", CHANNEL);

                while (running) {
                    PGNotification[] notifications = pgConn.getNotifications(5000);
                    if (notifications != null && notifications.length > 0) {
                        log.debug("Received {} notification(s) on channel {}", notifications.length, CHANNEL);
                        jobSchedulerService.processDueJobs();
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log.warn("PgNotifyListener connection lost, retrying in 5s: {}", e.getMessage());
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                }
            }
        }
    }
}
