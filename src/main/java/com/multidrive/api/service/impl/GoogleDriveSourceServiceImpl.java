package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveRootResponse;
import com.multidrive.api.dto.GoogleDriveSourceResponse;
import com.multidrive.api.dto.GoogleSharedDriveResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.GoogleDriveService;
import com.multidrive.api.service.GoogleDriveSourceService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoogleDriveSourceServiceImpl
        implements GoogleDriveSourceService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GoogleDriveSourceServiceImpl.class
            );

    private static final int SHARED_DRIVE_PAGE_SIZE =
            100;

    private final GoogleDriveConnectionRepository
            googleDriveConnectionRepository;

    private final GoogleDriveSourceRepository
            googleDriveSourceRepository;

    private final GoogleDriveService
            googleDriveService;

    private final TransactionTemplate
            transactionTemplate;

    public GoogleDriveSourceServiceImpl(
            GoogleDriveConnectionRepository
                    googleDriveConnectionRepository,

            GoogleDriveSourceRepository
                    googleDriveSourceRepository,

            GoogleDriveService
                    googleDriveService,

            PlatformTransactionManager
                    transactionManager
    ) {

        this.googleDriveConnectionRepository =
                googleDriveConnectionRepository;

        this.googleDriveSourceRepository =
                googleDriveSourceRepository;

        this.googleDriveService =
                googleDriveService;

        this.transactionTemplate =
                new TransactionTemplate(
                        transactionManager
                );
    }

    @Override
    public List<GoogleDriveSource> refreshSources(
            Long connectionId,
            Long userId
    ) {

        validateIds(
                connectionId,
                userId
        );

        /*
         * Validate ownership before making Google API
         * calls.
         */
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
         * Remote Google calls happen BEFORE the write
         * transaction.
         *
         * We don't want to hold a PostgreSQL transaction
         * open while waiting for network requests.
         */
        GoogleDriveRootResponse myDriveRoot =
                googleDriveService
                        .getMyDriveRoot(
                                connectionId,
                                userId
                        );

        List<GoogleSharedDriveResponse> sharedDrives =
                loadAllSharedDrives(
                        connectionId,
                        userId
                );

        String discoveryRunId =
                UUID
                        .randomUUID()
                        .toString();

        List<GoogleDriveSource> result =
                transactionTemplate.execute(
                        status -> persistDiscoveredSources(
                                connectionId,
                                userId,
                                myDriveRoot,
                                sharedDrives,
                                discoveryRunId
                        )
                );

        if (result == null) {

            throw new IllegalStateException(
                    "Google Drive source discovery transaction returned no result"
            );
        }

        LOGGER.info(
                "Google Drive sources refreshed. "
                        + "connectionId={}, sourceCount={}, "
                        + "sharedDriveCount={}",
                connectionId,
                result.size(),
                sharedDrives.size()
        );

        return List.copyOf(
                result
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoogleDriveSource> getActiveSources(
            Long connectionId
    ) {

        if (connectionId == null) {

            throw new IllegalArgumentException(
                    "connectionId is required"
            );
        }

        return googleDriveSourceRepository
                .findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(
                        connectionId,
                        GoogleDriveSourceStatus.ACTIVE
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoogleDriveSource> getActiveSharedDriveSources(
            Long connectionId
    ) {

        if (connectionId == null) {

            throw new IllegalArgumentException(
                    "connectionId is required"
            );
        }

        return googleDriveSourceRepository
                .findAllByConnection_IdAndSourceTypeAndStatusOrderByNameAsc(
                        connectionId,
                        GoogleDriveSourceType.SHARED_DRIVE,
                        GoogleDriveSourceStatus.ACTIVE
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoogleDriveSourceResponse>
    getActiveSourceResponses(
            Long userId
    ) {

        if (userId == null) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }

        return googleDriveSourceRepository
                .findSourceResponsesByUserIdAndStatus(
                        userId,
                        GoogleDriveSourceStatus.ACTIVE
                );
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoogleDriveSourceResponse>
    getActiveSourceResponses(
            Long userId,
            Long connectionId
    ) {

        validateIds(
                connectionId,
                userId
        );

        return googleDriveSourceRepository
                .findSourceResponsesByUserIdAndConnectionIdAndStatus(
                        userId,
                        connectionId,
                        GoogleDriveSourceStatus.ACTIVE
                );
    }

    private List<GoogleSharedDriveResponse>
    loadAllSharedDrives(
            Long connectionId,
            Long userId
    ) {

        Map<String, GoogleSharedDriveResponse>
                drivesById =
                new LinkedHashMap<>();

        String pageToken =
                null;

        do {

            GoogleSharedDrivesResponse response =
                    googleDriveService
                            .getSharedDrives(
                                    connectionId,
                                    userId,
                                    SHARED_DRIVE_PAGE_SIZE,
                                    pageToken
                            );

            if (response.drives() != null) {

                for (GoogleSharedDriveResponse drive
                        : response.drives()) {

                    if (drive == null
                            || drive.id() == null
                            || drive.id().isBlank()) {

                        continue;
                    }

                    drivesById.put(
                            drive.id(),
                            drive
                    );
                }
            }

            pageToken =
                    normalizePageToken(
                            response.nextPageToken()
                    );

        } while (pageToken != null);

        return new ArrayList<>(
                drivesById.values()
        );
    }

    private List<GoogleDriveSource>
    persistDiscoveredSources(
            Long connectionId,
            Long userId,
            GoogleDriveRootResponse myDriveRoot,
            List<GoogleSharedDriveResponse> sharedDrives,
            String discoveryRunId
    ) {

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

        List<GoogleDriveSource> existingSources =
                googleDriveSourceRepository
                        .findAllByConnection_Id(
                                connectionId
                        );

        GoogleDriveSource myDriveSource =
                null;

        Map<String, GoogleDriveSource>
                existingSharedDriveSources =
                new HashMap<>();

        for (GoogleDriveSource source
                : existingSources) {

            if (source.getSourceType()
                    == GoogleDriveSourceType.MY_DRIVE) {

                myDriveSource =
                        source;

                continue;
            }

            if (source.getSourceType()
                    == GoogleDriveSourceType.SHARED_DRIVE
                    && source.getGoogleDriveId() != null) {

                existingSharedDriveSources.put(
                        source.getGoogleDriveId(),
                        source
                );
            }
        }

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );

        List<GoogleDriveSource> sourcesToSave =
                new ArrayList<>();

        if (myDriveSource == null) {

            myDriveSource =
                    new GoogleDriveSource();
        }

        myDriveSource.setConnection(
                connection
        );

        myDriveSource.setSourceType(
                GoogleDriveSourceType.MY_DRIVE
        );

        myDriveSource.setGoogleDriveId(
                null
        );

        myDriveSource.setRootFolderId(
                myDriveRoot.id()
        );

        myDriveSource.setName(
                normalizeName(
                        myDriveRoot.name(),
                        "My Drive"
                )
        );

        myDriveSource.setStatus(
                GoogleDriveSourceStatus.ACTIVE
        );

        myDriveSource.setDiscoveryRunId(
                discoveryRunId
        );

        myDriveSource.setLastDiscoveredAt(
                now
        );

        sourcesToSave.add(
                myDriveSource
        );

        for (GoogleSharedDriveResponse drive
                : sharedDrives) {

            GoogleDriveSource source =
                    existingSharedDriveSources
                            .getOrDefault(
                                    drive.id(),
                                    new GoogleDriveSource()
                            );

            source.setConnection(
                    connection
            );

            source.setSourceType(
                    GoogleDriveSourceType.SHARED_DRIVE
            );

            source.setGoogleDriveId(
                    drive.id()
            );

            /*
             * Google documents that the Shared Drive ID
             * is also the ID of its top-level folder.
             */
            source.setRootFolderId(
                    drive.id()
            );

            source.setName(
                    normalizeName(
                            drive.name(),
                            source.getName()
                    )
            );

            source.setStatus(
                    GoogleDriveSourceStatus.ACTIVE
            );

            source.setDiscoveryRunId(
                    discoveryRunId
            );

            source.setLastDiscoveredAt(
                    now
            );

            sourcesToSave.add(
                    source
            );
        }

        googleDriveSourceRepository
                .saveAll(
                        sourcesToSave
                );

        /*
         * Flush before the bulk update so the current
         * discoveryRunId values are visible.
         */
        googleDriveSourceRepository
                .flush();

        int inactiveCount =
                googleDriveSourceRepository
                        .markUndiscoveredSharedDriveSourcesInactive(
                                connectionId,
                                GoogleDriveSourceType.SHARED_DRIVE,
                                GoogleDriveSourceStatus.ACTIVE,
                                GoogleDriveSourceStatus.INACTIVE,
                                discoveryRunId,
                                now
                        );

        if (inactiveCount > 0) {

            LOGGER.info(
                    "Marked stale Shared Drive sources inactive. "
                            + "connectionId={}, inactiveCount={}",
                    connectionId,
                    inactiveCount
            );
        }

        return googleDriveSourceRepository
                .findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(
                        connectionId,
                        GoogleDriveSourceStatus.ACTIVE
                );
    }

    private String normalizeName(
            String candidate,
            String fallback
    ) {

        if (candidate != null
                && !candidate.isBlank()) {

            return candidate;
        }

        if (fallback != null
                && !fallback.isBlank()) {

            return fallback;
        }

        return "Shared Drive";
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
}