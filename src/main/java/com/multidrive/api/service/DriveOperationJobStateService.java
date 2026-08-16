package com.multidrive.api.service;

import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.exception.DriveOperationLeaseLostException;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class DriveOperationJobStateService {

	public static final Duration LEASE_DURATION = Duration.ofSeconds(90);

	private final DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	private final DriveOperationJobProgressPublisher driveOperationJobProgressPublisher;

	public DriveOperationJobStateService(DriveOperationJobExecutionStore driveOperationJobExecutionStore,
			DriveOperationJobProgressPublisher driveOperationJobProgressPublisher) {

		this.driveOperationJobExecutionStore = driveOperationJobExecutionStore;

		this.driveOperationJobProgressPublisher = driveOperationJobProgressPublisher;
	}

	public void publishClaimed(Long jobId) {

		driveOperationJobProgressPublisher.publish(jobId, "Job claimed by background worker");
	}

	public void transition(Long jobId, String workerId, DriveOperationJobStatus status, String message) {

		LocalDateTime now = now();

		boolean updated = driveOperationJobExecutionStore.transition(jobId, workerId, status, now,
				now.plus(LEASE_DURATION));

		if (!updated) {

			throw new DriveOperationLeaseLostException(jobId);
		}

		driveOperationJobProgressPublisher.publish(jobId, message);
	}

	public boolean cancelIfRequested(Long jobId, String workerId) {

		if (!driveOperationJobExecutionStore.isCancelRequested(jobId)) {

			return false;
		}

		boolean cancelled = driveOperationJobExecutionStore.cancelOwnedJob(jobId, workerId, now());

		if (!cancelled) {

			throw new DriveOperationLeaseLostException(jobId);
		}

		driveOperationJobProgressPublisher.publish(jobId, "Operation cancelled before Google mutation");

		return true;
	}

	public void markRootItemRunning(Long jobId) {

		driveOperationJobExecutionStore.markRootItemRunning(jobId);
	}

	public void markRootItemVerifying(Long jobId) {

		driveOperationJobExecutionStore.markRootItemVerifying(jobId);
	}

	public void completeNativeMove(Long jobId, String workerId, Long resultItemId, String resultGoogleFileId) {

		driveOperationJobExecutionStore.completeNativeMove(jobId, workerId, resultItemId, resultGoogleFileId, now());

		driveOperationJobProgressPublisher.publish(jobId, "Native Drive move completed");
	}

	public void scheduleRetry(Long jobId, String workerId, LocalDateTime nextAttemptAt, String errorCode,
			String errorMessage) {

		boolean updated = driveOperationJobExecutionStore.scheduleRetry(jobId, workerId, nextAttemptAt, errorCode,
				errorMessage, now());

		if (!updated) {

			throw new DriveOperationLeaseLostException(jobId);
		}

		driveOperationJobProgressPublisher.publish(jobId, "Operation scheduled for retry");
	}

	public void fail(Long jobId, String workerId, DriveOperationJobStatus terminalStatus, String errorCode,
			String errorMessage) {

		boolean updated = driveOperationJobExecutionStore.markFailure(jobId, workerId, terminalStatus, errorCode,
				errorMessage, now());

		if (!updated) {

			throw new DriveOperationLeaseLostException(jobId);
		}

		driveOperationJobProgressPublisher.publish(jobId, terminalStatus == DriveOperationJobStatus.CLEANUP_REQUIRED
				? "Operation requires reconciliation or cleanup" : "Operation failed");
	}

	private LocalDateTime now() {

		return LocalDateTime.now(ZoneOffset.UTC);
	}

}
