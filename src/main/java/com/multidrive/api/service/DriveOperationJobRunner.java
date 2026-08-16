package com.multidrive.api.service;

import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.exception.DriveOperationLeaseLostException;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DriveOperationJobRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(DriveOperationJobRunner.class);

	private final DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	private final DriveOperationJobStateService driveOperationJobStateService;

	private final DriveOperationRetryPolicy driveOperationRetryPolicy;

	private final DriveOperationLeaseHeartbeatService driveOperationLeaseHeartbeatService;

	private final Map<DriveOperationStrategyType, DriveOperationJobExecutor> executors;

	public DriveOperationJobRunner(DriveOperationJobExecutionStore driveOperationJobExecutionStore,
			DriveOperationJobStateService driveOperationJobStateService,
			DriveOperationRetryPolicy driveOperationRetryPolicy,
			DriveOperationLeaseHeartbeatService driveOperationLeaseHeartbeatService,
			List<DriveOperationJobExecutor> executors) {

		this.driveOperationJobExecutionStore = driveOperationJobExecutionStore;

		this.driveOperationJobStateService = driveOperationJobStateService;

		this.driveOperationRetryPolicy = driveOperationRetryPolicy;

		this.driveOperationLeaseHeartbeatService = driveOperationLeaseHeartbeatService;

		this.executors = buildExecutorMap(executors);
	}

	public void run(Long jobId, String workerId) {

		driveOperationLeaseHeartbeatService.register(jobId, workerId);

		try {

			DriveOperationJobExecutionSnapshot job = driveOperationJobExecutionStore.findExecutionSnapshot(jobId)
				.orElseThrow(() -> new IllegalStateException("Claimed Drive operation job no longer exists"));

			driveOperationJobStateService.publishClaimed(jobId);

			DriveOperationJobExecutor executor = executors.get(job.strategyType());

			if (executor == null) {

				throw new IllegalStateException(
						"No Drive operation executor registered for strategy " + job.strategyType());
			}

			executor.execute(job, workerId);

		}
		catch (DriveOperationLeaseLostException exception) {

			LOGGER.warn("Stopped Drive operation because worker lease was lost. jobId={}", jobId);

		}
		catch (Exception exception) {

			handleFailure(jobId, workerId, exception);

		}
		finally {

			driveOperationLeaseHeartbeatService.unregister(jobId, workerId);
		}
	}

	private void handleFailure(Long jobId, String workerId, Exception exception) {

		DriveOperationJobExecutionSnapshot current = driveOperationJobExecutionStore.findExecutionSnapshot(jobId)
			.orElse(null);

		if (current == null || isTerminal(current.status())) {

			return;
		}

		boolean retryable = driveOperationRetryPolicy.shouldRetry(exception);

		String errorCode = driveOperationRetryPolicy.errorCode(exception);

		String errorMessage = driveOperationRetryPolicy.errorMessage(exception);

		if (retryable && current.attemptCount() < current.maxAttempts()) {

			LocalDateTime nextAttemptAt = LocalDateTime.now(ZoneOffset.UTC)
				.plus(driveOperationRetryPolicy.retryDelay(current.attemptCount()));

			try {

				driveOperationJobStateService.scheduleRetry(jobId, workerId, nextAttemptAt, errorCode, errorMessage);

			}
			catch (DriveOperationLeaseLostException ignored) {

				LOGGER.warn("Could not schedule retry because worker lease was lost. jobId={}", jobId);
			}

			return;
		}

		DriveOperationJobStatus terminalStatus = determineTerminalStatus(current.status(), retryable);

		try {

			driveOperationJobStateService.fail(jobId, workerId, terminalStatus, errorCode, errorMessage);

		}
		catch (DriveOperationLeaseLostException ignored) {

			LOGGER.warn("Could not mark job failed because worker lease was lost. jobId={}", jobId);
		}

		LOGGER.error("Drive operation failed. jobId={}, status={}, errorCode={}", jobId, terminalStatus, errorCode,
				exception);
	}

	private DriveOperationJobStatus determineTerminalStatus(DriveOperationJobStatus currentStatus, boolean retryable) {

		if (retryable && (currentStatus == DriveOperationJobStatus.RUNNING
				|| currentStatus == DriveOperationJobStatus.VERIFYING
				|| currentStatus == DriveOperationJobStatus.COMMITTING)) {

			return DriveOperationJobStatus.CLEANUP_REQUIRED;
		}

		return DriveOperationJobStatus.FAILED;
	}

	private Map<DriveOperationStrategyType, DriveOperationJobExecutor> buildExecutorMap(
			List<DriveOperationJobExecutor> executorList) {

		Map<DriveOperationStrategyType, DriveOperationJobExecutor> result = new EnumMap<>(
				DriveOperationStrategyType.class);

		for (DriveOperationJobExecutor executor : executorList) {

			DriveOperationJobExecutor previous = result.put(executor.strategyType(), executor);

			if (previous != null) {

				throw new IllegalStateException(
						"Multiple Drive operation executors registered for " + executor.strategyType());
			}
		}

		return Map.copyOf(result);
	}

	private boolean isTerminal(DriveOperationJobStatus status) {

		return status == DriveOperationJobStatus.COMPLETED || status == DriveOperationJobStatus.PARTIAL
				|| status == DriveOperationJobStatus.FAILED || status == DriveOperationJobStatus.CANCELLED
				|| status == DriveOperationJobStatus.CLEANUP_REQUIRED;
	}

}
