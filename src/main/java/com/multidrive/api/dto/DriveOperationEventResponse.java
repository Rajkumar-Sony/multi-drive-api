package com.multidrive.api.dto;

import com.multidrive.api.entity.DriveOperationJobStatus;

import java.time.Instant;

public record DriveOperationEventResponse(

        String eventType,

        Long jobId,

        DriveOperationJobStatus status,

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

        String message,

        Instant occurredAt
) {
}
