package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

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

		Map<String, String> appProperties,

		GoogleDriveFileCapabilitiesResponse capabilities) {

	public GoogleDriveFileResponse(String id, String name, String mimeType, String createdTime, String modifiedTime,
			List<String> parents, String webViewLink, String thumbnailLink, String iconLink, String size,
			String driveId, Boolean trashed, Boolean explicitlyTrashed,
			GoogleDriveFileCapabilitiesResponse capabilities) {

		this(id, name, mimeType, createdTime, modifiedTime, parents, webViewLink, thumbnailLink, iconLink, size,
				driveId, trashed, explicitlyTrashed, null, capabilities);
	}

}
