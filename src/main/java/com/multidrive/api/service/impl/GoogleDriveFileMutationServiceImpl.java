package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileMutationRequest;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.service.GoogleDriveFileMutationService;
import com.multidrive.api.service.GoogleTokenService;
import com.multidrive.api.util.GoogleDriveFieldMasks;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Service
public class GoogleDriveFileMutationServiceImpl
        implements GoogleDriveFileMutationService {

    private static final String GOOGLE_DRIVE_FILES_URL =
            "https://www.googleapis.com/drive/v3/files";

    private static final String GOOGLE_FOLDER_MIME_TYPE =
            "application/vnd.google-apps.folder";

    private final GoogleTokenService
            googleTokenService;

    private final RestClient
            restClient;

    public GoogleDriveFileMutationServiceImpl(
            GoogleTokenService googleTokenService
    ) {

        this.googleTokenService =
                googleTokenService;

        this.restClient =
                RestClient.create();
    }

    @Override
    public GoogleDriveFileResponse getFile(
            Long connectionId,
            Long userId,
            String googleFileId
    ) {

        validateIdentifiers(
                connectionId,
                userId,
                googleFileId
        );

        String accessToken =
                googleTokenService
                        .getValidAccessToken(
                                connectionId,
                                userId
                        );

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                GOOGLE_DRIVE_FILES_URL
                                        + "/"
                                        + googleFileId
                        )
                        .queryParam(
                                "supportsAllDrives",
                                true
                        )
                        .queryParam(
                                "fields",
                                GoogleDriveFieldMasks.FILE_RESOURCE
                        )
                        .build()
                        .encode()
                        .toUri();

        GoogleDriveFileResponse response =
                restClient
                        .get()
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .retrieve()
                        .body(
                                GoogleDriveFileResponse.class
                        );

        return requireFileResponse(
                response,
                "Google Drive returned an empty file response"
        );
    }

    @Override
    public GoogleDriveFileResponse createFolder(
            Long connectionId,
            Long userId,
            String parentGoogleFileId,
            String name
    ) {

        validateIdentifiers(
                connectionId,
                userId,
                parentGoogleFileId
        );

        String normalizedName =
                normalizeName(
                        name
                );

        String accessToken =
                googleTokenService
                        .getValidAccessToken(
                                connectionId,
                                userId
                        );

        GoogleDriveFileMutationRequest request =
                new GoogleDriveFileMutationRequest(
                        normalizedName,
                        GOOGLE_FOLDER_MIME_TYPE,
                        List.of(
                                parentGoogleFileId
                        ),
                        null
                );

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                GOOGLE_DRIVE_FILES_URL
                        )
                        .queryParam(
                                "supportsAllDrives",
                                true
                        )
                        .queryParam(
                                "fields",
                                GoogleDriveFieldMasks.FILE_RESOURCE
                        )
                        .build()
                        .encode()
                        .toUri();

        GoogleDriveFileResponse response =
                restClient
                        .post()
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .body(
                                request
                        )
                        .retrieve()
                        .body(
                                GoogleDriveFileResponse.class
                        );

        return requireFileResponse(
                response,
                "Google Drive returned an empty folder creation response"
        );
    }

    @Override
    public GoogleDriveFileResponse rename(
            Long connectionId,
            Long userId,
            String googleFileId,
            String name
    ) {

        validateIdentifiers(
                connectionId,
                userId,
                googleFileId
        );

        String normalizedName =
                normalizeName(
                        name
                );

        String accessToken =
                googleTokenService
                        .getValidAccessToken(
                                connectionId,
                                userId
                        );

        GoogleDriveFileMutationRequest request =
                new GoogleDriveFileMutationRequest(
                        normalizedName,
                        null,
                        null,
                        null
                );

        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                GOOGLE_DRIVE_FILES_URL
                                        + "/"
                                        + googleFileId
                        )
                        .queryParam(
                                "supportsAllDrives",
                                true
                        )
                        .queryParam(
                                "fields",
                                GoogleDriveFieldMasks.FILE_RESOURCE
                        )
                        .build()
                        .encode()
                        .toUri();

        GoogleDriveFileResponse response =
                restClient
                        .patch()
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .body(
                                request
                        )
                        .retrieve()
                        .body(
                                GoogleDriveFileResponse.class
                        );

        return requireFileResponse(
                response,
                "Google Drive returned an empty rename response"
        );
    }

    private GoogleDriveFileResponse requireFileResponse(
            GoogleDriveFileResponse response,
            String message
    ) {

        if (response == null
                || response.id() == null
                || response.id().isBlank()) {

            throw new IllegalStateException(
                    message
            );
        }

        return response;
    }

    private String normalizeName(
            String name
    ) {

        if (name == null) {

            throw new IllegalArgumentException(
                    "name is required"
            );
        }

        String normalized =
                name.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    "name must not be blank"
            );
        }

        if (normalized.length() > 255) {

            throw new IllegalArgumentException(
                    "name must not exceed 255 characters"
            );
        }

        return normalized;
    }

    private void validateIdentifiers(
            Long connectionId,
            Long userId,
            String googleFileId
    ) {

        if (connectionId == null) {

            throw new IllegalArgumentException(
                    "connectionId is required"
            );
        }

        if (userId == null) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }

        if (googleFileId == null
                || googleFileId.isBlank()) {

            throw new IllegalArgumentException(
                    "googleFileId is required"
            );
        }
    }
}