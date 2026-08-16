package com.multidrive.api.controller;

import com.multidrive.api.dto.GoogleDriveSourceResponse;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveSourceService;
import com.multidrive.api.service.UserService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(
        "/api/google/sources"
)
public class GoogleDriveSourceController {

    private final GoogleDriveSourceService
            googleDriveSourceService;

    private final UserService
            userService;

    public GoogleDriveSourceController(
            GoogleDriveSourceService
                    googleDriveSourceService,

            UserService
                    userService
    ) {

        this.googleDriveSourceService =
                googleDriveSourceService;

        this.userService =
                userService;
    }

    @GetMapping
    public List<GoogleDriveSourceResponse>
    getSources(

            @RequestParam(
                    required = false
            )
            Long connectionId,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        User user =
                getApplicationUser(
                        oidcUser
                );

        if (connectionId == null) {

            return googleDriveSourceService
                    .getActiveSourceResponses(
                            user.getId()
                    );
        }

        return googleDriveSourceService
                .getActiveSourceResponses(
                        user.getId(),
                        connectionId
                );
    }

    private User getApplicationUser(
            OidcUser oidcUser
    ) {

        if (oidcUser == null) {

            throw new IllegalStateException(
                    "Authenticated application user not found"
            );
        }

        return userService
                .findByGoogleSubjectId(
                        oidcUser.getSubject()
                );
    }
}