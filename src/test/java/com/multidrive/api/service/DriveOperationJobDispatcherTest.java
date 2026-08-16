package com.multidrive.api.service;

import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DriveOperationJobDispatcherTest {

	@Test
	void dispatchClaimsAndSubmitsAvailableJob() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationJobRunner runner = mock(DriveOperationJobRunner.class);
		DriveOperationWorkerIdentity workerIdentity = mock(DriveOperationWorkerIdentity.class);
		ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
		DriveOperationJobDispatcher dispatcher = new DriveOperationJobDispatcher(store, runner, workerIdentity,
				executor);

		when(workerIdentity.getWorkerId()).thenReturn("worker-1");
		when(store.claimNextNativeOperation(eq("worker-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(Optional.of(10L), Optional.empty());

		dispatcher.dispatch();

		verify(executor).execute(any(Runnable.class));
	}

	@Test
	void dispatchReleasesClaimWhenExecutorRejectsTask() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationJobRunner runner = mock(DriveOperationJobRunner.class);
		DriveOperationWorkerIdentity workerIdentity = mock(DriveOperationWorkerIdentity.class);
		ThreadPoolTaskExecutor executor = mock(ThreadPoolTaskExecutor.class);
		DriveOperationJobDispatcher dispatcher = new DriveOperationJobDispatcher(store, runner, workerIdentity,
				executor);

		when(workerIdentity.getWorkerId()).thenReturn("worker-1");
		when(store.claimNextNativeOperation(eq("worker-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(Optional.of(10L));
		doThrow(new TaskRejectedException("queue full")).when(executor).execute(any(Runnable.class));

		dispatcher.dispatch();

		verify(store).releaseRejectedClaim(eq(10L), eq("worker-1"), any(LocalDateTime.class), any(LocalDateTime.class));
	}

}
