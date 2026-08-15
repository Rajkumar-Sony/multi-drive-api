package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFilesResponse;
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

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 1000;

    private final GoogleTokenService googleTokenService;
    private final RestClient restClient;

    public GoogleDriveServiceImpl(
            GoogleTokenService googleTokenService
    ) {
        this.googleTokenService = googleTokenService;
        this.restClient = RestClient.create();
    }

    @Override
    public GoogleDriveFilesResponse getFiles(
            Long connectionId,
            Long userId,
            Integer pageSize,
            String pageToken
    ) {

        String accessToken =
                googleTokenService.getValidAccessToken(
                        connectionId,
                        userId
                );

        int requestedPageSize =
                normalizePageSize(pageSize);

        UriComponentsBuilder uriBuilder =
                UriComponentsBuilder
                        .fromUriString(GOOGLE_DRIVE_FILES_URL)

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
                                        + "driveId"
                                        + ")"
                        );

        if (pageToken != null && !pageToken.isBlank()) {

            uriBuilder.queryParam(
                    "pageToken",
                    pageToken
            );
        }

        // Pass a URI to RestClient so the already-encoded query is not encoded
        // again.
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
                    "Google Drive returned an empty response"
            );
        }

        return response;
    }

    private int normalizePageSize(
            Integer pageSize
    ) {

        if (pageSize == null) {
            return DEFAULT_PAGE_SIZE;
        }

        if (pageSize < 1
                || pageSize > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "pageSize must be between 1 and 1000"
            );
        }

        return pageSize;
    }
}
