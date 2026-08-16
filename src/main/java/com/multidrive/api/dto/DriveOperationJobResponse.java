package com.multidrive.api.dto;

import com.multidrive.api.entity.DriveConflictStrategy;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.model.DriveOperationStrategyType;

import java.time.LocalDateTime;

public record DriveOperationJobResponse(

        Long id,

        DriveOperationType operationType,

        DriveOperationStrategyType strategyType,

        DriveOperationJobStatus status,

        DriveConflictStrategy conflictStrategy,

        Long sourceItemId,

        String sourceName,

        String sourceMimeType,

        Long sourceSourceId,

        Long destinationSourceId,

        Long destinationParentItemId,

        String destinationParentGoogleFileId,

        String requestedName,

        Long resultItemId,

        String resultGoogleFileId,

        Long totalItems,

        Long completedItems,

        Long failedItems,

        Long totalBytes,

        Long transferredBytes,

        Integer attemptCount,

        Integer maxAttempts,

        Boolean cancelRequested,

        String errorCode,

        String errorMessage,

        LocalDateTime nextAttemptAt,

        LocalDateTime startedAt,

        LocalDateTime completedAt,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}
