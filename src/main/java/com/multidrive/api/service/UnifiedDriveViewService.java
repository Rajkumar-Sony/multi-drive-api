package com.multidrive.api.service;

import com.multidrive.api.dto.UnifiedDriveItemsPageResponse;

public interface UnifiedDriveViewService {

    UnifiedDriveItemsPageResponse getDashboard(
            Long userId,
            Integer page,
            Integer size
    );

    UnifiedDriveItemsPageResponse getDocs(
            Long userId,
            Integer page,
            Integer size
    );

    UnifiedDriveItemsPageResponse getGallery(
            Long userId,
            Integer page,
            Integer size
    );

    UnifiedDriveItemsPageResponse getVideos(
            Long userId,
            Integer page,
            Integer size
    );
}