package com.multidrive.api.controller;

import com.multidrive.api.service.DriveOperationSseService;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class DriveOperationEventController {

	private final DriveOperationSseService driveOperationSseService;

	public DriveOperationEventController(DriveOperationSseService driveOperationSseService) {

		this.driveOperationSseService = driveOperationSseService;
	}

	@GetMapping(value = "/operations", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(

			@AuthenticationPrincipal OidcUser oidcUser) {

		if (oidcUser == null || oidcUser.getSubject() == null || oidcUser.getSubject().isBlank()) {

			throw new IllegalStateException("Authenticated application user not found");
		}

		return driveOperationSseService.subscribe(oidcUser.getSubject());
	}

}
