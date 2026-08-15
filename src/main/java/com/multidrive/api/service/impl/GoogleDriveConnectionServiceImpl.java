package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveAccountResponse;
import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.User;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.GoogleDriveConnectionService;
import com.multidrive.api.service.TokenEncryptionService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class GoogleDriveConnectionServiceImpl
        implements GoogleDriveConnectionService {

    private final GoogleDriveConnectionRepository googleDriveConnectionRepository;
    private final TokenEncryptionService tokenEncryptionService;

    public GoogleDriveConnectionServiceImpl(
            GoogleDriveConnectionRepository googleDriveConnectionRepository,
            TokenEncryptionService tokenEncryptionService
    ) {
        this.googleDriveConnectionRepository =
                googleDriveConnectionRepository;

        this.tokenEncryptionService =
                tokenEncryptionService;
    }

    @Override
    @Transactional
    public GoogleDriveConnection saveOrUpdateConnection(
            User user,
            GoogleUserInfoResponse googleUserInfo,
            GoogleTokenResponse tokenResponse
    ) {

        validateInput(
                user,
                googleUserInfo,
                tokenResponse
        );

        GoogleDriveConnection connection =
                googleDriveConnectionRepository
                        .findByUserIdAndGoogleSubjectId(
                                user.getId(),
                                googleUserInfo.sub()
                        )
                        .orElseGet(GoogleDriveConnection::new);

        boolean newConnection =
                connection.getId() == null;

        connection.setUser(user);

        connection.setGoogleSubjectId(
                googleUserInfo.sub()
        );

        connection.setGoogleEmail(
                googleUserInfo.email()
        );

        connection.setEncryptedAccessToken(
                tokenEncryptionService.encrypt(
                        tokenResponse.accessToken()
                )
        );

        /*
         * Google may not return a refresh token every
         * time an already connected account authorizes.
         *
         * If a new refresh token is returned:
         * replace the old one.
         *
         * If no refresh token is returned for an existing
         * connection:
         * keep the existing encrypted refresh token.
         */
        if (tokenResponse.refreshToken() != null
                && !tokenResponse.refreshToken().isBlank()) {

            connection.setEncryptedRefreshToken(
                    tokenEncryptionService.encrypt(
                            tokenResponse.refreshToken()
                    )
            );

        } else if (newConnection) {

            throw new IllegalStateException(
                    "Google did not return a refresh token for the new Drive connection"
            );
        }

        if (tokenResponse.expiresIn() != null) {

            LocalDateTime expiryTime =
                    LocalDateTime.now(ZoneOffset.UTC)
                            .plusSeconds(
                                    tokenResponse.expiresIn()
                            );

            connection.setAccessTokenExpiry(
                    expiryTime
            );
        }

        connection.setScopes(
                tokenResponse.scope()
        );

        connection.setStatus(
                "CONNECTED"
        );

        return googleDriveConnectionRepository.save(
                connection
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoogleDriveAccountResponse> getConnectedAccounts(
            Long userId
    ) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required"
            );
        }

        return googleDriveConnectionRepository
                .findAllByUserId(userId)
                .stream()
                .map(this::mapToAccountResponse)
                .toList();
    }

    private GoogleDriveAccountResponse mapToAccountResponse(
            GoogleDriveConnection connection
    ) {

        // Access and refresh tokens are intentionally excluded from API responses.
        return new GoogleDriveAccountResponse(
                connection.getId(),
                connection.getGoogleEmail(),
                connection.getStatus(),
                connection.getAccessTokenExpiry()
        );
    }

    private void validateInput(
            User user,
            GoogleUserInfoResponse googleUserInfo,
            GoogleTokenResponse tokenResponse
    ) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "Application user is required"
            );
        }

        if (googleUserInfo == null) {
            throw new IllegalArgumentException(
                    "Google user information is required"
            );
        }

        if (googleUserInfo.sub() == null
                || googleUserInfo.sub().isBlank()) {

            throw new IllegalArgumentException(
                    "Google subject ID is required"
            );
        }

        if (googleUserInfo.email() == null
                || googleUserInfo.email().isBlank()) {

            throw new IllegalArgumentException(
                    "Google email is required"
            );
        }

        if (tokenResponse == null) {
            throw new IllegalArgumentException(
                    "Google token response is required"
            );
        }

        if (tokenResponse.accessToken() == null
                || tokenResponse.accessToken().isBlank()) {

            throw new IllegalArgumentException(
                    "Google access token is required"
            );
        }
    }
}
