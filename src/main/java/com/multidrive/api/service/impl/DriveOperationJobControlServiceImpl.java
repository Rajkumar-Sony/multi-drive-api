package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;
import com.multidrive.api.service.DriveOperationJobControlService;
import com.multidrive.api.service.DriveOperationJobProgressPublisher;
import com.multidrive.api.service.DriveOperationJobService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class DriveOperationJobControlServiceImpl implements DriveOperationJobControlService {

	private final DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	private final DriveOperationJobService driveOperationJobService;

	private final DriveOperationJobProgressPublisher driveOperationJobProgressPublisher;

	public DriveOperationJobControlServiceImpl(DriveOperationJobExecutionStore driveOperationJobExecutionStore,
			DriveOperationJobService driveOperationJobService,
			DriveOperationJobProgressPublisher driveOperationJobProgressPublisher) {

		this.driveOperationJobExecutionStore = driveOperationJobExecutionStore;

		this.driveOperationJobService = driveOperationJobService;

		this.driveOperationJobProgressPublisher = driveOperationJobProgressPublisher;
	}

	@Override
	public DriveOperationJobResponse requestCancellation(String googleSubjectId, Long jobId) {

		if (googleSubjectId == null || googleSubjectId.isBlank()) {

			throw new IllegalArgumentException("googleSubjectId is required");
		}

		if (jobId == null) {

			throw new IllegalArgumentException("jobId is required");
		}

		boolean changed = driveOperationJobExecutionStore.requestCancellation(googleSubjectId, jobId,
				LocalDateTime.now(ZoneOffset.UTC));

		DriveOperationJobResponse response = driveOperationJobService.getJob(googleSubjectId, jobId);

		if (changed) {

			driveOperationJobProgressPublisher.publish(jobId, response.status() == DriveOperationJobStatus.CANCELLED
					? "Queued operation cancelled" : "Cancellation requested");
		}

		return response;
	}

}
