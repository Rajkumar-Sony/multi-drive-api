package com.multidrive.api.service;

import com.multidrive.api.dto.UnifiedDriveItemsPageResponse;

public interface UnifiedDriveViewService {

	UnifiedDriveItemsPageResponse getDashboard(Long userId, Long connectionId, String parentId, String searchQuery,
			Integer page, Integer size);

	UnifiedDriveItemsPageResponse getDocs(Long userId, Long connectionId, String parentId, String searchQuery,
			Integer page, Integer size);

	UnifiedDriveItemsPageResponse getGallery(Long userId, Long connectionId, String parentId, String searchQuery,
			Integer page, Integer size);

	UnifiedDriveItemsPageResponse getVideos(Long userId, Long connectionId, String parentId, String searchQuery,
			Integer page, Integer size);

}