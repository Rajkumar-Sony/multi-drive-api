package com.multidrive.api.dto;

import com.multidrive.api.entity.DriveConflictStrategy;
import com.multidrive.api.entity.DriveOperationType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DriveOperationJobSubmitRequest(

		@NotNull(message = "operationType is required") DriveOperationType operationType,

		@NotNull(message = "sourceItemId is required") Long sourceItemId,

		@NotNull(message = "destinationSourceId is required") Long destinationSourceId,

		Long destinationParentItemId,

		@Size(max = 255, message = "name must not exceed 255 characters") String name,

		DriveConflictStrategy conflictStrategy) {
}
