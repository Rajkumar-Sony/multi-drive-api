package com.multidrive.api.controller;

import com.multidrive.api.dto.GoogleDriveAccountResponse;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveConnectionService;
import com.multidrive.api.service.GoogleDriveOAuthService;
import com.multidrive.api.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/google/accounts")
public class GoogleAccountController {

    private static final String OAUTH_STATE_SESSION_KEY =
            "GOOGLE_DRIVE_OAUTH_STATE";

    private final GoogleDriveOAuthService googleDriveOAuthService;
    private final GoogleDriveConnectionService googleDriveConnectionService;
    private final UserService userService;

    public GoogleAccountController(
            GoogleDriveOAuthService googleDriveOAuthService,
            GoogleDriveConnectionService googleDriveConnectionService,
            UserService userService
    ) {
        this.googleDriveOAuthService = googleDriveOAuthService;
        this.googleDriveConnectionService = googleDriveConnectionService;
        this.userService = userService;
    }

    @GetMapping("/connect")
    public void connectGoogleDrive(
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {

        String state = generateState();

        session.setAttribute(
                OAUTH_STATE_SESSION_KEY,
                state
        );

        String authorizationUrl =
                googleDriveOAuthService.buildAuthorizationUrl(state);

        response.sendRedirect(authorizationUrl);
    }

    @GetMapping
    public List<GoogleDriveAccountResponse> getConnectedAccounts(
            @AuthenticationPrincipal OidcUser oidcUser
    ) {

        User user = userService.findByGoogleSubjectId(
                oidcUser.getSubject()
        );

        return googleDriveConnectionService
                .getConnectedAccounts(user.getId());
    }

    private String generateState() {

        byte[] randomBytes = new byte[32];

        new SecureRandom()
                .nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}