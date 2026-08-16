package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveContentStreamResponse;
import com.multidrive.api.dto.DriveExportOptionsResponse;
import com.multidrive.api.service.DriveDownloadService;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@RestController
@RequestMapping("/api/drive/items")
public class DriveContentController {

	private final DriveDownloadService driveDownloadService;

	public DriveContentController(DriveDownloadService driveDownloadService) {

		this.driveDownloadService = driveDownloadService;
	}

	@GetMapping("/{itemId}/download")
	public ResponseEntity<StreamingResponseBody> download(

			@PathVariable Long itemId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		DriveContentStreamResponse response = driveDownloadService.prepareDownload(requireGoogleSubjectId(oidcUser),
				itemId);

		return buildStreamingResponse(response);
	}

	@GetMapping("/{itemId}/export")
	public ResponseEntity<StreamingResponseBody> export(

			@PathVariable Long itemId,

			@RequestParam(required = false) String mimeType,

			@AuthenticationPrincipal OidcUser oidcUser) {

		DriveContentStreamResponse response = driveDownloadService.prepareExport(requireGoogleSubjectId(oidcUser),
				itemId, mimeType);

		return buildStreamingResponse(response);
	}

	@GetMapping("/{itemId}/export-formats")
	public DriveExportOptionsResponse exportFormats(

			@PathVariable Long itemId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveDownloadService.getExportOptions(requireGoogleSubjectId(oidcUser), itemId);
	}

	private ResponseEntity<StreamingResponseBody> buildStreamingResponse(DriveContentStreamResponse response) {

		MediaType contentType = parseMediaType(response.contentType());

		ContentDisposition contentDisposition = ContentDisposition.attachment()
			.filename(response.fileName(), StandardCharsets.UTF_8)
			.build();

		StreamingResponseBody streamingResponseBody = outputStream -> response.writer().writeTo(outputStream);

		ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
			.contentType(contentType)
			.cacheControl(CacheControl.maxAge(Duration.ZERO).cachePrivate().mustRevalidate())
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
			.header("X-Content-Type-Options", "nosniff");

		if (response.contentLength() != null && response.contentLength() >= 0) {

			builder.contentLength(response.contentLength());
		}

		return builder.body(streamingResponseBody);
	}

	private MediaType parseMediaType(String mimeType) {

		if (mimeType == null || mimeType.isBlank()) {

			return MediaType.APPLICATION_OCTET_STREAM;
		}

		try {

			return MediaType.parseMediaType(mimeType);

		}
		catch (Exception exception) {

			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	private String requireGoogleSubjectId(OidcUser oidcUser) {

		if (oidcUser == null || oidcUser.getSubject() == null || oidcUser.getSubject().isBlank()) {

			throw new IllegalStateException("Authenticated application user not found");
		}

		return oidcUser.getSubject();
	}

}
