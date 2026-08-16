package com.multidrive.api.service;

import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class DriveOperationJobDispatcher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    DriveOperationJobDispatcher.class
            );

    private static final int MAX_CLAIMS_PER_TICK =
            4;

    private final DriveOperationJobExecutionStore
            driveOperationJobExecutionStore;

    private final DriveOperationJobRunner
            driveOperationJobRunner;

    private final DriveOperationWorkerIdentity
            driveOperationWorkerIdentity;

    private final ThreadPoolTaskExecutor
            driveOperationTaskExecutor;

    public DriveOperationJobDispatcher(
            DriveOperationJobExecutionStore
                    driveOperationJobExecutionStore,
            DriveOperationJobRunner
                    driveOperationJobRunner,
            DriveOperationWorkerIdentity
                    driveOperationWorkerIdentity,
            @Qualifier(
                    "driveOperationTaskExecutor"
            )
            ThreadPoolTaskExecutor
                    driveOperationTaskExecutor
    ) {

        this.driveOperationJobExecutionStore =
                driveOperationJobExecutionStore;

        this.driveOperationJobRunner =
                driveOperationJobRunner;

        this.driveOperationWorkerIdentity =
                driveOperationWorkerIdentity;

        this.driveOperationTaskExecutor =
                driveOperationTaskExecutor;
    }

    @Scheduled(
            fixedDelay = 1000
    )
    public void dispatch() {

        String workerId =
                driveOperationWorkerIdentity
                        .getWorkerId();

        for (int index = 0;
             index < MAX_CLAIMS_PER_TICK;
             index++) {

            LocalDateTime now =
                    LocalDateTime.now(
                            ZoneOffset.UTC
                    );

            Optional<Long> claimedJobId =
                    driveOperationJobExecutionStore
                            .claimNextNativeMove(
                                    workerId,
                                    now,
                                    now.plus(
                                            DriveOperationJobStateService
                                                    .LEASE_DURATION
                                    )
                            );

            if (claimedJobId.isEmpty()) {
                return;
            }

            Long jobId =
                    claimedJobId.get();

            try {

                driveOperationTaskExecutor
                        .execute(
                                () ->
                                        driveOperationJobRunner
                                                .run(
                                                        jobId,
                                                        workerId
                                                )
                        );

            } catch (TaskRejectedException exception) {

                LOGGER.warn(
                        "Drive operation executor queue is full. Releasing claimed job. jobId={}",
                        jobId
                );

                LocalDateTime releaseTime =
                        LocalDateTime.now(
                                ZoneOffset.UTC
                        );

                driveOperationJobExecutionStore
                        .releaseRejectedClaim(
                                jobId,
                                workerId,
                                releaseTime.plusSeconds(
                                        2
                                ),
                                releaseTime
                        );

                return;
            }
        }
    }
}
