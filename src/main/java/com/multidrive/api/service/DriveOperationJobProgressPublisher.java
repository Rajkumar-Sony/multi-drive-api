package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationEventResponse;
import com.multidrive.api.model.DriveOperationJobProgressSnapshot;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class DriveOperationJobProgressPublisher {

	private final DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	private final DriveOperationSseService driveOperationSseService;

	public DriveOperationJobProgressPublisher(DriveOperationJobExecutionStore driveOperationJobExecutionStore,
			DriveOperationSseService driveOperationSseService) {

		this.driveOperationJobExecutionStore = driveOperationJobExecutionStore;

		this.driveOperationSseService = driveOperationSseService;
	}

	public void publish(Long jobId, String message) {

		driveOperationJobExecutionStore.findProgressSnapshot(jobId)
			.ifPresent(snapshot -> publishSnapshot(snapshot, message));
	}

	private void publishSnapshot(DriveOperationJobProgressSnapshot snapshot, String message) {

		DriveOperationEventResponse event = new DriveOperationEventResponse("DRIVE_OPERATION", snapshot.jobId(),
				snapshot.status(), snapshot.totalItems(), snapshot.completedItems(), snapshot.failedItems(),
				snapshot.totalBytes(), snapshot.transferredBytes(), snapshot.attemptCount(), snapshot.maxAttempts(),
				snapshot.cancelRequested(), snapshot.errorCode(), snapshot.errorMessage(), message, Instant.now());

		driveOperationSseService.publish(snapshot.googleSubjectId(), event);
	}

}
