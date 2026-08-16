package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.mapper.DriveItemCapabilityResponseMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.DriveItemLookupService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriveItemLookupServiceImpl
        implements DriveItemLookupService {

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final DriveItemCapabilityResponseMapper
            driveItemCapabilityResponseMapper;

    public DriveItemLookupServiceImpl(
            GoogleDriveItemRepository
                    googleDriveItemRepository,

            DriveItemCapabilityResponseMapper
                    driveItemCapabilityResponseMapper
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.driveItemCapabilityResponseMapper =
                driveItemCapabilityResponseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DriveItemDetailsResponse getItem(
            String googleSubjectId,
            Long itemId
    ) {

        if (googleSubjectId == null
                || googleSubjectId.isBlank()) {

            throw new IllegalArgumentException(
                    "googleSubjectId is required"
            );
        }

        if (itemId == null) {

            throw new IllegalArgumentException(
                    "itemId is required"
            );
        }

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
                item.getConnection();

        GoogleDriveSource source =
                item.getSource();

        if (connection == null
                || source == null) {

            throw new IllegalStateException(
                    "Drive item source information is incomplete"
            );
        }

        return new DriveItemDetailsResponse(

                item.getId(),

                connection.getId(),

                connection.getGoogleEmail(),

                source.getId(),

                source.getName(),

                item.getSourceType(),

                source.getGoogleDriveId(),

                source.getRootFolderId(),

                item.getGoogleFileId(),

                item.getParentId(),

                item.getName(),

                item.getMimeType(),

                item.getCategory(),

                item.getCategory()
                        == GoogleDriveItemCategory.FOLDER,

                item.isTrashed(),

                item.getWebViewLink(),

                item.getThumbnailLink(),

                item.getIconLink(),

                item.getSizeBytes(),

                item.getGoogleCreatedTime(),

                item.getGoogleModifiedTime(),

                driveItemCapabilityResponseMapper
                        .toResponse(
                                item.getCapabilities()
                        )
        );
    }
}