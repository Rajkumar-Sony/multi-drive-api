package com.multidrive.api.service;

import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.exception.DriveOperationLeaseLostException;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveOperationJobStateServiceTest {

	@Mock
	private DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	@Mock
	private DriveOperationJobProgressPublisher driveOperationJobProgressPublisher;

	private DriveOperationJobStateService service;

	@BeforeEach
	void setUp() {

		service = new DriveOperationJobStateService(driveOperationJobExecutionStore,
				driveOperationJobProgressPublisher);
	}

	@Test
	void transitionUpdatesOwnedLeaseAndPublishesMessage() {

		when(driveOperationJobExecutionStore.transition(eq(10L), eq("worker-1"), eq(DriveOperationJobStatus.RUNNING),
				any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(true);

		service.transition(10L, "worker-1", DriveOperationJobStatus.RUNNING, "Running job");

		verify(driveOperationJobProgressPublisher).publish(10L, "Running job");
	}

	@Test
	void transitionThrowsWhenWorkerLeaseIsLost() {

		when(driveOperationJobExecutionStore.transition(eq(10L), eq("worker-1"), eq(DriveOperationJobStatus.RUNNING),
				any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(false);

		assertThatThrownBy(() -> service.transition(10L, "worker-1", DriveOperationJobStatus.RUNNING, "Running job"))
			.isInstanceOf(DriveOperationLeaseLostException.class)
			.hasMessage("Drive operation worker lease was lost for job 10");

		verifyNoInteractions(driveOperationJobProgressPublisher);
	}

	@Test
	void cancelIfRequestedReturnsFalseWhenCancellationWasNotRequested() {

		when(driveOperationJobExecutionStore.isCancelRequested(10L)).thenReturn(false);

		assertThat(service.cancelIfRequested(10L, "worker-1")).isFalse();

		verifyNoInteractions(driveOperationJobProgressPublisher);
	}

	@Test
	void cancelIfRequestedCancelsOwnedJobAndPublishesMessage() {

		when(driveOperationJobExecutionStore.isCancelRequested(10L)).thenReturn(true);
		when(driveOperationJobExecutionStore.cancelOwnedJob(eq(10L), eq("worker-1"), any(LocalDateTime.class)))
			.thenReturn(true);

		assertThat(service.cancelIfRequested(10L, "worker-1")).isTrue();

		verify(driveOperationJobProgressPublisher).publish(10L, "Operation cancelled before Google mutation");
	}

	@Test
	void cancelIfRequestedThrowsWhenLeaseIsLost() {

		when(driveOperationJobExecutionStore.isCancelRequested(10L)).thenReturn(true);
		when(driveOperationJobExecutionStore.cancelOwnedJob(eq(10L), eq("worker-1"), any(LocalDateTime.class)))
			.thenReturn(false);

		assertThatThrownBy(() -> service.cancelIfRequested(10L, "worker-1"))
			.isInstanceOf(DriveOperationLeaseLostException.class)
			.hasMessage("Drive operation worker lease was lost for job 10");

		verifyNoInteractions(driveOperationJobProgressPublisher);
	}

	@Test
	void scheduleRetryAndFailureRequireOwnedLease() {

		when(driveOperationJobExecutionStore.scheduleRetry(eq(10L), eq("worker-1"), any(LocalDateTime.class),
				eq("TIMEOUT"), eq("Timed out"), any(LocalDateTime.class)))
			.thenReturn(false);
		when(driveOperationJobExecutionStore.markFailure(eq(10L), eq("worker-1"), eq(DriveOperationJobStatus.FAILED),
				eq("FAILED"), eq("Failed"), any(LocalDateTime.class)))
			.thenReturn(false);

		assertThatThrownBy(() -> service.scheduleRetry(10L, "worker-1", LocalDateTime.now(), "TIMEOUT", "Timed out"))
			.isInstanceOf(DriveOperationLeaseLostException.class);

		assertThatThrownBy(() -> service.fail(10L, "worker-1", DriveOperationJobStatus.FAILED, "FAILED", "Failed"))
			.isInstanceOf(DriveOperationLeaseLostException.class);
	}

	@Test
	void completeNativeMoveAndMarkerMethodsDelegateToExecutionStore() {

		service.publishClaimed(10L);
		service.markRootItemRunning(10L);
		service.markRootItemVerifying(10L);
		service.completeNativeMove(10L, "worker-1", 123L, "google-file-id");
		service.completeNativeCopy(11L, "worker-1", 124L, "copy-google-file-id");
		service.completeRecursiveCopy(12L, "worker-1", 125L, "recursive-google-file-id");
		service.publishProgress(13L, "Progress message");

		verify(driveOperationJobProgressPublisher).publish(10L, "Job claimed by background worker");
		verify(driveOperationJobExecutionStore).markRootItemRunning(10L);
		verify(driveOperationJobExecutionStore).markRootItemVerifying(10L);
		verify(driveOperationJobExecutionStore).completeNativeMove(eq(10L), eq("worker-1"), eq(123L),
				eq("google-file-id"), any(LocalDateTime.class));
		verify(driveOperationJobProgressPublisher).publish(10L, "Native Drive move completed");
		verify(driveOperationJobExecutionStore).completeNativeCopy(eq(11L), eq("worker-1"), eq(124L),
				eq("copy-google-file-id"), any(LocalDateTime.class));
		verify(driveOperationJobProgressPublisher).publish(11L, "Native Drive copy completed");
		verify(driveOperationJobExecutionStore).completeRecursiveCopy(eq(12L), eq("worker-1"), eq(125L),
				eq("recursive-google-file-id"), any(LocalDateTime.class));
		verify(driveOperationJobProgressPublisher).publish(12L, "Recursive Drive folder copy completed");
		verify(driveOperationJobProgressPublisher).publish(13L, "Progress message");
	}

}
