package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveFileResponse(

		String id,

		String name,

		String mimeType,

		String createdTime,

		String modifiedTime,

		List<String> parents,

		String webViewLink,

		String thumbnailLink,

		String iconLink,

		String size,

		String driveId,

		Boolean trashed,

		Boolean explicitlyTrashed,

		GoogleDriveFileCapabilitiesResponse capabilities) {
}
