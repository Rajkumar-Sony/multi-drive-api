package com.multidrive.api.service;

import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class DriveOperationLeaseRecoveryScheduler {

	private static final Logger LOGGER = LoggerFactory.getLogger(DriveOperationLeaseRecoveryScheduler.class);

	private final DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	public DriveOperationLeaseRecoveryScheduler(DriveOperationJobExecutionStore driveOperationJobExecutionStore) {

		this.driveOperationJobExecutionStore = driveOperationJobExecutionStore;
	}

	@Scheduled(fixedDelay = 30_000)
	public void recoverExpiredLeases() {

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		int recovered = driveOperationJobExecutionStore.recoverExpiredLeases(now, now.plusSeconds(5));

		if (recovered > 0) {

			LOGGER.warn("Recovered expired Drive operation worker leases. count={}", recovered);
		}
	}

}
