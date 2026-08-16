package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.dto.GoogleDriveInitialSyncResponse;
import com.multidrive.api.dto.GoogleSharedDriveResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.GoogleDriveInitialSyncService;
import com.multidrive.api.service.GoogleDriveItemIndexService;
import com.multidrive.api.service.GoogleDriveService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GoogleDriveInitialSyncServiceImpl
        implements GoogleDriveInitialSyncService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GoogleDriveInitialSyncServiceImpl.class
            );

    private static final int FILE_PAGE_SIZE =
            1000;

    private static final int SHARED_DRIVE_PAGE_SIZE =
            100;

    private final GoogleDriveConnectionRepository
            googleDriveConnectionRepository;

    private final GoogleDriveService
            googleDriveService;

    private final GoogleDriveItemIndexService
            googleDriveItemIndexService;

    public GoogleDriveInitialSyncServiceImpl(
            GoogleDriveConnectionRepository
                    googleDriveConnectionRepository,

            GoogleDriveService
                    googleDriveService,

            GoogleDriveItemIndexService
                    googleDriveItemIndexService
    ) {

        this.googleDriveConnectionRepository =
                googleDriveConnectionRepository;

        this.googleDriveService =
                googleDriveService;

        this.googleDriveItemIndexService =
                googleDriveItemIndexService;
    }

    @Override
    public GoogleDriveInitialSyncResponse syncConnection(
            Long connectionId,
            Long userId
    ) {

        validateIds(
                connectionId,
                userId
        );

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

        String syncRunId =
                UUID
                        .randomUUID()
                        .toString();

        long myDriveItemCount =
                syncMyDrive(
                        connection,
                        userId,
                        syncRunId
                );

        SharedDriveSyncResult sharedDriveResult =
                syncSharedDrives(
                        connection,
                        userId,
                        syncRunId
                );

        /*
         * Only remove stale rows after every API page
         * from My Drive and every Shared Drive completed
         * successfully.
         */
        int staleItemCount =
                googleDriveItemIndexService
                        .deleteStaleItems(
                                connectionId,
                                syncRunId
                        );

        long totalItemCount =
                myDriveItemCount
                        + sharedDriveResult.itemCount();

        LOGGER.info(
                "Initial Google Drive synchronization completed. "
                        + "connectionId={}, myDriveItems={}, "
                        + "sharedDrives={}, sharedDriveItems={}, "
                        + "totalItems={}, staleItemsRemoved={}",
                connectionId,
                myDriveItemCount,
                sharedDriveResult.driveCount(),
                sharedDriveResult.itemCount(),
                totalItemCount,
                staleItemCount
        );

        return new GoogleDriveInitialSyncResponse(
                connectionId,
                myDriveItemCount,
                sharedDriveResult.driveCount(),
                sharedDriveResult.itemCount(),
                totalItemCount,
                staleItemCount
        );
    }

    private long syncMyDrive(
            GoogleDriveConnection connection,
            Long userId,
            String syncRunId
    ) {

        long itemCount =
                0;

        String pageToken =
                null;

        do {

            GoogleDriveFilesResponse response =
                    googleDriveService
                            .getMyDriveFiles(
                                    connection.getId(),
                                    userId,
                                    FILE_PAGE_SIZE,
                                    pageToken
                            );

            List<GoogleDriveFileResponse> files =
                    response.files() == null
                            ? List.of()
                            : response.files();

            itemCount +=
                    googleDriveItemIndexService
                            .upsertItems(
                                    connection,
                                    files,
                                    GoogleDriveItemSourceType.MY_DRIVE,
                                    null,
                                    syncRunId
                            );

            pageToken =
                    normalizePageToken(
                            response.nextPageToken()
                    );

        } while (pageToken != null);

        return itemCount;
    }

    private SharedDriveSyncResult syncSharedDrives(
            GoogleDriveConnection connection,
            Long userId,
            String syncRunId
    ) {

        long sharedDriveCount =
                0;

        long sharedDriveItemCount =
                0;

        String sharedDrivePageToken =
                null;

        do {

            GoogleSharedDrivesResponse response =
                    googleDriveService
                            .getSharedDrives(
                                    connection.getId(),
                                    userId,
                                    SHARED_DRIVE_PAGE_SIZE,
                                    sharedDrivePageToken
                            );

            List<GoogleSharedDriveResponse> sharedDrives =
                    response.drives() == null
                            ? List.of()
                            : response.drives();

            for (GoogleSharedDriveResponse sharedDrive
                    : sharedDrives) {

                if (sharedDrive == null
                        || sharedDrive.id() == null
                        || sharedDrive.id().isBlank()) {

                    continue;
                }

                sharedDriveCount++;

                sharedDriveItemCount +=
                        syncSingleSharedDrive(
                                connection,
                                userId,
                                sharedDrive.id(),
                                syncRunId
                        );
            }

            sharedDrivePageToken =
                    normalizePageToken(
                            response.nextPageToken()
                    );

        } while (sharedDrivePageToken != null);

        return new SharedDriveSyncResult(
                sharedDriveCount,
                sharedDriveItemCount
        );
    }

    private long syncSingleSharedDrive(
            GoogleDriveConnection connection,
            Long userId,
            String driveId,
            String syncRunId
    ) {

        long itemCount =
                0;

        String pageToken =
                null;

        do {

            GoogleDriveFilesResponse response =
                    googleDriveService
                            .getSharedDriveFiles(
                                    connection.getId(),
                                    userId,
                                    driveId,
                                    FILE_PAGE_SIZE,
                                    pageToken
                            );

            List<GoogleDriveFileResponse> files =
                    response.files() == null
                            ? List.of()
                            : response.files();

            itemCount +=
                    googleDriveItemIndexService
                            .upsertItems(
                                    connection,
                                    files,
                                    GoogleDriveItemSourceType.SHARED_DRIVE,
                                    driveId,
                                    syncRunId
                            );

            pageToken =
                    normalizePageToken(
                            response.nextPageToken()
                    );

        } while (pageToken != null);

        return itemCount;
    }

    private String normalizePageToken(
            String pageToken
    ) {

        if (pageToken == null
                || pageToken.isBlank()) {

            return null;
        }

        return pageToken;
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

    private record SharedDriveSyncResult(
            long driveCount,
            long itemCount
    ) {
    }
}