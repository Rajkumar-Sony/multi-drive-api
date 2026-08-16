package com.multidrive.api.dto;

import java.util.List;

public record UnifiedDriveItemsPageResponse(

		List<UnifiedDriveItemResponse> items,

		int page,

		int size,

		long totalElements,

		int totalPages,

		boolean first,

		boolean last) {
}