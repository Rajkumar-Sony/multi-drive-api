package com.multidrive.api.dto;

import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;

import java.time.Instant;

public record DriveItemDetailsResponse(

		Long id,

		Long connectionId,

		String accountEmail,

		Long sourceId,

		String sourceName,

		GoogleDriveItemSourceType sourceType,

		String googleDriveId,

		String rootFolderId,

		String googleFileId,

		String parentId,

		String name,

		String mimeType,

		GoogleDriveItemCategory category,

		boolean folder,

		boolean trashed,

		String webViewLink,

		String thumbnailLink,

		String iconLink,

		Long sizeBytes,

		Instant createdTime,

		Instant modifiedTime,

		DriveItemCapabilitiesResponse capabilities) {
}