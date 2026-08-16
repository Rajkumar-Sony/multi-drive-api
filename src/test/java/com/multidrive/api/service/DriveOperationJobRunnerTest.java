package com.multidrive.api.service;

import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DriveOperationJobRunnerTest {

	@Test
	void runExecutesRegisteredStrategyAndAlwaysUnregistersHeartbeat() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationJobStateService stateService = mock(DriveOperationJobStateService.class);
		DriveOperationLeaseHeartbeatService heartbeatService = mock(DriveOperationLeaseHeartbeatService.class);
		DriveOperationJobExecutor executor = executor(DriveOperationStrategyType.NATIVE_MOVE);
		DriveOperationJobRunner runner = runner(store, stateService, heartbeatService, executor);
		DriveOperationJobExecutionSnapshot job = job(DriveOperationJobStatus.VALIDATING, 1, 3);

		when(store.findExecutionSnapshot(10L)).thenReturn(Optional.of(job));

		runner.run(10L, "worker-1");

		verify(heartbeatService).register(10L, "worker-1");
		verify(stateService).publishClaimed(10L);
		verify(executor).execute(job, "worker-1");
		verify(heartbeatService).unregister(10L, "worker-1");
	}

	@Test
	void runSchedulesRetryForRetryableFailureBeforeMaxAttempts() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationJobStateService stateService = mock(DriveOperationJobStateService.class);
		DriveOperationLeaseHeartbeatService heartbeatService = mock(DriveOperationLeaseHeartbeatService.class);
		DriveOperationJobExecutor executor = executor(DriveOperationStrategyType.NATIVE_MOVE);
		DriveOperationJobRunner runner = runner(store, stateService, heartbeatService, executor);
		DriveOperationJobExecutionSnapshot job = job(DriveOperationJobStatus.RUNNING, 1, 3);

		when(store.findExecutionSnapshot(10L)).thenReturn(Optional.of(job), Optional.of(job));
		doThrow(new ResourceAccessException("timeout")).when(executor).execute(job, "worker-1");

		runner.run(10L, "worker-1");

		verify(stateService).scheduleRetry(eq(10L), eq("worker-1"), any(LocalDateTime.class),
				eq("GOOGLE_NETWORK_ERROR"), eq("timeout"));
		verify(heartbeatService).unregister(10L, "worker-1");
	}

	@Test
	void runMarksCleanupRequiredForExhaustedRetryableRunningFailure() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationJobStateService stateService = mock(DriveOperationJobStateService.class);
		DriveOperationLeaseHeartbeatService heartbeatService = mock(DriveOperationLeaseHeartbeatService.class);
		DriveOperationJobExecutor executor = executor(DriveOperationStrategyType.NATIVE_MOVE);
		DriveOperationJobRunner runner = runner(store, stateService, heartbeatService, executor);
		DriveOperationJobExecutionSnapshot job = job(DriveOperationJobStatus.RUNNING, 3, 3);

		when(store.findExecutionSnapshot(10L)).thenReturn(Optional.of(job), Optional.of(job));
		doThrow(new ResourceAccessException("timeout")).when(executor).execute(job, "worker-1");

		runner.run(10L, "worker-1");

		verify(stateService).fail(10L, "worker-1", DriveOperationJobStatus.CLEANUP_REQUIRED, "GOOGLE_NETWORK_ERROR",
				"timeout");
	}

	@Test
	void constructorRejectsDuplicateStrategyExecutors() {

		DriveOperationJobExecutor firstExecutor = executor(DriveOperationStrategyType.NATIVE_MOVE);
		DriveOperationJobExecutor secondExecutor = executor(DriveOperationStrategyType.NATIVE_MOVE);

		assertThatThrownBy(
				() -> runner(mock(DriveOperationJobExecutionStore.class), mock(DriveOperationJobStateService.class),
						mock(DriveOperationLeaseHeartbeatService.class), firstExecutor, secondExecutor))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Multiple Drive operation executors registered for NATIVE_MOVE");
	}

	private DriveOperationJobRunner runner(DriveOperationJobExecutionStore store,
			DriveOperationJobStateService stateService, DriveOperationLeaseHeartbeatService heartbeatService,
			DriveOperationJobExecutor... executors) {

		return new DriveOperationJobRunner(store, stateService, new DriveOperationRetryPolicy(), heartbeatService,
				List.of(executors));
	}

	private DriveOperationJobExecutor executor(DriveOperationStrategyType strategyType) {

		DriveOperationJobExecutor executor = mock(DriveOperationJobExecutor.class);

		when(executor.strategyType()).thenReturn(strategyType);

		return executor;
	}

	private DriveOperationJobExecutionSnapshot job(DriveOperationJobStatus status, int attemptCount, int maxAttempts) {

		return new DriveOperationJobExecutionSnapshot(10L, 42L, "google-subject-123", DriveOperationType.MOVE,
				DriveOperationStrategyType.NATIVE_MOVE, status, 123L, 20L, 30L, "file-id", 30L, 20L, null, "root-id",
				attemptCount, maxAttempts, false);
	}

}
