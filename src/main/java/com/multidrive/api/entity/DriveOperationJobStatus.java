package com.multidrive.api.entity;

public enum DriveOperationJobStatus {

    QUEUED,

    VALIDATING,

    PLANNING,

    RUNNING,

    VERIFYING,

    COMMITTING,

    COMPLETED,

    PARTIAL,

    FAILED,

    CANCELLED,

    CLEANUP_REQUIRED
}
