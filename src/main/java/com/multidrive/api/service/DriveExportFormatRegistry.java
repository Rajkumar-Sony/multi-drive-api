package com.multidrive.api.service;

import com.multidrive.api.dto.DriveExportFormatResponse;
import com.multidrive.api.exception.DriveContentNotSupportedException;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DriveExportFormatRegistry {

	public static final String GOOGLE_DOCUMENT = "application/vnd.google-apps.document";

	public static final String GOOGLE_SPREADSHEET = "application/vnd.google-apps.spreadsheet";

	public static final String GOOGLE_PRESENTATION = "application/vnd.google-apps.presentation";

	public static final String GOOGLE_DRAWING = "application/vnd.google-apps.drawing";

	public static final String GOOGLE_FOLDER = "application/vnd.google-apps.folder";

	private static final Map<String, List<DriveExportFormatResponse>> EXPORT_FORMATS = Map.of(

			GOOGLE_DOCUMENT,
			List.of(format("Microsoft Word", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
					".docx", true), format("PDF", "application/pdf", ".pdf", false),
					format("OpenDocument Text", "application/vnd.oasis.opendocument.text", ".odt", false),
					format("Rich Text", "application/rtf", ".rtf", false),
					format("Plain Text", "text/plain", ".txt", false),
					format("Markdown", "text/markdown", ".md", false),
					format("EPUB", "application/epub+zip", ".epub", false),
					format("Web Page", "application/zip", ".zip", false)),

			GOOGLE_SPREADSHEET,
			List.of(format("Microsoft Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
					".xlsx", true), format("PDF", "application/pdf", ".pdf", false),
					format("OpenDocument Spreadsheet", "application/vnd.oasis.opendocument.spreadsheet", ".ods", false),
					format("CSV - First Sheet", "text/csv", ".csv", false),
					format("TSV - First Sheet", "text/tab-separated-values", ".tsv", false),
					format("Web Page", "application/zip", ".zip", false)),

			GOOGLE_PRESENTATION,
			List.of(format("Microsoft PowerPoint",
					"application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx", true),
					format("PDF", "application/pdf", ".pdf", false),
					format("OpenDocument Presentation", "application/vnd.oasis.opendocument.presentation", ".odp",
							false),
					format("Plain Text", "text/plain", ".txt", false),
					format("JPEG - First Slide", "image/jpeg", ".jpg", false),
					format("PNG - First Slide", "image/png", ".png", false),
					format("SVG - First Slide", "image/svg+xml", ".svg", false)),

			GOOGLE_DRAWING,
			List.of(format("PDF", "application/pdf", ".pdf", true), format("JPEG", "image/jpeg", ".jpg", false),
					format("PNG", "image/png", ".png", false), format("SVG", "image/svg+xml", ".svg", false)));

	public boolean isExportable(String sourceMimeType) {

		return sourceMimeType != null && EXPORT_FORMATS.containsKey(sourceMimeType);
	}

	public List<DriveExportFormatResponse> getFormats(String sourceMimeType) {

		List<DriveExportFormatResponse> formats = EXPORT_FORMATS.get(sourceMimeType);

		if (formats == null) {

			return List.of();
		}

		return formats;
	}

	public DriveExportFormatResponse resolve(String sourceMimeType, String requestedMimeType) {

		List<DriveExportFormatResponse> formats = EXPORT_FORMATS.get(sourceMimeType);

		if (formats == null || formats.isEmpty()) {

			throw new DriveContentNotSupportedException(
					"This Google Drive file type cannot be exported by this endpoint");
		}

		if (requestedMimeType == null || requestedMimeType.isBlank()) {

			return formats.stream()
				.filter(DriveExportFormatResponse::defaultFormat)
				.findFirst()
				.orElse(formats.getFirst());
		}

		return formats.stream()
			.filter(format -> format.mimeType().equalsIgnoreCase(requestedMimeType.trim()))
			.findFirst()
			.orElseThrow(() -> new DriveContentNotSupportedException(
					"Requested export MIME type is not supported for this Google Workspace file"));
	}

	public boolean isGoogleNativeMimeType(String mimeType) {

		return mimeType != null && mimeType.startsWith("application/vnd.google-apps.");
	}

	private static DriveExportFormatResponse format(String label, String mimeType, String extension,
			boolean defaultFormat) {

		return new DriveExportFormatResponse(label, mimeType, extension, defaultFormat);
	}

}
