package com.multidrive.api.dto;

import com.multidrive.api.entity.DriveConflictStrategy;
import com.multidrive.api.entity.DriveOperationType;

public record DriveOperationJobSubmitRequest(

        DriveOperationType operationType,

        Long sourceItemId,

        Long destinationSourceId,

        Long destinationParentItemId,

        String name,

        DriveConflictStrategy conflictStrategy
) {
}
