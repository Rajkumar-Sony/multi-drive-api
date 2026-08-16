package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.GoogleDriveItemIndexService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GoogleDriveItemIndexServiceImpl
        implements GoogleDriveItemIndexService {

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

    public GoogleDriveItemIndexServiceImpl(
            GoogleDriveItemRepository
                    googleDriveItemRepository
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;
    }

    @Override
    @Transactional
    public int upsertItems(
            GoogleDriveConnection connection,
            List<GoogleDriveFileResponse> files,
            GoogleDriveItemSourceType sourceType,
            String driveId,
            String syncRunId
    ) {

        if (connection == null
                || connection.getId() == null) {

            throw new IllegalArgumentException(
                    "Google Drive connection is required"
            );
        }

        if (sourceType == null) {

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

        if (sourceType
                == GoogleDriveItemSourceType.SHARED_DRIVE
                && (
                    driveId == null
                    || driveId.isBlank()
                )) {

            throw new IllegalArgumentException(
                    "driveId is required for Shared Drive items"
            );
        }

        if (files == null
                || files.isEmpty()) {

            return 0;
        }

        Set<String> googleFileIds =
                files
                        .stream()
                        .filter(
                                file ->
                                        file != null
                                                && file.id() != null
                                                && !file.id().isBlank()
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
                    || file.id() == null
                    || file.id().isBlank()) {

                continue;
            }

            GoogleDriveItem item =
                    existingItems.getOrDefault(
                            file.id(),
                            new GoogleDriveItem()
                    );

            item.setConnection(
                    connection
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

            if (sourceType
                    == GoogleDriveItemSourceType.SHARED_DRIVE) {

                item.setDriveId(
                        driveId
                );

            } else {

                item.setDriveId(
                        null
                );
            }

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
    public int deleteStaleItems(
            Long connectionId,
            String syncRunId
    ) {

        if (connectionId == null) {

            throw new IllegalArgumentException(
                    "connectionId is required"
            );
        }

        if (syncRunId == null
                || syncRunId.isBlank()) {

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

    private GoogleDriveItemCategory resolveCategory(
            String mimeType
    ) {

        if (mimeType == null
                || mimeType.isBlank()) {

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

        if (size == null
                || size.isBlank()) {

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

        if (value == null
                || value.isBlank()) {

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

        if (name == null
                || name.isBlank()) {

            return "Untitled";
        }

        return name;
    }

    private String normalizeMimeType(
            String mimeType
    ) {

        if (mimeType == null
                || mimeType.isBlank()) {

            return "application/octet-stream";
        }

        return mimeType;
    }
}