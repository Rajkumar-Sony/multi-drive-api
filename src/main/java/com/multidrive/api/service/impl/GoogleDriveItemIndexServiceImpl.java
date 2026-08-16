package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.mapper.GoogleDriveCapabilityMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.GoogleDriveItemIndexService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GoogleDriveItemIndexServiceImpl
        implements GoogleDriveItemIndexService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GoogleDriveItemIndexServiceImpl.class
            );

    private static final String GOOGLE_FOLDER_MIME_TYPE =
            "application/vnd.google-apps.folder";

    private static final Set<String> DOCUMENT_MIME_TYPES =
            Set.of(
                    "application/pdf",

                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",

                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",

                    "application/rtf",

                    "application/vnd.oasis.opendocument.text",
                    "application/vnd.oasis.opendocument.spreadsheet",
                    "application/vnd.oasis.opendocument.presentation",

                    "application/json",
                    "application/xml",
                    "text/xml",

                    "application/vnd.google-apps.document",
                    "application/vnd.google-apps.spreadsheet",
                    "application/vnd.google-apps.presentation",
                    "application/vnd.google-apps.drawing",
                    "application/vnd.google-apps.form",
                    "application/vnd.google-apps.script",
                    "application/vnd.google-apps.site",
                    "application/vnd.google-apps.jam",
                    "application/vnd.google-apps.map"
            );

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final GoogleDriveSourceRepository
            googleDriveSourceRepository;

    private final GoogleDriveCapabilityMapper
            googleDriveCapabilityMapper;

    public GoogleDriveItemIndexServiceImpl(
            GoogleDriveItemRepository
                    googleDriveItemRepository,

            GoogleDriveSourceRepository
                    googleDriveSourceRepository,

            GoogleDriveCapabilityMapper
                    googleDriveCapabilityMapper
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.googleDriveSourceRepository =
                googleDriveSourceRepository;

        this.googleDriveCapabilityMapper =
                googleDriveCapabilityMapper;
    }

    @Override
    @Transactional
    public int upsertItems(
            GoogleDriveConnection connection,
            List<GoogleDriveFileResponse> files,
            GoogleDriveItemSourceType requestedSourceType,
            String requestedDriveId,
            String syncRunId
    ) {

        validateConnection(
                connection
        );

        if (requestedSourceType == null) {

            throw new IllegalArgumentException(
                    "sourceType is required"
            );
        }

        if (syncRunId == null
                || syncRunId.isBlank()) {

            throw new IllegalArgumentException(
                    "syncRunId is required"
            );
        }

        if (files == null
                || files.isEmpty()) {

            return 0;
        }

        SourceLookup sourceLookup =
                loadSourceLookup(
                        connection.getId()
                );

        Set<String> googleFileIds =
                files
                        .stream()
                        .filter(
                                file ->
                                        file != null
                                                && hasText(
                                                file.id()
                                        )
                        )
                        .map(
                                GoogleDriveFileResponse::id
                        )
                        .collect(
                                Collectors.toCollection(
                                        HashSet::new
                                )
                        );

        if (googleFileIds.isEmpty()) {
            return 0;
        }

        Map<String, GoogleDriveItem> existingItems =
                googleDriveItemRepository
                        .findAllByConnection_IdAndGoogleFileIdIn(
                                connection.getId(),
                                googleFileIds
                        )
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        GoogleDriveItem::getGoogleFileId,
                                        Function.identity(),
                                        (first, second) -> first
                                )
                        );

        List<GoogleDriveItem> itemsToSave =
                new ArrayList<>();

        for (GoogleDriveFileResponse file : files) {

            if (file == null
                    || !hasText(file.id())) {

                continue;
            }

            GoogleDriveItemSourceType effectiveSourceType =
                    resolveInitialSyncSourceType(
                            requestedSourceType,
                            file
                    );

            String effectiveDriveId =
                    resolveInitialSyncDriveId(
                            requestedSourceType,
                            requestedDriveId,
                            file
                    );

            GoogleDriveSource source =
                    resolveSource(
                            sourceLookup,
                            effectiveSourceType,
                            effectiveDriveId
                    );

            GoogleDriveItem item =
                    existingItems.getOrDefault(
                            file.id(),
                            new GoogleDriveItem()
                    );

            populateItem(
                    item,
                    connection,
                    source,
                    file,
                    effectiveSourceType,
                    effectiveDriveId
            );

            item.setSyncRunId(
                    syncRunId
            );

            itemsToSave.add(
                    item
            );
        }

        googleDriveItemRepository
                .saveAll(
                        itemsToSave
                );

        return itemsToSave.size();
    }

    @Override
    @Transactional
    public int applyChanges(
            GoogleDriveConnection connection,
            GoogleDriveTrackerType trackerType,
            String trackerDriveId,
            List<GoogleDriveChangeResponse> changes
    ) {

        validateConnection(
                connection
        );

        if (trackerType == null) {

            throw new IllegalArgumentException(
                    "trackerType is required"
            );
        }

        if (trackerType
                == GoogleDriveTrackerType.SHARED_DRIVE
                && !hasText(trackerDriveId)) {

            throw new IllegalArgumentException(
                    "trackerDriveId is required for Shared Drive changes"
            );
        }

        if (changes == null
                || changes.isEmpty()) {

            return 0;
        }

        SourceLookup sourceLookup =
                loadSourceLookup(
                        connection.getId()
                );

        Set<String> fileIds =
                changes
                        .stream()
                        .filter(
                                change ->
                                        change != null
                                                && isFileChange(
                                                change
                                        )
                        )
                        .map(
                                this::resolveFileId
                        )
                        .filter(
                                this::hasText
                        )
                        .collect(
                                Collectors.toSet()
                        );

        Map<String, GoogleDriveItem> existingItems =
                new HashMap<>();

        if (!fileIds.isEmpty()) {

            existingItems.putAll(
                    googleDriveItemRepository
                            .findAllByConnection_IdAndGoogleFileIdIn(
                                    connection.getId(),
                                    fileIds
                            )
                            .stream()
                            .collect(
                                    Collectors.toMap(
                                            GoogleDriveItem::getGoogleFileId,
                                            Function.identity(),
                                            (first, second) -> first
                                    )
                            )
            );
        }

        Map<String, GoogleDriveItem> itemsToSave =
                new HashMap<>();

        Set<String> itemsToDelete =
                new HashSet<>();

        int processedChangeCount =
                0;

        for (GoogleDriveChangeResponse change : changes) {

            if (change == null
                    || !isFileChange(change)) {

                continue;
            }

            String fileId =
                    resolveFileId(
                            change
                    );

            if (!hasText(fileId)) {

                LOGGER.warn(
                        "Ignoring Google Drive file change "
                                + "because fileId is missing"
                );

                continue;
            }

            if (Boolean.TRUE.equals(
                    change.removed()
            )) {

                itemsToSave.remove(
                        fileId
                );

                itemsToDelete.add(
                        fileId
                );

                processedChangeCount++;

                continue;
            }

            GoogleDriveFileResponse file =
                    change.file();

            if (file == null) {

                LOGGER.warn(
                        "Ignoring Google Drive change because "
                                + "updated file data is missing. "
                                + "fileId={}",
                        fileId
                );

                continue;
            }

            GoogleDriveItem item =
                    itemsToSave.getOrDefault(
                            fileId,
                            existingItems.getOrDefault(
                                    fileId,
                                    new GoogleDriveItem()
                            )
                    );

            GoogleDriveItemSourceType sourceType =
                    resolveChangeSourceType(
                            trackerType,
                            file
                    );

            String driveId =
                    resolveChangeDriveId(
                            trackerType,
                            trackerDriveId,
                            file
                    );

            GoogleDriveSource source =
                    resolveSource(
                            sourceLookup,
                            sourceType,
                            driveId
                    );

            String existingSyncRunId =
                    item.getSyncRunId();

            populateItem(
                    item,
                    connection,
                    source,
                    file,
                    sourceType,
                    driveId
            );

            item.setSyncRunId(
                    existingSyncRunId
            );

            itemsToDelete.remove(
                    fileId
            );

            itemsToSave.put(
                    fileId,
                    item
            );

            processedChangeCount++;
        }

        for (String fileId : itemsToDelete) {

            googleDriveItemRepository
                    .deleteByConnection_IdAndGoogleFileId(
                            connection.getId(),
                            fileId
                    );
        }

        if (!itemsToSave.isEmpty()) {

            googleDriveItemRepository
                    .saveAll(
                            itemsToSave.values()
                    );
        }

        LOGGER.info(
                "Applied Google Drive changes to local index. "
                        + "connectionId={}, trackerType={}, "
                        + "processedChanges={}, upsertedItems={}, "
                        + "deletedItems={}",
                connection.getId(),
                trackerType,
                processedChangeCount,
                itemsToSave.size(),
                itemsToDelete.size()
        );

        return processedChangeCount;
    }

    @Override
    @Transactional
    public int deleteStaleItems(
            Long connectionId,
            String syncRunId
    ) {

        if (connectionId == null) {

            throw new IllegalArgumentException(
                    "connectionId is required"
            );
        }

        if (!hasText(syncRunId)) {

            throw new IllegalArgumentException(
                    "syncRunId is required"
            );
        }

        return googleDriveItemRepository
                .deleteStaleItems(
                        connectionId,
                        syncRunId
                );
    }

    private void populateItem(
            GoogleDriveItem item,
            GoogleDriveConnection connection,
            GoogleDriveSource source,
            GoogleDriveFileResponse file,
            GoogleDriveItemSourceType sourceType,
            String driveId
    ) {

        item.setConnection(
                connection
        );

        item.setSource(
                source
        );

        item.setGoogleFileId(
                file.id()
        );

        item.setName(
                normalizeName(
                        file.name()
                )
        );

        item.setMimeType(
                normalizeMimeType(
                        file.mimeType()
                )
        );

        item.setCategory(
                resolveCategory(
                        file.mimeType()
                )
        );

        item.setSourceType(
                sourceType
        );

        item.setDriveId(
                sourceType
                        == GoogleDriveItemSourceType.SHARED_DRIVE
                        ? driveId
                        : null
        );

        item.setParentId(
                extractParentId(
                        file.parents()
                )
        );

        item.setWebViewLink(
                file.webViewLink()
        );

        item.setThumbnailLink(
                file.thumbnailLink()
        );

        item.setIconLink(
                file.iconLink()
        );

        item.setSizeBytes(
                parseSize(
                        file.size()
                )
        );

        item.setGoogleCreatedTime(
                parseInstant(
                        file.createdTime()
                )
        );

        item.setGoogleModifiedTime(
                parseInstant(
                        file.modifiedTime()
                )
        );

        item.setTrashed(
                Boolean.TRUE.equals(
                        file.trashed()
                )
        );

        item.setExplicitlyTrashed(
                file.explicitlyTrashed()
        );

        item.setCapabilities(
                googleDriveCapabilityMapper
                        .toItemCapabilities(
                                file.capabilities()
                        )
        );
    }

    private SourceLookup loadSourceLookup(
            Long connectionId
    ) {

        List<GoogleDriveSource> sources =
                googleDriveSourceRepository
                        .findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(
                                connectionId,
                                GoogleDriveSourceStatus.ACTIVE
                        );

        GoogleDriveSource myDrive =
                null;

        Map<String, GoogleDriveSource> sharedDrives =
                new HashMap<>();

        for (GoogleDriveSource source : sources) {

            if (source.getSourceType()
                    == GoogleDriveSourceType.MY_DRIVE) {

                myDrive =
                        source;

                continue;
            }

            if (source.getSourceType()
                    == GoogleDriveSourceType.SHARED_DRIVE
                    && hasText(source.getGoogleDriveId())) {

                sharedDrives.put(
                        source.getGoogleDriveId(),
                        source
                );
            }
        }

        return new SourceLookup(
                myDrive,
                sharedDrives
        );
    }

    private GoogleDriveSource resolveSource(
            SourceLookup lookup,
            GoogleDriveItemSourceType sourceType,
            String driveId
    ) {

        if (sourceType
                == GoogleDriveItemSourceType.MY_DRIVE) {

            if (lookup.myDrive() == null) {

                throw new IllegalStateException(
                        "Active My Drive source was not found"
                );
            }

            return lookup.myDrive();
        }

        if (!hasText(driveId)) {

            throw new IllegalStateException(
                    "Shared Drive ID is missing"
            );
        }

        GoogleDriveSource source =
                lookup.sharedDrives()
                        .get(
                                driveId
                        );

        if (source == null) {

            throw new IllegalStateException(
                    "Active Shared Drive source was not found: "
                            + driveId
            );
        }

        return source;
    }

    private GoogleDriveItemSourceType
    resolveInitialSyncSourceType(
            GoogleDriveItemSourceType requestedSourceType,
            GoogleDriveFileResponse file
    ) {

        if (requestedSourceType
                == GoogleDriveItemSourceType.SHARED_DRIVE) {

            return GoogleDriveItemSourceType.SHARED_DRIVE;
        }

        if (hasText(file.driveId())) {

            return GoogleDriveItemSourceType.SHARED_DRIVE;
        }

        return GoogleDriveItemSourceType.MY_DRIVE;
    }

    private String resolveInitialSyncDriveId(
            GoogleDriveItemSourceType requestedSourceType,
            String requestedDriveId,
            GoogleDriveFileResponse file
    ) {

        if (requestedSourceType
                == GoogleDriveItemSourceType.SHARED_DRIVE) {

            return requestedDriveId;
        }

        if (hasText(file.driveId())) {

            return file.driveId();
        }

        return null;
    }

    private GoogleDriveItemSourceType resolveChangeSourceType(
            GoogleDriveTrackerType trackerType,
            GoogleDriveFileResponse file
    ) {

        if (trackerType
                == GoogleDriveTrackerType.SHARED_DRIVE) {

            return GoogleDriveItemSourceType.SHARED_DRIVE;
        }

        if (hasText(file.driveId())) {

            return GoogleDriveItemSourceType.SHARED_DRIVE;
        }

        return GoogleDriveItemSourceType.MY_DRIVE;
    }

    private String resolveChangeDriveId(
            GoogleDriveTrackerType trackerType,
            String trackerDriveId,
            GoogleDriveFileResponse file
    ) {

        if (hasText(file.driveId())) {

            return file.driveId();
        }

        if (trackerType
                == GoogleDriveTrackerType.SHARED_DRIVE) {

            return trackerDriveId;
        }

        return null;
    }

    private boolean isFileChange(
            GoogleDriveChangeResponse change
    ) {

        return "file".equalsIgnoreCase(
                change.changeType()
        );
    }

    private String resolveFileId(
            GoogleDriveChangeResponse change
    ) {

        if (hasText(change.fileId())) {

            return change.fileId();
        }

        if (change.file() != null
                && hasText(change.file().id())) {

            return change.file().id();
        }

        return null;
    }

    private GoogleDriveItemCategory resolveCategory(
            String mimeType
    ) {

        if (!hasText(mimeType)) {

            return GoogleDriveItemCategory.OTHER;
        }

        if (GOOGLE_FOLDER_MIME_TYPE.equals(
                mimeType
        )) {

            return GoogleDriveItemCategory.FOLDER;
        }

        if (mimeType.startsWith(
                "image/"
        )) {

            return GoogleDriveItemCategory.IMAGE;
        }

        if (mimeType.startsWith(
                "video/"
        )) {

            return GoogleDriveItemCategory.VIDEO;
        }

        if (mimeType.startsWith(
                "text/"
        )) {

            return GoogleDriveItemCategory.DOCUMENT;
        }

        if (DOCUMENT_MIME_TYPES.contains(
                mimeType
        )) {

            return GoogleDriveItemCategory.DOCUMENT;
        }

        return GoogleDriveItemCategory.OTHER;
    }

    private String extractParentId(
            List<String> parents
    ) {

        if (parents == null
                || parents.isEmpty()) {

            return null;
        }

        return parents.getFirst();
    }

    private Long parseSize(
            String size
    ) {

        if (!hasText(size)) {
            return null;
        }

        try {

            return Long.parseLong(
                    size
            );

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    private Instant parseInstant(
            String value
    ) {

        if (!hasText(value)) {
            return null;
        }

        try {

            return Instant.parse(
                    value
            );

        } catch (DateTimeParseException exception) {

            return null;
        }
    }

    private String normalizeName(
            String name
    ) {

        if (!hasText(name)) {

            return "Untitled";
        }

        return name;
    }

    private String normalizeMimeType(
            String mimeType
    ) {

        if (!hasText(mimeType)) {

            return "application/octet-stream";
        }

        return mimeType;
    }

    private void validateConnection(
            GoogleDriveConnection connection
    ) {

        if (connection == null
                || connection.getId() == null) {

            throw new IllegalArgumentException(
                    "Google Drive connection is required"
            );
        }
    }

    private boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }

    private record SourceLookup(

            GoogleDriveSource myDrive,

            Map<String, GoogleDriveSource> sharedDrives
    ) {
    }
}
