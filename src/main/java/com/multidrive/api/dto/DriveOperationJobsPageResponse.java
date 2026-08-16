package com.multidrive.api.dto;

import java.util.List;

public record DriveOperationJobsPageResponse(

		List<DriveOperationJobResponse> items,

		int page,

		int size,

		long totalElements,

		int totalPages,

		boolean first,

		boolean last) {
}
