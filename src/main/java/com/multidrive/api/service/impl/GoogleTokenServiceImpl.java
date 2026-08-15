package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleRefreshTokenResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.GoogleTokenService;
import com.multidrive.api.service.TokenEncryptionService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class GoogleTokenServiceImpl
        implements GoogleTokenService {

    private static final String GOOGLE_TOKEN_URL =
            "https://oauth2.googleapis.com/token";

    // Refresh early so a token does not expire during an in-flight Drive request.
    private static final long EXPIRY_BUFFER_MINUTES = 1;

    private final GoogleDriveConnectionRepository
            googleDriveConnectionRepository;

    private final TokenEncryptionService tokenEncryptionService;

    private final RestClient restClient;

    private final String clientId;
    private final String clientSecret;

    public GoogleTokenServiceImpl(
            GoogleDriveConnectionRepository googleDriveConnectionRepository,
            TokenEncryptionService tokenEncryptionService,

            @Value("${spring.security.oauth2.client.registration.google.client-id}")
            String clientId,

            @Value("${spring.security.oauth2.client.registration.google.client-secret}")
            String clientSecret
    ) {

        this.googleDriveConnectionRepository =
                googleDriveConnectionRepository;

        this.tokenEncryptionService =
                tokenEncryptionService;

        this.clientId = clientId;
        this.clientSecret = clientSecret;

        this.restClient = RestClient.create();
    }

    @Override
    @Transactional
    public String getValidAccessToken(
            Long connectionId,
            Long userId
    ) {

        GoogleDriveConnection connection =
                googleDriveConnectionRepository
                        .findByIdAndUserId(
                                connectionId,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Google Drive connection not found"
                                )
                        );

        if (isAccessTokenValid(connection)) {

            return tokenEncryptionService.decrypt(
                    connection.getEncryptedAccessToken()
            );
        }

        return refreshAccessToken(connection);
    }

    private boolean isAccessTokenValid(
            GoogleDriveConnection connection
    ) {

        if (connection.getEncryptedAccessToken() == null
                || connection.getAccessTokenExpiry() == null) {

            return false;
        }

        LocalDateTime now =
                LocalDateTime.now(ZoneOffset.UTC);

        return connection
                .getAccessTokenExpiry()
                .isAfter(
                        now.plusMinutes(
                                EXPIRY_BUFFER_MINUTES
                        )
                );
    }

    private String refreshAccessToken(
            GoogleDriveConnection connection
    ) {

        String refreshToken =
                tokenEncryptionService.decrypt(
                        connection.getEncryptedRefreshToken()
                );

        if (refreshToken == null
                || refreshToken.isBlank()) {

            throw new IllegalStateException(
                    "Refresh token is not available. Reconnect the Google Drive account."
            );
        }

        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add(
                "client_id",
                clientId
        );

        formData.add(
                "client_secret",
                clientSecret
        );

        formData.add(
                "refresh_token",
                refreshToken
        );

        formData.add(
                "grant_type",
                "refresh_token"
        );

        GoogleRefreshTokenResponse response =
                restClient
                        .post()
                        .uri(GOOGLE_TOKEN_URL)
                        .contentType(
                                MediaType.APPLICATION_FORM_URLENCODED
                        )
                        .body(formData)
                        .retrieve()
                        .body(
                                GoogleRefreshTokenResponse.class
                        );

        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {

            throw new IllegalStateException(
                    "Google access token refresh failed"
            );
        }

        connection.setEncryptedAccessToken(
                tokenEncryptionService.encrypt(
                        response.accessToken()
                )
        );

        if (response.expiresIn() != null) {

            connection.setAccessTokenExpiry(
                    LocalDateTime
                            .now(ZoneOffset.UTC)
                            .plusSeconds(
                                    response.expiresIn()
                            )
            );
        }

        if (response.scope() != null
                && !response.scope().isBlank()) {

            connection.setScopes(
                    response.scope()
            );
        }

        connection.setStatus("CONNECTED");

        googleDriveConnectionRepository.save(
                connection
        );

        return response.accessToken();
    }
}