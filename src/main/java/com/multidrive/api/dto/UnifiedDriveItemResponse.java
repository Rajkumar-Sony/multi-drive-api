package com.multidrive.api.dto;

import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;

import java.time.Instant;

public record UnifiedDriveItemResponse(

		Long id,

		Long connectionId,

		String accountEmail,

		Long sourceId,

		String sourceName,

		String googleFileId,

		String name,

		String mimeType,

		GoogleDriveItemCategory category,

		GoogleDriveItemSourceType sourceType,

		String parentId,

		String driveId,

		String webViewLink,

		String thumbnailLink,

		String iconLink,

		Long sizeBytes,

		Instant createdTime,

		Instant modifiedTime,

		DriveItemCapabilitiesResponse capabilities) {
}