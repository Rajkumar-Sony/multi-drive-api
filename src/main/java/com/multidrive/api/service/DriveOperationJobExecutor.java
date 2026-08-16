package com.multidrive.api.service;

import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;

public interface DriveOperationJobExecutor {

    DriveOperationStrategyType strategyType();

    void execute(
            DriveOperationJobExecutionSnapshot job,
            String workerId
    );
}
