package com.multidrive.api.dto;

import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;

import java.time.LocalDateTime;

public record GoogleDriveSourceResponse(

		Long id,

		Long connectionId,

		String accountEmail,

		GoogleDriveSourceType sourceType,

		String googleDriveId,

		String rootFolderId,

		String name,

		GoogleDriveSourceStatus status,

		LocalDateTime lastDiscoveredAt) {
}