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
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.DriveItemLookupService;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.DriveOperationLocalStateService;
import com.multidrive.api.service.DriveOperationService;
import com.multidrive.api.service.GoogleDriveFileMutationService;

import org.springframework.stereotype.Service;

@Service
public class DriveOperationServiceImpl
        implements DriveOperationService {

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final GoogleDriveSourceRepository
            googleDriveSourceRepository;

    private final GoogleDriveFileMutationService
            googleDriveFileMutationService;

    private final DriveOperationCapabilityGuard
            driveOperationCapabilityGuard;

    private final DriveOperationLocalStateService
            driveOperationLocalStateService;

    private final DriveItemLookupService
            driveItemLookupService;

    public DriveOperationServiceImpl(
            GoogleDriveItemRepository googleDriveItemRepository,
            GoogleDriveSourceRepository googleDriveSourceRepository,
            GoogleDriveFileMutationService googleDriveFileMutationService,
            DriveOperationCapabilityGuard driveOperationCapabilityGuard,
            DriveOperationLocalStateService driveOperationLocalStateService,
            DriveItemLookupService driveItemLookupService
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.googleDriveSourceRepository =
                googleDriveSourceRepository;

        this.googleDriveFileMutationService =
                googleDriveFileMutationService;

        this.driveOperationCapabilityGuard =
                driveOperationCapabilityGuard;

        this.driveOperationLocalStateService =
                driveOperationLocalStateService;

        this.driveItemLookupService =
                driveItemLookupService;
    }

    @Override
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
    public DriveItemDetailsResponse rename(
            String googleSubjectId,
            Long itemId,
            DriveRenameRequest request
    ) {

        validateGoogleSubjectId(
                googleSubjectId
        );

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
                requireOwnedItem(
                        googleSubjectId,
                        itemId
                );

        GoogleDriveConnection connection =
                requireConnection(
                        item
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse currentRemoteFile =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteFile.capabilities() != null
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

        driveOperationLocalStateService
                .updateItem(
                        item.getId(),
                        updatedRemoteFile
                );

        return driveItemLookupService
                .getItem(
                        googleSubjectId,
                        item.getId()
                );
    }

    @Override
    public DriveItemDetailsResponse trash(
            String googleSubjectId,
            Long itemId
    ) {

        validateGoogleSubjectId(
                googleSubjectId
        );

        GoogleDriveItem item =
                requireOwnedItem(
                        googleSubjectId,
                        itemId
                );

        GoogleDriveConnection connection =
                requireConnection(
                        item
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse currentRemoteFile =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        if (Boolean.TRUE.equals(
                currentRemoteFile.trashed()
        )) {

            driveOperationLocalStateService
                    .markTrashed(
                            item.getId(),
                            currentRemoteFile
                    );

            return driveItemLookupService
                    .getItem(
                            googleSubjectId,
                            item.getId()
                    );
        }

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteFile.capabilities() != null
                                ? currentRemoteFile
                                        .capabilities()
                                        .canTrash()
                                : null,
                        "TRASH"
                );

        GoogleDriveFileResponse trashedRemoteFile =
                googleDriveFileMutationService
                        .trash(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        driveOperationLocalStateService
                .markTrashed(
                        item.getId(),
                        trashedRemoteFile
                );

        return driveItemLookupService
                .getItem(
                        googleSubjectId,
                        item.getId()
                );
    }

    @Override
    public DriveItemDetailsResponse restore(
            String googleSubjectId,
            Long itemId
    ) {

        validateGoogleSubjectId(
                googleSubjectId
        );

        GoogleDriveItem item =
                requireOwnedItem(
                        googleSubjectId,
                        itemId
                );

        GoogleDriveConnection connection =
                requireConnection(
                        item
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse currentRemoteFile =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        if (!Boolean.TRUE.equals(
                currentRemoteFile.trashed()
        )) {

            driveOperationLocalStateService
                    .markRestored(
                            item.getId(),
                            currentRemoteFile
                    );

            return driveItemLookupService
                    .getItem(
                            googleSubjectId,
                            item.getId()
                    );
        }

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteFile.capabilities() != null
                                ? currentRemoteFile
                                        .capabilities()
                                        .canUntrash()
                                : null,
                        "RESTORE"
                );

        GoogleDriveFileResponse restoredRemoteFile =
                googleDriveFileMutationService
                        .restore(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        driveOperationLocalStateService
                .markRestored(
                        item.getId(),
                        restoredRemoteFile
                );

        return driveItemLookupService
                .getItem(
                        googleSubjectId,
                        item.getId()
                );
    }

    @Override
    public void permanentlyDelete(
            String googleSubjectId,
            Long itemId
    ) {

        validateGoogleSubjectId(
                googleSubjectId
        );

        GoogleDriveItem item =
                requireOwnedItem(
                        googleSubjectId,
                        itemId
                );

        GoogleDriveConnection connection =
                requireConnection(
                        item
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse currentRemoteFile =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                item.getGoogleFileId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteFile.capabilities() != null
                                ? currentRemoteFile
                                        .capabilities()
                                        .canDelete()
                                : null,
                        "PERMANENT_DELETE"
                );

        googleDriveFileMutationService
                .permanentlyDelete(
                        connection.getId(),
                        userId,
                        item.getGoogleFileId()
                );

        driveOperationLocalStateService
                .deleteSubtree(
                        item.getId()
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

        GoogleDriveConnection connection =
                requireConnection(
                        source
                );

        Long userId =
                requireUserId(
                        connection
                );

        GoogleDriveFileResponse currentRemoteParent =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                source.getRootFolderId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteParent.capabilities() != null
                                ? currentRemoteParent
                                        .capabilities()
                                        .canAddChildren()
                                : null,
                        "CREATE_FOLDER"
                );

        GoogleDriveFileResponse createdRemoteFile =
                googleDriveFileMutationService
                        .createFolder(
                                connection.getId(),
                                userId,
                                source.getRootFolderId(),
                                name
                        );

        GoogleDriveItemSourceType sourceType =
                resolveItemSourceType(
                        source
                );

        Long createdItemId =
                driveOperationLocalStateService
                        .createFolder(
                                connection.getId(),
                                source.getId(),
                                sourceType,
                                sourceType == GoogleDriveItemSourceType.SHARED_DRIVE
                                        ? source.getGoogleDriveId()
                                        : null,
                                createdRemoteFile
                        );

        return driveItemLookupService
                .getItem(
                        googleSubjectId,
                        createdItemId
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
                requireOwnedItem(
                        googleSubjectId,
                        parentItemId
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

        GoogleDriveFileResponse currentRemoteParent =
                googleDriveFileMutationService
                        .getFile(
                                connection.getId(),
                                userId,
                                parentItem.getGoogleFileId()
                        );

        driveOperationCapabilityGuard
                .requireAllowed(
                        currentRemoteParent.capabilities() != null
                                ? currentRemoteParent
                                        .capabilities()
                                        .canAddChildren()
                                : null,
                        "CREATE_FOLDER"
                );

        GoogleDriveFileResponse createdRemoteFile =
                googleDriveFileMutationService
                        .createFolder(
                                connection.getId(),
                                userId,
                                parentItem.getGoogleFileId(),
                                name
                        );

        GoogleDriveItemSourceType sourceType =
                resolveItemSourceType(
                        source
                );

        Long createdItemId =
                driveOperationLocalStateService
                        .createFolder(
                                connection.getId(),
                                source.getId(),
                                sourceType,
                                sourceType == GoogleDriveItemSourceType.SHARED_DRIVE
                                        ? source.getGoogleDriveId()
                                        : null,
                                createdRemoteFile
                        );

        return driveItemLookupService
                .getItem(
                        googleSubjectId,
                        createdItemId
                );
    }

    private GoogleDriveItem requireOwnedItem(
            String googleSubjectId,
            Long itemId
    ) {

        if (itemId == null) {

            throw new IllegalArgumentException(
                    "itemId is required"
            );
        }

        return googleDriveItemRepository
                .findOwnedItemForDetails(
                        itemId,
                        googleSubjectId
                )
                .orElseThrow(
                        () -> new DriveItemNotFoundException(
                                itemId
                        )
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
