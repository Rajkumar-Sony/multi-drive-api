package com.multidrive.api.dto;

import java.util.List;

public record GoogleDriveUploadMetadataRequest(

		String name,

		String mimeType,

		List<String> parents) {
}
