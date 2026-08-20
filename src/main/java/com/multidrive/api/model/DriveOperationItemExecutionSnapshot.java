package com.multidrive.api.model;

public record DriveOperationItemExecutionSnapshot(

		Long id,

		Integer sequenceNo,

		Long sourceLocalItemId,

		String sourceGoogleFileId,

		String sourceName,

		String sourceMimeType,

		String sourceParentGoogleFileId,

		String sourcePath,

		String destinationParentGoogleFileId,

		Long destinationLocalItemId,

		String destinationGoogleFileId,

		Long sizeBytes,

		Integer attemptCount,

		String errorCode,

		Boolean mutationStarted) {
}
