package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleStartPageTokenResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.repository.GoogleDriveChangeTrackerRepository;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.GoogleDriveChangeService;
import com.multidrive.api.service.GoogleTokenService;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

@Service
public class GoogleDriveChangeServiceImpl
        implements GoogleDriveChangeService {

    private static final String GOOGLE_START_PAGE_TOKEN_URL =
            "https://www.googleapis.com/drive/v3/changes/startPageToken";

    private static final String TRACKER_STATUS_ACTIVE =
            "ACTIVE";

    private final GoogleDriveChangeTrackerRepository
            googleDriveChangeTrackerRepository;

    private final GoogleDriveConnectionRepository
            googleDriveConnectionRepository;

    private final GoogleTokenService
            googleTokenService;

    private final RestClient
            restClient;

    public GoogleDriveChangeServiceImpl(
            GoogleDriveChangeTrackerRepository
                    googleDriveChangeTrackerRepository,

            GoogleDriveConnectionRepository
                    googleDriveConnectionRepository,

            GoogleTokenService
                    googleTokenService
    ) {

        this.googleDriveChangeTrackerRepository =
                googleDriveChangeTrackerRepository;

        this.googleDriveConnectionRepository =
                googleDriveConnectionRepository;

        this.googleTokenService =
                googleTokenService;

        this.restClient =
                RestClient.create();
    }

    @Override
    public GoogleDriveChangeTracker initializeUserTracker(
            Long connectionId,
            Long userId
    ) {

        validateIds(
                connectionId,
                userId
        );

        /*
         * Make sure this Google Drive connection belongs
         * to the currently authenticated application user.
         */
        GoogleDriveConnection connection =
                googleDriveConnectionRepository
                        .findByIdAndUserId(
                                connectionId,
                                userId
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Google Drive connection not found"
                                )
                        );

        /*
         * A USER tracker may already exist.
         *
         * If it already has a page token, we must keep it.
         *
         * We should NOT request a new token every time,
         * because doing that could skip changes that have
         * not been processed yet.
         */
        Optional<GoogleDriveChangeTracker> existingTracker =
                googleDriveChangeTrackerRepository
                        .findByConnection_IdAndTrackerTypeAndDriveIdIsNull(
                                connectionId,
                                GoogleDriveTrackerType.USER
                        );

        if (existingTracker.isPresent()
                && hasPageToken(existingTracker.get())) {

            return existingTracker.get();
        }

        /*
         * Get a valid Google access token.
         *
         * GoogleTokenService will automatically refresh
         * the access token if necessary.
         */
        String accessToken =
                googleTokenService
                        .getValidAccessToken(
                                connectionId,
                                userId
                        );

        /*
         * supportsAllDrives=true tells Google that our
         * application supports My Drive and Shared Drives.
         */
        URI uri =
                UriComponentsBuilder
                        .fromUriString(
                                GOOGLE_START_PAGE_TOKEN_URL
                        )
                        .queryParam(
                                "supportsAllDrives",
                                true
                        )
                        .build()
                        .encode()
                        .toUri();

        GoogleStartPageTokenResponse response =
                restClient
                        .get()
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .retrieve()
                        .body(
                                GoogleStartPageTokenResponse.class
                        );

        if (response == null) {
            throw new IllegalStateException(
                    "Google Drive returned an empty start page token response"
            );
        }

        if (response.startPageToken() == null
                || response.startPageToken().isBlank()) {

            throw new IllegalStateException(
                    "Google Drive did not return a start page token"
            );
        }

        GoogleDriveChangeTracker tracker =
                existingTracker.orElseGet(
                        GoogleDriveChangeTracker::new
                );

        tracker.setConnection(
                connection
        );

        tracker.setTrackerType(
                GoogleDriveTrackerType.USER
        );

        tracker.setDriveId(
                null
        );

        tracker.setPageToken(
                response.startPageToken()
        );

        tracker.setStatus(
                TRACKER_STATUS_ACTIVE
        );

        return googleDriveChangeTrackerRepository
                .save(
                        tracker
                );
    }

    private boolean hasPageToken(
            GoogleDriveChangeTracker tracker
    ) {

        return tracker.getPageToken() != null
                && !tracker.getPageToken().isBlank();
    }

    private void validateIds(
            Long connectionId,
            Long userId
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
    }
}