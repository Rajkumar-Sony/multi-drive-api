package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationEventResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.model.DriveOperationJobProgressSnapshot;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DriveOperationJobProgressPublisherTest {

	@Test
	void publishMapsProgressSnapshotToSseEvent() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationSseService sseService = mock(DriveOperationSseService.class);
		DriveOperationJobProgressPublisher publisher = new DriveOperationJobProgressPublisher(store, sseService);

		when(store.findProgressSnapshot(10L)).thenReturn(Optional
			.of(new DriveOperationJobProgressSnapshot(10L, "google-subject-1", DriveOperationJobStatus.RUNNING, 5L, 2L,
					1L, 100L, 40L, 2, 3, false, "ERROR_CODE", "error", LocalDateTime.of(2026, 8, 16, 12, 0))));

		publisher.publish(10L, "copying");

		ArgumentCaptor<DriveOperationEventResponse> eventCaptor = ArgumentCaptor
			.forClass(DriveOperationEventResponse.class);

		verify(sseService).publish(org.mockito.ArgumentMatchers.eq("google-subject-1"), eventCaptor.capture());
		assertThat(eventCaptor.getValue().eventType()).isEqualTo("DRIVE_OPERATION");
		assertThat(eventCaptor.getValue().jobId()).isEqualTo(10L);
		assertThat(eventCaptor.getValue().status()).isEqualTo(DriveOperationJobStatus.RUNNING);
		assertThat(eventCaptor.getValue().completedItems()).isEqualTo(2L);
		assertThat(eventCaptor.getValue().message()).isEqualTo("copying");
		assertThat(eventCaptor.getValue().occurredAt()).isNotNull();
	}

	@Test
	void publishSkipsWhenSnapshotIsMissing() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationSseService sseService = mock(DriveOperationSseService.class);
		DriveOperationJobProgressPublisher publisher = new DriveOperationJobProgressPublisher(store, sseService);

		when(store.findProgressSnapshot(10L)).thenReturn(Optional.empty());

		publisher.publish(10L, "copying");

		verifyNoInteractions(sseService);
	}

}
