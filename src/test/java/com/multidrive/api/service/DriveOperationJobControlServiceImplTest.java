package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.entity.DriveConflictStrategy;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;
import com.multidrive.api.service.impl.DriveOperationJobControlServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveOperationJobControlServiceImplTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Mock
	private DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	@Mock
	private DriveOperationJobService driveOperationJobService;

	@Mock
	private DriveOperationJobProgressPublisher driveOperationJobProgressPublisher;

	private DriveOperationJobControlServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new DriveOperationJobControlServiceImpl(driveOperationJobExecutionStore, driveOperationJobService,
				driveOperationJobProgressPublisher);
	}

	@Test
	void requestCancellationPublishesQueuedCancellationMessageWhenJobBecomesCancelled() {

		DriveOperationJobResponse response = response(DriveOperationJobStatus.CANCELLED);

		when(driveOperationJobExecutionStore.requestCancellation(org.mockito.ArgumentMatchers.eq(GOOGLE_SUBJECT_ID),
				org.mockito.ArgumentMatchers.eq(10L), any()))
			.thenReturn(true);
		when(driveOperationJobService.getJob(GOOGLE_SUBJECT_ID, 10L)).thenReturn(response);

		assertThat(service.requestCancellation(GOOGLE_SUBJECT_ID, 10L)).isSameAs(response);

		verify(driveOperationJobProgressPublisher).publish(10L, "Queued operation cancelled");
	}

	@Test
	void requestCancellationPublishesRequestedMessageForRunningJob() {

		DriveOperationJobResponse response = response(DriveOperationJobStatus.RUNNING);

		when(driveOperationJobExecutionStore.requestCancellation(org.mockito.ArgumentMatchers.eq(GOOGLE_SUBJECT_ID),
				org.mockito.ArgumentMatchers.eq(10L), any()))
			.thenReturn(true);
		when(driveOperationJobService.getJob(GOOGLE_SUBJECT_ID, 10L)).thenReturn(response);

		assertThat(service.requestCancellation(GOOGLE_SUBJECT_ID, 10L)).isSameAs(response);

		verify(driveOperationJobProgressPublisher).publish(10L, "Cancellation requested");
	}

	@Test
	void requestCancellationReturnsLatestJobWithoutPublishingWhenStateDoesNotChange() {

		DriveOperationJobResponse response = response(DriveOperationJobStatus.COMPLETED);

		when(driveOperationJobExecutionStore.requestCancellation(org.mockito.ArgumentMatchers.eq(GOOGLE_SUBJECT_ID),
				org.mockito.ArgumentMatchers.eq(10L), any()))
			.thenReturn(false);
		when(driveOperationJobService.getJob(GOOGLE_SUBJECT_ID, 10L)).thenReturn(response);

		assertThat(service.requestCancellation(GOOGLE_SUBJECT_ID, 10L)).isSameAs(response);

		verifyNoInteractions(driveOperationJobProgressPublisher);
	}

	@Test
	void requestCancellationRejectsInvalidInputs() {

		assertThatThrownBy(() -> service.requestCancellation(" ", 10L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("googleSubjectId is required");

		assertThatThrownBy(() -> service.requestCancellation(GOOGLE_SUBJECT_ID, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("jobId is required");

		verifyNoMoreInteractions(driveOperationJobExecutionStore, driveOperationJobService,
				driveOperationJobProgressPublisher);
	}

	private DriveOperationJobResponse response(DriveOperationJobStatus status) {

		LocalDateTime now = LocalDateTime.of(2026, 8, 16, 12, 0);

		return new DriveOperationJobResponse(10L, DriveOperationType.MOVE, DriveOperationStrategyType.NATIVE_MOVE,
				status, DriveConflictStrategy.KEEP_BOTH, 123L, "Report.pdf", "application/pdf", 1L, 2L, 456L,
				"destination-parent", null, null, null, 1L, 0L, 0L, 1024L, 0L, 0, 3,
				status == DriveOperationJobStatus.CANCELLED, null, null, null, null,
				status == DriveOperationJobStatus.CANCELLED ? now : null, now, now);
	}

}
