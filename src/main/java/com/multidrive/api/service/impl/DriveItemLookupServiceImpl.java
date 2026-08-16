package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.mapper.DriveItemDetailsMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.DriveItemLookupService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriveItemLookupServiceImpl
        implements DriveItemLookupService {

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    private final DriveItemDetailsMapper
            driveItemDetailsMapper;

    public DriveItemLookupServiceImpl(
            GoogleDriveItemRepository
                    googleDriveItemRepository,

            DriveItemDetailsMapper
                    driveItemDetailsMapper
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;

        this.driveItemDetailsMapper =
                driveItemDetailsMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DriveItemDetailsResponse getItem(
            String googleSubjectId,
            Long itemId
    ) {

        validateInput(
                googleSubjectId,
                itemId
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

        return driveItemDetailsMapper
                .toResponse(
                        item
                );
    }

    private void validateInput(
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
    }
}