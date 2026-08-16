package com.multidrive.api.model;

import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;

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

		Long destinationSourceId,

		Long destinationConnectionId,

		Long destinationParentItemId,

		String destinationParentGoogleFileId,

		Integer attemptCount,

		Integer maxAttempts,

		Boolean cancelRequested) {
}
