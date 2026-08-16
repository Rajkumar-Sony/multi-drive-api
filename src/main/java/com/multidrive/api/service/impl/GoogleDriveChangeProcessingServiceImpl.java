package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.dto.GoogleDriveChangesResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.repository.GoogleDriveChangeTrackerRepository;
import com.multidrive.api.service.GoogleDriveChangeProcessingService;
import com.multidrive.api.service.GoogleDriveItemIndexService;
import com.multidrive.api.service.GoogleTokenService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleDriveChangeProcessingServiceImpl
        implements GoogleDriveChangeProcessingService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GoogleDriveChangeProcessingServiceImpl.class
            );

    private static final String GOOGLE_DRIVE_CHANGES_URL =
            "https://www.googleapis.com/drive/v3/changes";

    private static final String TRACKER_STATUS_ACTIVE =
            "ACTIVE";

    private static final int CHANGE_PAGE_SIZE =
            1000;

    private final GoogleDriveChangeTrackerRepository
            googleDriveChangeTrackerRepository;

    private final GoogleTokenService
            googleTokenService;

    private final GoogleDriveItemIndexService
            googleDriveItemIndexService;

    private final RestClient
            restClient;

    public GoogleDriveChangeProcessingServiceImpl(
            GoogleDriveChangeTrackerRepository
                    googleDriveChangeTrackerRepository,

            GoogleTokenService
                    googleTokenService,

            GoogleDriveItemIndexService
                    googleDriveItemIndexService
    ) {

        this.googleDriveChangeTrackerRepository =
                googleDriveChangeTrackerRepository;

        this.googleTokenService =
                googleTokenService;

        this.googleDriveItemIndexService =
                googleDriveItemIndexService;

        this.restClient =
                RestClient.create();
    }

    @Override
    @Transactional
    public List<GoogleDriveChangeResponse> processChanges(
            Long trackerId
    ) {

        if (trackerId == null) {

            throw new IllegalArgumentException(
                    "trackerId is required"
            );
        }

        GoogleDriveChangeTracker tracker =
                googleDriveChangeTrackerRepository
                        .findById(
                                trackerId
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Google Drive change tracker not found"
                                )
                        );

        if (!TRACKER_STATUS_ACTIVE.equals(
                tracker.getStatus()
        )) {

            throw new IllegalStateException(
                    "Google Drive change tracker is not active"
            );
        }

        if (tracker.getPageToken() == null
                || tracker.getPageToken().isBlank()) {

            throw new IllegalStateException(
                    "Google Drive change tracker page token is missing"
            );
        }

        GoogleDriveConnection connection =
                tracker.getConnection();

        if (connection == null
                || connection.getId() == null) {

            throw new IllegalStateException(
                    "Google Drive connection is missing"
            );
        }

        if (connection.getUser() == null
                || connection.getUser().getId() == null) {

            throw new IllegalStateException(
                    "Google Drive connection user is missing"
            );
        }

        Long connectionId =
                connection.getId();

        Long userId =
                connection
                        .getUser()
                        .getId();

        String accessToken =
                googleTokenService
                        .getValidAccessToken(
                                connectionId,
                                userId
                        );

        String currentPageToken =
                tracker.getPageToken();

        String newStartPageToken =
                null;

        List<GoogleDriveChangeResponse> allChanges =
                new ArrayList<>();

        boolean hasMorePages =
                true;

        while (hasMorePages) {

            GoogleDriveChangesResponse response =
                    fetchChanges(
                            tracker,
                            accessToken,
                            currentPageToken
                    );

            if (response.changes() != null) {

                allChanges.addAll(
                        response.changes()
                );
            }

            /*
             * Google returns nextPageToken while
             * additional change pages remain.
             */
            if (response.nextPageToken() != null
                    && !response.nextPageToken().isBlank()) {

                currentPageToken =
                        response.nextPageToken();

                continue;
            }

            /*
             * newStartPageToken is returned only once
             * the end of the current change list has
             * been reached.
             */
            if (response.newStartPageToken() != null
                    && !response.newStartPageToken().isBlank()) {

                newStartPageToken =
                        response.newStartPageToken();
            }

            hasMorePages =
                    false;
        }

        if (newStartPageToken == null
                || newStartPageToken.isBlank()) {

            throw new IllegalStateException(
                    "Google Drive did not return a new start page token"
            );
        }

        /*
         * Update our unified local file index BEFORE
         * moving the synchronization token forward.
         *
         * If indexing fails, the transaction rolls back
         * and this page token is not consumed.
         */
        int indexedChangeCount =
                googleDriveItemIndexService
                        .applyChanges(
                                connection,
                                tracker.getTrackerType(),
                                tracker.getDriveId(),
                                allChanges
                        );

        /*
         * Only after the local index is successfully
         * updated do we advance the change cursor.
         */
        tracker.setPageToken(
                newStartPageToken
        );

        tracker.setLastSyncedAt(
                LocalDateTime.now(
                        ZoneOffset.UTC
                )
        );

        googleDriveChangeTrackerRepository
                .save(
                        tracker
                );

        logChanges(
                tracker,
                allChanges
        );

        LOGGER.info(
                "Google Drive changes processed successfully. "
                        + "trackerId={}, connectionId={}, trackerType={}, "
                        + "receivedChanges={}, indexedChanges={}",
                tracker.getId(),
                connectionId,
                tracker.getTrackerType(),
                allChanges.size(),
                indexedChangeCount
        );

        return List.copyOf(
                allChanges
        );
    }

    private GoogleDriveChangesResponse fetchChanges(
            GoogleDriveChangeTracker tracker,
            String accessToken,
            String pageToken
    ) {

        UriComponentsBuilder uriBuilder =
                UriComponentsBuilder
                        .fromUriString(
                                GOOGLE_DRIVE_CHANGES_URL
                        )
                        .queryParam(
                                "pageToken",
                                pageToken
                        )
                        .queryParam(
                                "pageSize",
                                CHANGE_PAGE_SIZE
                        )
                        .queryParam(
                                "includeRemoved",
                                true
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
                                "spaces",
                                "drive"
                        )
                        .queryParam(
                                "fields",
                                "nextPageToken,"
                                        + "newStartPageToken,"
                                        + "changes("
                                        + "removed,"
                                        + "fileId,"
                                        + "time,"
                                        + "driveId,"
                                        + "changeType,"
                                        + "file("
                                        + "id,"
                                        + "name,"
                                        + "mimeType,"
                                        + "createdTime,"
                                        + "modifiedTime,"
                                        + "parents,"
                                        + "webViewLink,"
                                        + "thumbnailLink,"
                                        + "iconLink,"
                                        + "size,"
                                        + "driveId,"
                                        + "trashed"
                                        + "),"
                                        + "drive("
                                        + "id,"
                                        + "name"
                                        + ")"
                                        + ")"
                        );

        if (tracker.getTrackerType()
                == GoogleDriveTrackerType.SHARED_DRIVE) {

            if (tracker.getDriveId() == null
                    || tracker.getDriveId().isBlank()) {

                throw new IllegalStateException(
                        "Shared Drive tracker driveId is missing"
                );
            }

            uriBuilder.queryParam(
                    "driveId",
                    tracker.getDriveId()
            );
        }

        URI uri =
                uriBuilder
                        .build()
                        .encode()
                        .toUri();

        GoogleDriveChangesResponse response =
                restClient
                        .get()
                        .uri(uri)
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + accessToken
                        )
                        .retrieve()
                        .body(
                                GoogleDriveChangesResponse.class
                        );

        if (response == null) {

            throw new IllegalStateException(
                    "Google Drive returned an empty changes response"
            );
        }

        return response;
    }

    private void logChanges(
            GoogleDriveChangeTracker tracker,
            List<GoogleDriveChangeResponse> changes
    ) {

        for (GoogleDriveChangeResponse change : changes) {

            if (change == null) {
                continue;
            }

            if (Boolean.TRUE.equals(
                    change.removed()
            )) {

                LOGGER.info(
                        "Google Drive file removed from local index. "
                                + "trackerId={}, fileId={}, driveId={}",
                        tracker.getId(),
                        change.fileId(),
                        change.driveId()
                );

                continue;
            }

            if ("file".equalsIgnoreCase(
                    change.changeType()
            )) {

                if (change.file() != null) {

                    LOGGER.info(
                            "Google Drive file indexed. "
                                    + "trackerId={}, fileId={}, "
                                    + "name={}, mimeType={}, trashed={}",
                            tracker.getId(),
                            change.fileId(),
                            change.file().name(),
                            change.file().mimeType(),
                            change.file().trashed()
                    );

                } else {

                    LOGGER.info(
                            "Google Drive file changed. "
                                    + "trackerId={}, fileId={}",
                            tracker.getId(),
                            change.fileId()
                    );
                }

                continue;
            }

            if ("drive".equalsIgnoreCase(
                    change.changeType()
            )) {

                LOGGER.info(
                        "Google Shared Drive changed. "
                                + "trackerId={}, driveId={}, driveName={}",
                        tracker.getId(),
                        change.driveId(),
                        change.drive() != null
                                ? change.drive().name()
                                : null
                );
            }
        }
    }
}