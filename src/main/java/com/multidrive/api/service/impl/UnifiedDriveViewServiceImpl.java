package com.multidrive.api.service.impl;

import com.multidrive.api.dto.UnifiedDriveItemResponse;
import com.multidrive.api.dto.UnifiedDriveItemsPageResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.service.UnifiedDriveViewService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnifiedDriveViewServiceImpl
        implements UnifiedDriveViewService {

    private static final int DEFAULT_PAGE =
            0;

    private static final int DEFAULT_PAGE_SIZE =
            50;

    private static final int MAX_PAGE_SIZE =
            200;

    private final GoogleDriveItemRepository
            googleDriveItemRepository;

    public UnifiedDriveViewServiceImpl(
            GoogleDriveItemRepository
                    googleDriveItemRepository
    ) {

        this.googleDriveItemRepository =
                googleDriveItemRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UnifiedDriveItemsPageResponse getDashboard(
            Long userId,
            Integer page,
            Integer size
    ) {

        validateUserId(
                userId
        );

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        Page<GoogleDriveItem> itemPage =
                googleDriveItemRepository
                        .findAllByConnection_User_IdAndTrashedFalse(
                                userId,
                                pageable
                        );

        return toPageResponse(
                itemPage
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UnifiedDriveItemsPageResponse getDocs(
            Long userId,
            Integer page,
            Integer size
    ) {

        return getItemsByCategory(
                userId,
                GoogleDriveItemCategory.DOCUMENT,
                page,
                size
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UnifiedDriveItemsPageResponse getGallery(
            Long userId,
            Integer page,
            Integer size
    ) {

        return getItemsByCategory(
                userId,
                GoogleDriveItemCategory.IMAGE,
                page,
                size
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UnifiedDriveItemsPageResponse getVideos(
            Long userId,
            Integer page,
            Integer size
    ) {

        return getItemsByCategory(
                userId,
                GoogleDriveItemCategory.VIDEO,
                page,
                size
        );
    }

    private UnifiedDriveItemsPageResponse
    getItemsByCategory(
            Long userId,
            GoogleDriveItemCategory category,
            Integer page,
            Integer size
    ) {

        validateUserId(
                userId
        );

        Pageable pageable =
                createPageable(
                        page,
                        size
                );

        Page<GoogleDriveItem> itemPage =
                googleDriveItemRepository
                        .findAllByConnection_User_IdAndCategoryAndTrashedFalse(
                                userId,
                                category,
                                pageable
                        );

        return toPageResponse(
                itemPage
        );
    }

    private Pageable createPageable(
            Integer page,
            Integer size
    ) {

        int requestedPage =
                page == null
                        ? DEFAULT_PAGE
                        : page;

        int requestedSize =
                size == null
                        ? DEFAULT_PAGE_SIZE
                        : size;

        if (requestedPage < 0) {

            throw new IllegalArgumentException(
                    "page must be 0 or greater"
            );
        }

        if (requestedSize < 1
                || requestedSize > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "size must be between 1 and 200"
            );
        }

        /*
         * Most recently modified Drive items appear first.
         *
         * id is added as a stable secondary sort when
         * multiple files have the same modified time.
         */
        Sort sort =
                Sort.by(
                                Sort.Direction.DESC,
                                "googleModifiedTime"
                        )
                        .and(
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "id"
                                )
                        );

        return PageRequest.of(
                requestedPage,
                requestedSize,
                sort
        );
    }

    private UnifiedDriveItemsPageResponse toPageResponse(
            Page<GoogleDriveItem> itemPage
    ) {

        List<UnifiedDriveItemResponse> items =
                itemPage
                        .getContent()
                        .stream()
                        .map(
                                this::toResponse
                        )
                        .toList();

        return new UnifiedDriveItemsPageResponse(
                items,
                itemPage.getNumber(),
                itemPage.getSize(),
                itemPage.getTotalElements(),
                itemPage.getTotalPages(),
                itemPage.isFirst(),
                itemPage.isLast()
        );
    }

    private UnifiedDriveItemResponse toResponse(
            GoogleDriveItem item
    ) {

        GoogleDriveConnection connection =
                item.getConnection();

        return new UnifiedDriveItemResponse(
                item.getId(),

                connection.getId(),

                connection.getGoogleEmail(),

                item.getGoogleFileId(),

                item.getName(),

                item.getMimeType(),

                item.getCategory(),

                item.getSourceType(),

                item.getParentId(),

                item.getDriveId(),

                item.getWebViewLink(),

                item.getThumbnailLink(),

                item.getIconLink(),

                item.getSizeBytes(),

                item.getGoogleCreatedTime(),

                item.getGoogleModifiedTime()
        );
    }

    private void validateUserId(
            Long userId
    ) {

        if (userId == null) {

            throw new IllegalArgumentException(
                    "userId is required"
            );
        }
    }
}