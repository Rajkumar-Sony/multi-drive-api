package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationEventResponse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface DriveOperationSseService {

    SseEmitter subscribe(
            String googleSubjectId
    );

    void publish(
            String googleSubjectId,
            DriveOperationEventResponse event
    );
}
