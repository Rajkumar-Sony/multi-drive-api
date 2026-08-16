package com.multidrive.api.model;

public record DriveOperationPlan(

        DriveOperationStrategyType strategyType,

        String reason
) {
}
