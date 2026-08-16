package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveCreateFolderRequest;
import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveRenameRequest;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.mapper.DriveItemDetailsMapper;
import com.multidrive.api.mapper.GoogleDriveCapabilityMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.DriveOperationService;
import com.multidrive.api.service.GoogleDriveFileMutationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class DriveOperationServiceImpl
        implements DriveOperationService {

    private static final String GOOGLE_FOLDER_MIME_TYPE =
            "application/vnd.google-apps.folder";

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final GoogleDriveSourceRepository
            googleDriveSourceRepository;

    private final GoogleDriveFileMutationService
            googleDriveFileMutationService;

    private final DriveOperationCapabilityGuard
            driveOperationCapabilityGuard;

    private final GoogleDriveCapabilityMapper
            googleDriveCapabilityMapper;

    private final DriveItemDetailsMapper
            driveItemDetailsMapper;

    public DriveOperationServiceImpl(
            GoogleDriveItemRepository
                    googleDriveItemRepository,

            GoogleDriveSourceRepository
                    googleDriveSourceRepository,

            GoogleDriveFileMutationService
                    googleDriveFileMutationService,

            DriveOperationCapabilityGuard
                    driveOperationCapabilityGuard,

            GoogleDriveCapabilityMapper
                    googleDriveCapabilityMapper,

            DriveItemDetailsMapper
                    driveItemDetailsMapper
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.googleDriveSourceRepository =
                googleDriveSourceRepository;

        this.googleDriveFileMutationService =
                googleDriveFileMutationService;

        this.driveOperationCapabilityGuard =
                driveOperationCapabilityGuard;

        this.googleDriveCapabilityMapper =
                googleDriveCapabilityMapper;

        this.driveItemDetailsMapper =
                driveItemDetailsMapper;
    }

    @Override
    @Transactional
    public DriveItemDetailsResponse createFolder(
            String googleSubjectId,
            DriveCreateFolderRequest request
    ) {

        validateGoogleSubjectId(
                googleSubjectId
        );

        if (request == null) {

            throw new IllegalArgumentException(
                    "request is required"
            );
        }

        if (request.sourceId() == null) {

            throw new IllegalArgumentException(
                    "sourceId is required"
            );
        }

        String normalizedName =
                normalizeName(
                        request.name()
                );

        /*
         * Two paths:
         *
         * 1. parentItemId == null
         *      create directly in source root
         *
         * 2. parentItemId != null
         *      create inside an indexed folder
         */
        if (request.parentItemId() == null) {

            return createFolderInSourceRoot(
                    googleSubjectId,
                    request.sourceId(),
                    normalizedName
            );
        }

        return createFolderInsideFolder(
                googleSubjectId,
                request.sourceId(),
                request.parentItemId(),
                normalizedName
        );
    }

    @Override
    @Transactional
    public DriveItemDetailsResponse rename(
            String googleSubjectId,
            Long itemId,
            DriveRenameRequest request
    ) {

        validateGoogleSubjectId(
                googleSubjectId
        );

        if (itemId == null) {

            throw new IllegalArgumentException(
                    "itemId is required"
            );
        }

        if (request == null) {

            throw new IllegalArgumentException(
                    "request is required"
            );
        }

        String normalizedName =
                normalizeName(
                        request.name()
                );

        GoogleDriveItem item =
                googleDriveItemRepository
                        .findOwnedItemForDetails(
                                itemId,
                                googleSubjectId
                        )
                        .orElseThrow(
                                () ->
                                        new DriveItemNotFoundException(
                                                itemId
                                        )
                        );

        GoogleDriveConnection connection =
                requireConnection(
                        item
                );

        Long userId =
                requireUserId(
                        connection
                );

        /*
         * Re-read Google before mutation.
         *
         * The capability stored in PostgreSQL is useful
         * for fast UI decisions, but permissions may have
         * changed since the last sync.
         */
        GoogleDriveFileResponse currentRemoteFile =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteFile.capabilities()
                                != null
                                ? currentRemoteFile
                                        .capabilities()
                                        .canRename()
                                : null,
                        "RENAME"
                );

        GoogleDriveFileResponse updatedRemoteFile =
                googleDriveFileMutationService
                        .rename(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId(),
                                normalizedName
                        );

        GoogleDriveItem updatedItem =
                updateExistingLocalItem(
                        item,
                        updatedRemoteFile
                );

        return driveItemDetailsMapper
                .toResponse(
                        updatedItem
                );
    }

    private DriveItemDetailsResponse
    createFolderInSourceRoot(
            String googleSubjectId,
            Long sourceId,
            String name
    ) {

        GoogleDriveSource source =
                googleDriveSourceRepository
                        .findOwnedSourceForOperation(
                                sourceId,
                                googleSubjectId,
                                GoogleDriveSourceStatus.ACTIVE
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Active Google Drive source not found"
                                )
                        );

        if (source.getRootFolderId() == null
                || source.getRootFolderId().isBlank()) {

            throw new IllegalStateException(
                    "Drive source root folder is missing"
            );
        }

        driveOperationCapabilityGuard
                .requireAllowed(
                        source.getCapabilities()
                                != null
                                ? source.getCapabilities()
                                        .getCanAddChildren()
                                : null,
                        "CREATE_FOLDER",
                        "Cannot create a folder in this Drive root"
                );

        GoogleDriveConnection connection =
                requireConnection(
                        source
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse createdRemoteFile =
                googleDriveFileMutationService
                        .createFolder(
                                connection.getId(),
                                userId,
                                source.getRootFolderId(),
                                name
                        );

        GoogleDriveItem createdItem =
                createLocalFolderItem(
                        connection,
                        source,
                        createdRemoteFile
                );

        return driveItemDetailsMapper
                .toResponse(
                        createdItem
                );
    }

    private DriveItemDetailsResponse
    createFolderInsideFolder(
            String googleSubjectId,
            Long sourceId,
            Long parentItemId,
            String name
    ) {

        GoogleDriveItem parentItem =
                googleDriveItemRepository
                        .findOwnedItemForDetails(
                                parentItemId,
                                googleSubjectId
                        )
                        .orElseThrow(
                                () ->
                                        new DriveItemNotFoundException(
                                                parentItemId
                                        )
                        );

        if (parentItem.getCategory()
                != GoogleDriveItemCategory.FOLDER) {

            throw new IllegalArgumentException(
                    "parentItemId must reference a folder"
            );
        }

        if (parentItem.isTrashed()) {

            throw new IllegalArgumentException(
                    "Cannot create a folder inside a trashed folder"
            );
        }

        GoogleDriveSource source =
                requireSource(
                        parentItem
                );

        if (!source.getId().equals(
                sourceId
        )) {

            throw new IllegalArgumentException(
                    "parentItemId does not belong to sourceId"
            );
        }

        GoogleDriveConnection connection =
                requireConnection(
                        parentItem
                );

        Long userId =
                requireUserId(
                        connection
                );

        /*
         * Get the current Google capability instead of
         * relying only on the local snapshot.
         */
        GoogleDriveFileResponse currentRemoteParent =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                parentItem.getGoogleFileId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteParent.capabilities()
                                != null
                                ? currentRemoteParent
                                        .capabilities()
                                        .canAddChildren()
                                : null,
                        "CREATE_FOLDER",
                        "Cannot add children to the selected folder"
                );

        GoogleDriveFileResponse createdRemoteFile =
                googleDriveFileMutationService
                        .createFolder(
                                connection.getId(),
                                userId,
                                parentItem.getGoogleFileId(),
                                name
                        );

        GoogleDriveItem createdItem =
                createLocalFolderItem(
                        connection,
                        source,
                        createdRemoteFile
                );

        return driveItemDetailsMapper
                .toResponse(
                        createdItem
                );
    }

    private GoogleDriveItem createLocalFolderItem(
            GoogleDriveConnection connection,
            GoogleDriveSource source,
            GoogleDriveFileResponse remoteFile
    ) {

        GoogleDriveItem item =
                new GoogleDriveItem();

        item.setConnection(
                connection
        );

        item.setSource(
                source
        );

        item.setGoogleFileId(
                remoteFile.id()
        );

        item.setName(
                normalizeRemoteName(
                        remoteFile.name()
                )
        );

        item.setMimeType(
                remoteFile.mimeType() != null
                        ? remoteFile.mimeType()
                        : GOOGLE_FOLDER_MIME_TYPE
        );

        item.setCategory(
                GoogleDriveItemCategory.FOLDER
        );

        GoogleDriveItemSourceType sourceType =
                resolveItemSourceType(
                        source
                );

        item.setSourceType(
                sourceType
        );

        item.setDriveId(
                sourceType
                        == GoogleDriveItemSourceType.SHARED_DRIVE
                        ? resolveDriveId(
                                source,
                                remoteFile
                        )
                        : null
        );

        item.setParentId(
                extractParentId(
                        remoteFile.parents()
                )
        );

        item.setWebViewLink(
                remoteFile.webViewLink()
        );

        item.setThumbnailLink(
                remoteFile.thumbnailLink()
        );

        item.setIconLink(
                remoteFile.iconLink()
        );

        item.setSizeBytes(
                parseSize(
                        remoteFile.size()
                )
        );

        item.setGoogleCreatedTime(
                parseInstant(
                        remoteFile.createdTime()
                )
        );

        item.setGoogleModifiedTime(
                parseInstant(
                        remoteFile.modifiedTime()
                )
        );

        item.setTrashed(
                Boolean.TRUE.equals(
                        remoteFile.trashed()
                )
        );

        item.setCapabilities(
                googleDriveCapabilityMapper
                        .toItemCapabilities(
                                remoteFile.capabilities()
                        )
        );

        return googleDriveItemRepository
                .saveAndFlush(
                        item
                );
    }

    private GoogleDriveItem updateExistingLocalItem(
            GoogleDriveItem item,
            GoogleDriveFileResponse remoteFile
    ) {

        item.setName(
                normalizeRemoteName(
                        remoteFile.name()
                )
        );

        if (remoteFile.mimeType() != null
                && !remoteFile.mimeType().isBlank()) {

            item.setMimeType(
                    remoteFile.mimeType()
            );
        }

        item.setParentId(
                extractParentId(
                        remoteFile.parents()
                )
        );

        item.setWebViewLink(
                remoteFile.webViewLink()
        );

        item.setThumbnailLink(
                remoteFile.thumbnailLink()
        );

        item.setIconLink(
                remoteFile.iconLink()
        );

        item.setSizeBytes(
                parseSize(
                        remoteFile.size()
                )
        );

        item.setGoogleCreatedTime(
                parseInstant(
                        remoteFile.createdTime()
                )
        );

        item.setGoogleModifiedTime(
                parseInstant(
                        remoteFile.modifiedTime()
                )
        );

        item.setTrashed(
                Boolean.TRUE.equals(
                        remoteFile.trashed()
                )
        );

        item.setCapabilities(
                googleDriveCapabilityMapper
                        .toItemCapabilities(
                                remoteFile.capabilities()
                        )
        );

        return googleDriveItemRepository
                .saveAndFlush(
                        item
                );
    }

    private GoogleDriveItemSourceType resolveItemSourceType(
            GoogleDriveSource source
    ) {

        if (source.getSourceType()
                == GoogleDriveSourceType.SHARED_DRIVE) {

            return GoogleDriveItemSourceType.SHARED_DRIVE;
        }

        return GoogleDriveItemSourceType.MY_DRIVE;
    }

    private String resolveDriveId(
            GoogleDriveSource source,
            GoogleDriveFileResponse remoteFile
    ) {

        if (remoteFile.driveId() != null
                && !remoteFile.driveId().isBlank()) {

            return remoteFile.driveId();
        }

        return source.getGoogleDriveId();
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
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        try {

            return Long.parseLong(
                    value
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

    private GoogleDriveConnection requireConnection(
            GoogleDriveItem item
    ) {

        if (item.getConnection() == null
                || item.getConnection().getId() == null) {

            throw new IllegalStateException(
                    "Drive item's Google connection is missing"
            );
        }

        return item.getConnection();
    }

    private GoogleDriveConnection requireConnection(
            GoogleDriveSource source
    ) {

        if (source.getConnection() == null
                || source.getConnection().getId() == null) {

            throw new IllegalStateException(
                    "Drive source's Google connection is missing"
            );
        }

        return source.getConnection();
    }

    private GoogleDriveSource requireSource(
            GoogleDriveItem item
    ) {

        if (item.getSource() == null
                || item.getSource().getId() == null) {

            throw new IllegalStateException(
                    "Drive item's source is missing"
            );
        }

        return item.getSource();
    }

    private Long requireUserId(
            GoogleDriveConnection connection
    ) {

        if (connection.getUser() == null
                || connection.getUser().getId() == null) {

            throw new IllegalStateException(
                    "Application user is missing"
            );
        }

        return connection
                .getUser()
                .getId();
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

    private String normalizeRemoteName(
            String name
    ) {

        if (name == null
                || name.isBlank()) {

            return "Untitled";
        }

        return name;
    }

    private void validateGoogleSubjectId(
            String googleSubjectId
    ) {

        if (googleSubjectId == null
                || googleSubjectId.isBlank()) {

            throw new IllegalArgumentException(
                    "googleSubjectId is required"
            );
        }
    }
}
