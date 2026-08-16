package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.mapper.GoogleDriveCapabilityMapper;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.DriveOperationLocalStateService;
import com.multidrive.api.service.GoogleDriveItemCategoryResolver;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class DriveOperationLocalStateServiceImpl
        implements DriveOperationLocalStateService {

    private static final String GOOGLE_FOLDER_MIME_TYPE =
            "application/vnd.google-apps.folder";

    private static final String DEFAULT_BINARY_MIME_TYPE =
            "application/octet-stream";

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final GoogleDriveConnectionRepository
            googleDriveConnectionRepository;

    private final GoogleDriveSourceRepository
            googleDriveSourceRepository;

    private final GoogleDriveCapabilityMapper
            googleDriveCapabilityMapper;

    private final GoogleDriveItemCategoryResolver
            googleDriveItemCategoryResolver;

    public DriveOperationLocalStateServiceImpl(
            GoogleDriveItemRepository googleDriveItemRepository,
            GoogleDriveConnectionRepository googleDriveConnectionRepository,
            GoogleDriveSourceRepository googleDriveSourceRepository,
            GoogleDriveCapabilityMapper googleDriveCapabilityMapper,
            GoogleDriveItemCategoryResolver googleDriveItemCategoryResolver
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.googleDriveConnectionRepository =
                googleDriveConnectionRepository;

        this.googleDriveSourceRepository =
                googleDriveSourceRepository;

        this.googleDriveCapabilityMapper =
                googleDriveCapabilityMapper;

        this.googleDriveItemCategoryResolver =
                googleDriveItemCategoryResolver;
    }

    @Override
    @Transactional
    public Long createFolder(
            Long connectionId,
            Long sourceId,
            GoogleDriveItemSourceType sourceType,
            String driveId,
            GoogleDriveFileResponse remoteFile
    ) {

        return upsertCreatedItem(
                connectionId,
                sourceId,
                sourceType,
                driveId,
                remoteFile,
                GoogleDriveItemCategory.FOLDER,
                GOOGLE_FOLDER_MIME_TYPE
        );
    }

    @Override
    @Transactional
    public Long createUploadedItem(
            Long connectionId,
            Long sourceId,
            GoogleDriveItemSourceType sourceType,
            String driveId,
            GoogleDriveFileResponse remoteFile
    ) {

        return upsertCreatedItem(
                connectionId,
                sourceId,
                sourceType,
                driveId,
                remoteFile,
                null,
                DEFAULT_BINARY_MIME_TYPE
        );
    }

    @Override
    @Transactional
    public Long createCopiedItem(
            Long connectionId,
            Long sourceId,
            GoogleDriveItemSourceType sourceType,
            String driveId,
            GoogleDriveFileResponse remoteFile
    ) {

        return upsertCreatedItem(
                connectionId,
                sourceId,
                sourceType,
                driveId,
                remoteFile,
                null,
                DEFAULT_BINARY_MIME_TYPE
        );
    }

    @Override
    @Transactional
    public void updateItem(
            Long itemId,
            GoogleDriveFileResponse remoteFile
    ) {

        GoogleDriveItem item =
                requireItem(
                        itemId
                );

        applyRemoteState(
                item,
                remoteFile
        );

        googleDriveItemRepository
                .saveAndFlush(
                        item
                );
    }

    @Override
    @Transactional
    public void markTrashed(
            Long itemId,
            GoogleDriveFileResponse remoteFile
    ) {

        GoogleDriveItem item =
                requireItem(
                        itemId
                );

        applyRemoteState(
                item,
                remoteFile
        );

        googleDriveItemRepository
                .saveAndFlush(
                        item
                );

        if (item.getCategory()
                == GoogleDriveItemCategory.FOLDER) {

            googleDriveItemRepository
                    .markDescendantsTrashed(
                            item.getConnection().getId(),
                            item.getSource().getId(),
                            item.getGoogleFileId()
                    );
        }
    }

    @Override
    @Transactional
    public void markRestored(
            Long itemId,
            GoogleDriveFileResponse remoteFile
    ) {

        GoogleDriveItem item =
                requireItem(
                        itemId
                );

        applyRemoteState(
                item,
                remoteFile
        );

        googleDriveItemRepository
                .saveAndFlush(
                        item
                );

        if (item.getCategory()
                == GoogleDriveItemCategory.FOLDER) {

            googleDriveItemRepository
                    .restoreDescendantsAfterParentRestore(
                            item.getConnection().getId(),
                            item.getSource().getId(),
                            item.getGoogleFileId()
                    );
        }
    }

    @Override
    @Transactional
    public void deleteSubtree(
            Long itemId
    ) {

        GoogleDriveItem item =
                requireItem(
                        itemId
                );

        googleDriveItemRepository
                .deleteLocalSubtree(
                        itemId,
                        item.getConnection().getId(),
                        item.getSource().getId()
                );
    }

    private Long upsertCreatedItem(
            Long connectionId,
            Long sourceId,
            GoogleDriveItemSourceType sourceType,
            String driveId,
            GoogleDriveFileResponse remoteFile,
            GoogleDriveItemCategory forcedCategory,
            String fallbackMimeType
    ) {

        validateCreateRequest(
                connectionId,
                sourceId,
                sourceType,
                remoteFile
        );

        GoogleDriveConnection connection =
                googleDriveConnectionRepository
                        .getReferenceById(
                                connectionId
                        );

        GoogleDriveSource source =
                googleDriveSourceRepository
                        .getReferenceById(
                                sourceId
                        );

        GoogleDriveItem item =
                googleDriveItemRepository
                        .findByConnection_IdAndGoogleFileId(
                                connectionId,
                                remoteFile.id()
                        )
                        .orElseGet(
                                GoogleDriveItem::new
                        );

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

        String mimeType =
                hasText(
                        remoteFile.mimeType()
                )
                        ? remoteFile.mimeType()
                        : fallbackMimeType;

        item.setMimeType(
                mimeType
        );

        item.setCategory(
                forcedCategory != null
                        ? forcedCategory
                        : googleDriveItemCategoryResolver
                        .resolve(
                                mimeType
                        )
        );

        item.setSourceType(
                sourceType
        );

        item.setDriveId(
                sourceType
                        == GoogleDriveItemSourceType.SHARED_DRIVE
                        ? resolveDriveId(
                        driveId,
                        remoteFile
                )
                        : null
        );

        applyRemoteState(
                item,
                remoteFile
        );

        return googleDriveItemRepository
                .saveAndFlush(
                        item
                )
                .getId();
    }

    private void applyRemoteState(
            GoogleDriveItem item,
            GoogleDriveFileResponse remoteFile
    ) {

        if (remoteFile == null) {

            throw new IllegalArgumentException(
                    "remoteFile is required"
            );
        }

        if (hasText(
                remoteFile.name()
        )) {

            item.setName(
                    remoteFile.name()
            );
        }

        if (hasText(
                remoteFile.mimeType()
        )) {

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

        item.setExplicitlyTrashed(
                remoteFile.explicitlyTrashed()
        );

        item.setCapabilities(
                googleDriveCapabilityMapper
                        .toItemCapabilities(
                                remoteFile.capabilities()
                        )
        );
    }

    private GoogleDriveItem requireItem(
            Long itemId
    ) {

        if (itemId == null) {

            throw new IllegalArgumentException(
                    "itemId is required"
            );
        }

        return googleDriveItemRepository
                .findById(
                        itemId
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Drive item not found: "
                                        + itemId
                        )
                );
    }

    private void validateCreateRequest(
            Long connectionId,
            Long sourceId,
            GoogleDriveItemSourceType sourceType,
            GoogleDriveFileResponse remoteFile
    ) {

        if (connectionId == null) {

            throw new IllegalArgumentException(
                    "connectionId is required"
            );
        }

        if (sourceId == null) {

            throw new IllegalArgumentException(
                    "sourceId is required"
            );
        }

        if (sourceType == null) {

            throw new IllegalArgumentException(
                    "sourceType is required"
            );
        }

        if (remoteFile == null
                || remoteFile.id() == null
                || remoteFile.id().isBlank()) {

            throw new IllegalArgumentException(
                    "Google Drive file response is incomplete"
            );
        }
    }

    private String resolveDriveId(
            String requestedDriveId,
            GoogleDriveFileResponse remoteFile
    ) {

        if (hasText(
                remoteFile.driveId()
        )) {

            return remoteFile.driveId();
        }

        return requestedDriveId;
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

        if (!hasText(
                value
        )) {

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

        if (!hasText(
                value
        )) {

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

    private String normalizeRemoteName(
            String value
    ) {

        if (!hasText(
                value
        )) {

            return "Untitled";
        }

        return value;
    }

    private boolean hasText(
            String value
    ) {

        return value != null
                && !value.isBlank();
    }
}
