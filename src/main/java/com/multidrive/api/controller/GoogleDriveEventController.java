package com.multidrive.api.controller;

import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveSseService;
import com.multidrive.api.service.UserService;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/events")
public class GoogleDriveEventController {

    private final GoogleDriveSseService
            googleDriveSseService;

    private final UserService
            userService;

    public GoogleDriveEventController(
            GoogleDriveSseService googleDriveSseService,
            UserService userService
    ) {

        this.googleDriveSseService =
                googleDriveSseService;

        this.userService =
                userService;
    }

    @GetMapping(
            value = "/drive",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter subscribeToDriveEvents(
            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        if (oidcUser == null) {

            throw new IllegalStateException(
                    "Authenticated application user not found"
            );
        }

        User user =
                userService
                        .findByGoogleSubjectId(
                                oidcUser.getSubject()
                        );

        return googleDriveSseService
                .subscribe(
                        user.getId()
                );
    }
}