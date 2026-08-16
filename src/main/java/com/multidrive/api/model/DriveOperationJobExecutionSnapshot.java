package com.multidrive.api.model;

import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveSourceType;

import java.time.LocalDateTime;

public record DriveOperationJobExecutionSnapshot(

		Long jobId,

		Long userId,

		String googleSubjectId,

		DriveOperationType operationType,

		DriveOperationStrategyType strategyType,

		DriveOperationJobStatus status,

		Long sourceItemId,

		Long sourceConnectionId,

		Long sourceSourceId,

		String sourceGoogleFileId,

		String sourceName,

		String sourceMimeType,

		Long destinationSourceId,

		Long destinationConnectionId,

		GoogleDriveSourceType destinationSourceType,

		String destinationGoogleDriveId,

		Long destinationParentItemId,

		String destinationParentGoogleFileId,

		String requestedName,

		Integer attemptCount,

		Integer maxAttempts,

		Boolean cancelRequested,

		String errorCode,

		LocalDateTime createdAt) {
}
