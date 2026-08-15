package com.multidrive.api.controller;

import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveConnectionService;
import com.multidrive.api.service.GoogleDriveOAuthService;
import com.multidrive.api.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/google/oauth")
public class GoogleOAuthCallbackController {

    private static final String OAUTH_STATE_SESSION_KEY =
            "GOOGLE_DRIVE_OAUTH_STATE";

    private final GoogleDriveOAuthService googleDriveOAuthService;
    private final GoogleDriveConnectionService googleDriveConnectionService;
    private final UserService userService;

    public GoogleOAuthCallbackController(
            GoogleDriveOAuthService googleDriveOAuthService,
            GoogleDriveConnectionService googleDriveConnectionService,
            UserService userService
    ) {
        this.googleDriveOAuthService = googleDriveOAuthService;
        this.googleDriveConnectionService = googleDriveConnectionService;
        this.userService = userService;
    }

    @GetMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpSession session,
            @AuthenticationPrincipal OidcUser oidcUser
    ) {

        // 1. Check Google OAuth error
        if (error != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Google authorization failed",
                            "error", error
                    ));
        }

        // 2. Make sure application user is logged in
        if (oidcUser == null) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Application user is not authenticated"
                    ));
        }

        // 3. Validate OAuth state
        String expectedState =
                (String) session.getAttribute(OAUTH_STATE_SESSION_KEY);

        if (expectedState == null
                || state == null
                || !expectedState.equals(state)) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Invalid OAuth state"
                    ));
        }

        // 4. Remove state after validation
        session.removeAttribute(OAUTH_STATE_SESSION_KEY);

        // 5. Validate authorization code
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "ERROR",
                            "message", "Authorization code is missing"
                    ));
        }

        try {

            // 6. Find our logged-in application user
            User applicationUser =
                    userService.findByGoogleSubjectId(
                            oidcUser.getSubject()
                    );

            // 7. Exchange Google authorization code for tokens
            GoogleTokenResponse tokenResponse =
                    googleDriveOAuthService
                            .exchangeAuthorizationCode(code);

            if (tokenResponse.accessToken() == null
                    || tokenResponse.accessToken().isBlank()) {

                return ResponseEntity.internalServerError()
                        .body(Map.of(
                                "status", "ERROR",
                                "message",
                                "Google access token was not received"
                        ));
            }

            // 8. Find which Google account was connected
            GoogleUserInfoResponse googleUserInfo =
                    googleDriveOAuthService.getUserInfo(
                            tokenResponse.accessToken()
                    );

            if (googleUserInfo.sub() == null
                    || googleUserInfo.sub().isBlank()) {

                return ResponseEntity.internalServerError()
                        .body(Map.of(
                                "status", "ERROR",
                                "message",
                                "Unable to identify connected Google account"
                        ));
            }

            // 9. Save/update Google Drive connection
            GoogleDriveConnection connection =
                    googleDriveConnectionService
                            .saveOrUpdateConnection(
                                    applicationUser,
                                    googleUserInfo,
                                    tokenResponse
                            );

            // 10. Return safe response
            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "status",
                    "SUCCESS"
            );

            response.put(
                    "message",
                    "Google Drive account connected successfully"
            );

            response.put(
                    "connectionId",
                    connection.getId()
            );

            response.put(
                    "email",
                    connection.getGoogleEmail()
            );

            response.put(
                    "connectionStatus",
                    connection.getStatus()
            );

            return ResponseEntity.ok(response);

        } catch (Exception ex) {

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "ERROR",
                            "message",
                            "Unable to connect Google Drive account",
                            "details",
                            ex.getMessage() != null
                                    ? ex.getMessage()
                                    : "Unknown error"
                    ));
        }
    }
}