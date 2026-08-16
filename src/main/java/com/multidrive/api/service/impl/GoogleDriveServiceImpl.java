package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.service.GoogleDriveService;
import com.multidrive.api.service.GoogleTokenService;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class GoogleDriveServiceImpl implements GoogleDriveService {

    private static final String GOOGLE_DRIVE_FILES_URL =
            "https://www.googleapis.com/drive/v3/files";

    private static final String GOOGLE_SHARED_DRIVES_URL =
            "https://www.googleapis.com/drive/v3/drives";

    private static final int DEFAULT_FILE_PAGE_SIZE =
            50;

    private static final int MAX_FILE_PAGE_SIZE =
            1000;

    private static final int DEFAULT_SHARED_DRIVE_PAGE_SIZE =
            50;

    private static final int MAX_SHARED_DRIVE_PAGE_SIZE =
            100;

    private final GoogleTokenService
            googleTokenService;

    private final RestClient
            restClient;

    public GoogleDriveServiceImpl(
            GoogleTokenService googleTokenService
    ) {

        this.googleTokenService =
                googleTokenService;

        this.restClient =
                RestClient.create();
    }

    @Override
    public GoogleDriveFilesResponse getFiles(
            Long connectionId,
            Long userId,
            Integer pageSize,
            String pageToken
    ) {

        String accessToken =
                googleTokenService
                        .getValidAccessToken(
                                connectionId,
                                userId
                        );

        int requestedPageSize =
                normalizeFilePageSize(
                        pageSize
                );

        UriComponentsBuilder uriBuilder =
                UriComponentsBuilder
                        .fromUriString(
                                GOOGLE_DRIVE_FILES_URL
                        )
                        .queryParam(
                                "pageSize",
                                requestedPageSize
                        )
                        .queryParam(
                                "q",
                                "trashed=false"
                        )
                        .queryParam(
                                "spaces",
                                "drive"
                        )
                        .queryParam(
                                "includeItemsFromAllDrives",
                                true
                        )
                        .queryParam(
                                "supportsAllDrives",
                                true
                        )
                        .queryParam(
                                "fields",
                                "nextPageToken,"
                                        + "incompleteSearch,"
                                        + "files("
                                        + "id,"
                                        + "name,"
                                        + "mimeType,"
                                        + "modifiedTime,"
                                        + "parents,"
                                        + "webViewLink,"
                                        + "driveId,"
                                        + "trashed"
                                        + ")"
                        );

        if (pageToken != null
                && !pageToken.isBlank()) {

            uriBuilder.queryParam(
                    "pageToken",
                    pageToken
            );
        }

        URI uri =
                uriBuilder
                        .build()
                        .encode()
                        .toUri();

        GoogleDriveFilesResponse response =
                restClient
                        .get()
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .retrieve()
                        .body(
                                GoogleDriveFilesResponse.class
                        );

        if (response == null) {

            throw new IllegalStateException(
                    "Google Drive returned an empty file response"
            );
        }

        return response;
    }

    @Override
    public GoogleSharedDrivesResponse getSharedDrives(
            Long connectionId,
            Long userId,
            Integer pageSize,
            String pageToken
    ) {

        String accessToken =
                googleTokenService
                        .getValidAccessToken(
                                connectionId,
                                userId
                        );

        int requestedPageSize =
                normalizeSharedDrivePageSize(
                        pageSize
                );

        UriComponentsBuilder uriBuilder =
                UriComponentsBuilder
                        .fromUriString(
                                GOOGLE_SHARED_DRIVES_URL
                        )
                        .queryParam(
                                "pageSize",
                                requestedPageSize
                        )
                        .queryParam(
                                "fields",
                                "nextPageToken,"
                                        + "drives("
                                        + "id,"
                                        + "name"
                                        + ")"
                        );

        if (pageToken != null
                && !pageToken.isBlank()) {

            uriBuilder.queryParam(
                    "pageToken",
                    pageToken
            );
        }

        URI uri =
                uriBuilder
                        .build()
                        .encode()
                        .toUri();

        GoogleSharedDrivesResponse response =
                restClient
                        .get()
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .retrieve()
                        .body(
                                GoogleSharedDrivesResponse.class
                        );

        if (response == null) {

            throw new IllegalStateException(
                    "Google Drive returned an empty Shared Drives response"
            );
        }

        return response;
    }

    private int normalizeFilePageSize(
            Integer pageSize
    ) {

        if (pageSize == null) {
            return DEFAULT_FILE_PAGE_SIZE;
        }

        if (pageSize < 1
                || pageSize > MAX_FILE_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "File pageSize must be between 1 and 1000"
            );
        }

        return pageSize;
    }

    private int normalizeSharedDrivePageSize(
            Integer pageSize
    ) {

        if (pageSize == null) {
            return DEFAULT_SHARED_DRIVE_PAGE_SIZE;
        }

        if (pageSize < 1
                || pageSize > MAX_SHARED_DRIVE_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Shared Drive pageSize must be between 1 and 100"
            );
        }

        return pageSize;
    }
}