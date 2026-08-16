package com.multidrive.api.model;

import com.multidrive.api.entity.DriveOperationJobStatus;

import java.time.LocalDateTime;

public record DriveOperationJobProgressSnapshot(

        Long jobId,

        String googleSubjectId,

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

        LocalDateTime updatedAt
) {
}
