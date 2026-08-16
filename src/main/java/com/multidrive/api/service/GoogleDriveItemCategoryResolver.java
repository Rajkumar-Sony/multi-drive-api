package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveItemCategory;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GoogleDriveItemCategoryResolver {

	private static final String GOOGLE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

	private static final Set<String> DOCUMENT_MIME_TYPES = Set.of("application/pdf",

			"application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",

			"application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",

			"application/vnd.ms-powerpoint",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",

			"application/rtf",

			"application/vnd.oasis.opendocument.text", "application/vnd.oasis.opendocument.spreadsheet",
			"application/vnd.oasis.opendocument.presentation",

			"application/json", "application/xml", "text/xml",

			"application/vnd.google-apps.document", "application/vnd.google-apps.spreadsheet",
			"application/vnd.google-apps.presentation", "application/vnd.google-apps.drawing",
			"application/vnd.google-apps.form", "application/vnd.google-apps.script",
			"application/vnd.google-apps.site", "application/vnd.google-apps.jam", "application/vnd.google-apps.map");

	public GoogleDriveItemCategory resolve(String mimeType) {

		if (mimeType == null || mimeType.isBlank()) {

			return GoogleDriveItemCategory.OTHER;
		}

		if (GOOGLE_FOLDER_MIME_TYPE.equals(mimeType)) {

			return GoogleDriveItemCategory.FOLDER;
		}

		if (mimeType.startsWith("image/")) {

			return GoogleDriveItemCategory.IMAGE;
		}

		if (mimeType.startsWith("video/")) {

			return GoogleDriveItemCategory.VIDEO;
		}

		if (mimeType.startsWith("text/")) {

			return GoogleDriveItemCategory.DOCUMENT;
		}

		if (DOCUMENT_MIME_TYPES.contains(mimeType)) {

			return GoogleDriveItemCategory.DOCUMENT;
		}

		return GoogleDriveItemCategory.OTHER;
	}

}
