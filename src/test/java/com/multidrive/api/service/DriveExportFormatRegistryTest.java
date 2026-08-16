package com.multidrive.api.service;

import com.multidrive.api.dto.DriveExportFormatResponse;
import com.multidrive.api.exception.DriveContentNotSupportedException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriveExportFormatRegistryTest {

	private final DriveExportFormatRegistry registry = new DriveExportFormatRegistry();

	@Test
	void resolvesDefaultFormatWhenRequestIsBlank() {

		DriveExportFormatResponse format = registry.resolve(DriveExportFormatRegistry.GOOGLE_DOCUMENT, " ");

		assertThat(format.label()).isEqualTo("Microsoft Word");
		assertThat(format.mimeType())
			.isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		assertThat(format.defaultFormat()).isTrue();
	}

	@Test
	void resolvesRequestedFormatCaseInsensitively() {

		DriveExportFormatResponse format = registry.resolve(DriveExportFormatRegistry.GOOGLE_PRESENTATION,
				" IMAGE/PNG ");

		assertThat(format.label()).isEqualTo("PNG - First Slide");
		assertThat(format.extension()).isEqualTo(".png");
	}

	@Test
	void returnsEmptyFormatsForUnsupportedSourceMimeType() {

		assertThat(registry.isExportable("application/pdf")).isFalse();
		assertThat(registry.getFormats("application/pdf")).isEmpty();
	}

	@Test
	void rejectsUnsupportedExportSourceAndRequestedMimeTypes() {

		assertThatThrownBy(() -> registry.resolve("application/pdf", null))
			.isInstanceOf(DriveContentNotSupportedException.class)
			.hasMessage("This Google Drive file type cannot be exported by this endpoint");

		assertThatThrownBy(() -> registry.resolve(DriveExportFormatRegistry.GOOGLE_SPREADSHEET, "application/json"))
			.isInstanceOf(DriveContentNotSupportedException.class)
			.hasMessage("Requested export MIME type is not supported for this Google Workspace file");
	}

	@Test
	void detectsGoogleNativeMimeTypes() {

		assertThat(registry.isGoogleNativeMimeType("application/vnd.google-apps.document")).isTrue();
		assertThat(registry.isGoogleNativeMimeType("application/pdf")).isFalse();
		assertThat(registry.isGoogleNativeMimeType(null)).isFalse();
	}

}
