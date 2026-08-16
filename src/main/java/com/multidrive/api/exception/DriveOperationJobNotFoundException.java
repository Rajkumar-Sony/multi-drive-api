package com.multidrive.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(
        HttpStatus.NOT_FOUND
)
public class DriveOperationJobNotFoundException
        extends RuntimeException {

    public DriveOperationJobNotFoundException(
            Long jobId
    ) {

        super(
                "Drive operation job not found: "
                        + jobId
        );
    }
}
