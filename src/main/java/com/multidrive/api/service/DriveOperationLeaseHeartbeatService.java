package com.multidrive.api.service;

import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DriveOperationLeaseHeartbeatService {

    private final ConcurrentHashMap<
            Long,
            String
            > activeJobs =
            new ConcurrentHashMap<>();

    private final DriveOperationJobExecutionStore
            driveOperationJobExecutionStore;

    public DriveOperationLeaseHeartbeatService(
            DriveOperationJobExecutionStore
                    driveOperationJobExecutionStore
    ) {

        this.driveOperationJobExecutionStore =
                driveOperationJobExecutionStore;
    }

    public void register(
            Long jobId,
            String workerId
    ) {

        activeJobs.put(
                jobId,
                workerId
        );
    }

    public void unregister(
            Long jobId,
            String workerId
    ) {

        activeJobs.remove(
                jobId,
                workerId
        );
    }

    @Scheduled(
            fixedDelay = 20_000
    )
    public void heartbeat() {

        if (activeJobs.isEmpty()) {
            return;
        }

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );

        LocalDateTime leaseExpiresAt =
                now.plus(
                        DriveOperationJobStateService
                                .LEASE_DURATION
                );

        activeJobs.forEach(
                (
                        jobId,
                        workerId
                ) -> {

                    boolean updated =
                            driveOperationJobExecutionStore
                                    .extendLease(
                                            jobId,
                                            workerId,
                                            now,
                                            leaseExpiresAt
                                    );

                    if (!updated) {

                        activeJobs.remove(
                                jobId,
                                workerId
                        );
                    }
                }
        );
    }
}
