package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveRealtimeEventResponse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface GoogleDriveSseService {

	SseEmitter subscribe(Long userId);

	void publishDriveChanges(Long userId, GoogleDriveRealtimeEventResponse event);

}