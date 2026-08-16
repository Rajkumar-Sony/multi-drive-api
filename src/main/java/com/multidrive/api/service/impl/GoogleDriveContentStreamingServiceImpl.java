package com.multidrive.api.service.impl;

import com.multidrive.api.service.GoogleDriveContentStreamingService;
import com.multidrive.api.service.GoogleTokenService;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

@Service
public class GoogleDriveContentStreamingServiceImpl implements GoogleDriveContentStreamingService {

	private static final String GOOGLE_DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

	private final GoogleTokenService googleTokenService;

	private final RestClient restClient;

	public GoogleDriveContentStreamingServiceImpl(GoogleTokenService googleTokenService) {

		this.googleTokenService = googleTokenService;

		this.restClient = RestClient.create();
	}

	@Override
	public void streamBlob(Long connectionId, Long userId, String googleFileId, OutputStream outputStream)
			throws IOException {

		validate(connectionId, userId, googleFileId, outputStream);

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		URI uri = UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_FILES_URL + "/" + googleFileId)
			.queryParam("alt", "media")
			.queryParam("supportsAllDrives", true)
			.build()
			.encode()
			.toUri();

		streamGoogleResponse(uri, accessToken, outputStream, "Google Drive file download");
	}

	@Override
	public void streamExport(Long connectionId, Long userId, String googleFileId, String exportMimeType,
			OutputStream outputStream) throws IOException {

		validate(connectionId, userId, googleFileId, outputStream);

		if (exportMimeType == null || exportMimeType.isBlank()) {

			throw new IllegalArgumentException("exportMimeType is required");
		}

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		URI uri = UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_FILES_URL + "/" + googleFileId + "/export")
			.queryParam("mimeType", exportMimeType)
			.build()
			.encode()
			.toUri();

		streamGoogleResponse(uri, accessToken, outputStream, "Google Drive file export");
	}

	private void streamGoogleResponse(URI uri, String accessToken, OutputStream outputStream, String operation)
			throws IOException {

		restClient.get()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.header(HttpHeaders.ACCEPT, "*/*")
			.exchange((request, response) -> {

				if (!response.getStatusCode().is2xxSuccessful()) {

					throw new IllegalStateException(
							operation + " failed with HTTP status " + response.getStatusCode().value());
				}

				try (InputStream inputStream = response.getBody()) {

					byte[] buffer = new byte[64 * 1024];

					int bytesRead;

					while ((bytesRead = inputStream.read(buffer)) != -1) {

						outputStream.write(buffer, 0, bytesRead);
					}

					outputStream.flush();

				}

				return null;
			});
	}

	private void validate(Long connectionId, Long userId, String googleFileId, OutputStream outputStream) {

		if (connectionId == null) {

			throw new IllegalArgumentException("connectionId is required");
		}

		if (userId == null) {

			throw new IllegalArgumentException("userId is required");
		}

		if (googleFileId == null || googleFileId.isBlank()) {

			throw new IllegalArgumentException("googleFileId is required");
		}

		if (outputStream == null) {

			throw new IllegalArgumentException("outputStream is required");
		}
	}

}
