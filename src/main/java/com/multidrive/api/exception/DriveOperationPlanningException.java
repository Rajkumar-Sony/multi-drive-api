package com.multidrive.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        HttpStatus.CONFLICT
)
public class DriveOperationPlanningException
        extends RuntimeException {

    public DriveOperationPlanningException(
            String message
    ) {

        super(
                message
        );
    }
}
