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

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final GoogleDriveConnectionRepository
            googleDriveConnectionRepository;

    private final GoogleDriveSourceRepository
            googleDriveSourceRepository;

    private final GoogleDriveCapabilityMapper
            googleDriveCapabilityMapper;

    public DriveOperationLocalStateServiceImpl(
            GoogleDriveItemRepository googleDriveItemRepository,
            GoogleDriveConnectionRepository googleDriveConnectionRepository,
            GoogleDriveSourceRepository googleDriveSourceRepository,
            GoogleDriveCapabilityMapper googleDriveCapabilityMapper
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.googleDriveConnectionRepository =
                googleDriveConnectionRepository;

        this.googleDriveSourceRepository =
                googleDriveSourceRepository;

        this.googleDriveCapabilityMapper =
                googleDriveCapabilityMapper;
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

        if (connectionId == null
                || sourceId == null
                || sourceType == null
                || remoteFile == null) {

            throw new IllegalArgumentException(
                    "Folder persistence information is incomplete"
            );
        }

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
                hasText(
                        remoteFile.mimeType()
                )
                        ? remoteFile.mimeType()
                        : GOOGLE_FOLDER_MIME_TYPE
        );

        item.setCategory(
                GoogleDriveItemCategory.FOLDER
        );

        item.setSourceType(
                sourceType
        );

        item.setDriveId(
                sourceType == GoogleDriveItemSourceType.SHARED_DRIVE
                        ? driveId
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
