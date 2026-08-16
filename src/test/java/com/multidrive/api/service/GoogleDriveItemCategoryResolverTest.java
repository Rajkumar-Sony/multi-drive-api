package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveItemCategory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleDriveItemCategoryResolverTest {

	private final GoogleDriveItemCategoryResolver resolver = new GoogleDriveItemCategoryResolver();

	@Test
	void resolvesFoldersImagesVideosDocumentsAndOtherFiles() {

		assertThat(resolver.resolve("application/vnd.google-apps.folder")).isEqualTo(GoogleDriveItemCategory.FOLDER);
		assertThat(resolver.resolve("image/png")).isEqualTo(GoogleDriveItemCategory.IMAGE);
		assertThat(resolver.resolve("video/mp4")).isEqualTo(GoogleDriveItemCategory.VIDEO);
		assertThat(resolver.resolve("text/plain")).isEqualTo(GoogleDriveItemCategory.DOCUMENT);
		assertThat(resolver.resolve("application/pdf")).isEqualTo(GoogleDriveItemCategory.DOCUMENT);
		assertThat(resolver.resolve("application/vnd.google-apps.spreadsheet"))
			.isEqualTo(GoogleDriveItemCategory.DOCUMENT);
		assertThat(resolver.resolve("application/octet-stream")).isEqualTo(GoogleDriveItemCategory.OTHER);
	}

	@Test
	void treatsBlankMimeTypeAsOther() {

		assertThat(resolver.resolve(null)).isEqualTo(GoogleDriveItemCategory.OTHER);
		assertThat(resolver.resolve(" ")).isEqualTo(GoogleDriveItemCategory.OTHER);
	}

}
