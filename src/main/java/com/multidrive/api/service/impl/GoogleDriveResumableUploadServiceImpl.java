package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.dto.GoogleDriveUploadMetadataRequest;
import com.multidrive.api.service.GoogleDriveResumableUploadService;
import com.multidrive.api.service.GoogleTokenService;
import com.multidrive.api.service.InputStreamProvider;
import com.multidrive.api.util.GoogleDriveFieldMasks;

import tools.jackson.databind.ObjectMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

@Service
public class GoogleDriveResumableUploadServiceImpl implements GoogleDriveResumableUploadService {

	private static final String GOOGLE_DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files";

	private static final int UPLOAD_CHUNK_SIZE = 8 * 1024 * 1024;

	private static final int HTTP_RESUME_INCOMPLETE = 308;

	private final GoogleTokenService googleTokenService;

	private final ObjectMapper objectMapper;

	private final RestClient restClient;

	public GoogleDriveResumableUploadServiceImpl(GoogleTokenService googleTokenService, ObjectMapper objectMapper) {

		this.googleTokenService = googleTokenService;

		this.objectMapper = objectMapper;

		this.restClient = RestClient.create();
	}

	@Override
	public GoogleDriveFileResponse upload(Long connectionId, Long userId, String parentGoogleFileId, String fileName,
			String contentType, long contentLength, InputStreamProvider inputStreamProvider) {

		validateRequest(connectionId, userId, parentGoogleFileId, fileName, contentLength, inputStreamProvider);

		String normalizedContentType = normalizeContentType(contentType);

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		URI sessionUri = initiateUploadSession(accessToken, parentGoogleFileId, fileName, normalizedContentType,
				contentLength);

		if (contentLength == 0) {

			return uploadEmptyFile(sessionUri, normalizedContentType);
		}

		return uploadContent(sessionUri, normalizedContentType, contentLength, inputStreamProvider);
	}

	private URI initiateUploadSession(String accessToken, String parentGoogleFileId, String fileName,
			String contentType, long contentLength) {

		GoogleDriveUploadMetadataRequest metadata = new GoogleDriveUploadMetadataRequest(fileName, contentType,
				List.of(parentGoogleFileId));

		URI uri = UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_UPLOAD_URL)
			.queryParam("uploadType", "resumable")
			.queryParam("supportsAllDrives", true)
			.queryParam("fields", GoogleDriveFieldMasks.FILE_RESOURCE)
			.build()
			.encode()
			.toUri();

		ResponseEntity<Void> response = restClient.post()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.header("X-Upload-Content-Type", contentType)
			.header("X-Upload-Content-Length", Long.toString(contentLength))
			.contentType(MediaType.APPLICATION_JSON)
			.body(metadata)
			.retrieve()
			.toBodilessEntity();

		URI location = response.getHeaders().getLocation();

		if (location == null) {

			throw new IllegalStateException("Google Drive did not return a resumable upload session URI");
		}

		return location;
	}

	private GoogleDriveFileResponse uploadContent(URI sessionUri, String contentType, long totalLength,
			InputStreamProvider inputStreamProvider) {

		byte[] buffer = new byte[UPLOAD_CHUNK_SIZE];

		long startOffset = 0;

		try (InputStream inputStream = inputStreamProvider.openStream()) {

			while (startOffset < totalLength) {

				int bytesRead = readChunk(inputStream, buffer);

				if (bytesRead <= 0) {

					throw new IllegalStateException("Upload stream ended before the expected content length");
				}

				byte[] chunk = bytesRead == buffer.length ? buffer.clone() : Arrays.copyOf(buffer, bytesRead);

				long endOffset = startOffset + bytesRead - 1;

				UploadChunkResult result = uploadChunk(sessionUri, contentType, chunk, startOffset, endOffset,
						totalLength);

				if (result.completed()) {

					if (result.file() == null) {

						throw new IllegalStateException(
								"Google Drive completed the upload but returned no file metadata");
					}

					return result.file();
				}

				if (result.acknowledgedEndOffset() != endOffset) {

					throw new IllegalStateException("Google Drive did not acknowledge the complete upload chunk");
				}

				startOffset = endOffset + 1;
			}

		}
		catch (IOException exception) {

			throw new IllegalStateException("Unable to read file content for Google Drive upload", exception);
		}

		throw new IllegalStateException("Google Drive resumable upload did not complete");
	}

	private UploadChunkResult uploadChunk(URI sessionUri, String contentType, byte[] chunk, long startOffset,
			long endOffset, long totalLength) {

		MediaType mediaType = parseMediaType(contentType);

		return restClient.put()
			.uri(sessionUri)
			.contentType(mediaType)
			.contentLength(chunk.length)
			.header(HttpHeaders.CONTENT_RANGE, "bytes " + startOffset + "-" + endOffset + "/" + totalLength)
			.body(chunk)
			.exchange((request, response) -> {

				int status = response.getStatusCode().value();

				if (status == HTTP_RESUME_INCOMPLETE) {

					String range = response.getHeaders().getFirst(HttpHeaders.RANGE);

					long acknowledgedEndOffset = parseAcknowledgedEndOffset(range);

					return new UploadChunkResult(false, acknowledgedEndOffset, null);
				}

				if (response.getStatusCode().is2xxSuccessful()) {

					try {

						GoogleDriveFileResponse file = objectMapper.readValue(response.getBody(),
								GoogleDriveFileResponse.class);

						return new UploadChunkResult(true, endOffset, file);

					}
					catch (IOException exception) {

						throw new IllegalStateException("Unable to parse Google Drive upload response", exception);
					}
				}

				throw new IllegalStateException("Google Drive upload chunk failed with HTTP status " + status);
			});
	}

	private GoogleDriveFileResponse uploadEmptyFile(URI sessionUri, String contentType) {

		MediaType mediaType = parseMediaType(contentType);

		return restClient.put()
			.uri(sessionUri)
			.contentType(mediaType)
			.contentLength(0)
			.body(new byte[0])
			.exchange((request, response) -> {

				if (!response.getStatusCode().is2xxSuccessful()) {

					throw new IllegalStateException("Google Drive empty-file upload failed with HTTP status "
							+ response.getStatusCode().value());
				}

				try {

					GoogleDriveFileResponse file = objectMapper.readValue(response.getBody(),
							GoogleDriveFileResponse.class);

					if (file == null || file.id() == null || file.id().isBlank()) {

						throw new IllegalStateException(
								"Google Drive returned incomplete metadata for the uploaded file");
					}

					return file;

				}
				catch (IOException exception) {

					throw new IllegalStateException("Unable to parse Google Drive upload response", exception);
				}
			});
	}

	private int readChunk(InputStream inputStream, byte[] buffer) throws IOException {

		int offset = 0;

		while (offset < buffer.length) {

			int read = inputStream.read(buffer, offset, buffer.length - offset);

			if (read < 0) {
				break;
			}

			if (read == 0) {
				continue;
			}

			offset += read;
		}

		return offset;
	}

	private long parseAcknowledgedEndOffset(String range) {

		if (range == null || range.isBlank()) {

			return -1;
		}

		int dashIndex = range.lastIndexOf('-');

		if (dashIndex < 0 || dashIndex == range.length() - 1) {

			return -1;
		}

		try {

			return Long.parseLong(range.substring(dashIndex + 1));

		}
		catch (NumberFormatException exception) {

			return -1;
		}
	}

	private String normalizeContentType(String contentType) {

		if (contentType == null || contentType.isBlank()) {

			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}

		try {

			MediaType.parseMediaType(contentType);

			return contentType;

		}
		catch (Exception exception) {

			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}
	}

	private MediaType parseMediaType(String contentType) {

		try {

			return MediaType.parseMediaType(contentType);

		}
		catch (Exception exception) {

			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}

	private void validateRequest(Long connectionId, Long userId, String parentGoogleFileId, String fileName,
			long contentLength, InputStreamProvider inputStreamProvider) {

		if (connectionId == null) {

			throw new IllegalArgumentException("connectionId is required");
		}

		if (userId == null) {

			throw new IllegalArgumentException("userId is required");
		}

		if (parentGoogleFileId == null || parentGoogleFileId.isBlank()) {

			throw new IllegalArgumentException("parentGoogleFileId is required");
		}

		if (fileName == null || fileName.isBlank()) {

			throw new IllegalArgumentException("fileName is required");
		}

		if (contentLength < 0) {

			throw new IllegalArgumentException("contentLength must be 0 or greater");
		}

		if (inputStreamProvider == null) {

			throw new IllegalArgumentException("inputStreamProvider is required");
		}
	}

	private record UploadChunkResult(

			boolean completed,

			long acknowledgedEndOffset,

			GoogleDriveFileResponse file) {
	}

}
