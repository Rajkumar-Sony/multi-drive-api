package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileMutationRequest;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.service.GoogleDriveFileMutationService;
import com.multidrive.api.service.GoogleTokenService;
import com.multidrive.api.util.GoogleDriveFieldMasks;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GoogleDriveFileMutationServiceImpl implements GoogleDriveFileMutationService {

	private static final String GOOGLE_DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

	private static final String GOOGLE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

	private final GoogleTokenService googleTokenService;

	private final RestClient restClient;

	public GoogleDriveFileMutationServiceImpl(GoogleTokenService googleTokenService) {

		this.googleTokenService = googleTokenService;

		this.restClient = RestClient.create();
	}

	@Override
	public GoogleDriveFileResponse getFile(Long connectionId, Long userId, String googleFileId) {

		validateIdentifiers(connectionId, userId, googleFileId);

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		GoogleDriveFileResponse response = restClient.get()
			.uri(buildFileUri(googleFileId))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.retrieve()
			.body(GoogleDriveFileResponse.class);

		return requireFileResponse(response, "Google Drive returned an empty file response");
	}

	@Override
	public GoogleDriveFileResponse createFolder(Long connectionId, Long userId, String parentGoogleFileId,
			String name) {

		return createFolderWithAppProperties(connectionId, userId, parentGoogleFileId, name, null);
	}

	@Override
	public GoogleDriveFileResponse createFolderWithAppProperties(Long connectionId, Long userId,
			String parentGoogleFileId, String name, Map<String, String> appProperties) {

		validateIdentifiers(connectionId, userId, parentGoogleFileId);

		String normalizedName = normalizeName(name);

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		GoogleDriveFileMutationRequest request = new GoogleDriveFileMutationRequest(normalizedName,
				GOOGLE_FOLDER_MIME_TYPE, List.of(parentGoogleFileId), null, normalizeAppProperties(appProperties));

		URI uri = UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_FILES_URL)
			.queryParam("supportsAllDrives", true)
			.queryParam("fields", GoogleDriveFieldMasks.FILE_RESOURCE)
			.build()
			.encode()
			.toUri();

		GoogleDriveFileResponse response = restClient.post()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.body(request)
			.retrieve()
			.body(GoogleDriveFileResponse.class);

		return requireFileResponse(response, "Google Drive returned an empty folder creation response");
	}

	@Override
	public GoogleDriveFileResponse rename(Long connectionId, Long userId, String googleFileId, String name) {

		validateIdentifiers(connectionId, userId, googleFileId);

		GoogleDriveFileMutationRequest request = new GoogleDriveFileMutationRequest(normalizeName(name), null, null,
				null);

		return patchFile(connectionId, userId, googleFileId, request, "Google Drive returned an empty rename response");
	}

	@Override
	public GoogleDriveFileResponse move(Long connectionId, Long userId, String googleFileId,
			String destinationParentGoogleFileId, List<String> currentParentGoogleFileIds) {

		validateIdentifiers(connectionId, userId, googleFileId);

		if (destinationParentGoogleFileId == null || destinationParentGoogleFileId.isBlank()) {

			throw new IllegalArgumentException("destinationParentGoogleFileId is required");
		}

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		UriComponentsBuilder uriBuilder = UriComponentsBuilder
			.fromUriString(GOOGLE_DRIVE_FILES_URL + "/" + googleFileId)
			.queryParam("addParents", destinationParentGoogleFileId)
			.queryParam("supportsAllDrives", true)
			.queryParam("fields", GoogleDriveFieldMasks.FILE_RESOURCE);

		String removeParents = buildRemoveParents(currentParentGoogleFileIds, destinationParentGoogleFileId);

		if (!removeParents.isBlank()) {

			uriBuilder.queryParam("removeParents", removeParents);
		}

		GoogleDriveFileMutationRequest emptyRequest = new GoogleDriveFileMutationRequest(null, null, null, null);

		GoogleDriveFileResponse response = restClient.patch()
			.uri(uriBuilder.build().encode().toUri())
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.body(emptyRequest)
			.retrieve()
			.body(GoogleDriveFileResponse.class);

		return requireFileResponse(response, "Google Drive returned an empty move response");
	}

	@Override
	public GoogleDriveFileResponse copy(Long connectionId, Long userId, String googleFileId,
			String destinationParentGoogleFileId, String name) {

		return copyWithAppProperties(connectionId, userId, googleFileId, destinationParentGoogleFileId, name, null);
	}

	@Override
	public GoogleDriveFileResponse copyWithAppProperties(Long connectionId, Long userId, String googleFileId,
			String destinationParentGoogleFileId, String name, Map<String, String> appProperties) {

		validateIdentifiers(connectionId, userId, googleFileId);

		if (destinationParentGoogleFileId == null || destinationParentGoogleFileId.isBlank()) {

			throw new IllegalArgumentException("destinationParentGoogleFileId is required");
		}

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		GoogleDriveFileMutationRequest request = new GoogleDriveFileMutationRequest(normalizeOptionalName(name), null,
				List.of(destinationParentGoogleFileId), null, normalizeAppProperties(appProperties));

		URI uri = UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_FILES_URL + "/" + googleFileId + "/copy")
			.queryParam("supportsAllDrives", true)
			.queryParam("fields", GoogleDriveFieldMasks.FILE_RESOURCE)
			.build()
			.encode()
			.toUri();

		GoogleDriveFileResponse response = restClient.post()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.body(request)
			.retrieve()
			.body(GoogleDriveFileResponse.class);

		return requireFileResponse(response, "Google Drive returned an empty copy response");
	}

	@Override
	public GoogleDriveFileResponse trash(Long connectionId, Long userId, String googleFileId) {

		validateIdentifiers(connectionId, userId, googleFileId);

		GoogleDriveFileMutationRequest request = new GoogleDriveFileMutationRequest(null, null, null, true);

		return patchFile(connectionId, userId, googleFileId, request, "Google Drive returned an empty trash response");
	}

	@Override
	public GoogleDriveFileResponse restore(Long connectionId, Long userId, String googleFileId) {

		validateIdentifiers(connectionId, userId, googleFileId);

		GoogleDriveFileMutationRequest request = new GoogleDriveFileMutationRequest(null, null, null, false);

		return patchFile(connectionId, userId, googleFileId, request,
				"Google Drive returned an empty restore response");
	}

	@Override
	public void permanentlyDelete(Long connectionId, Long userId, String googleFileId) {

		validateIdentifiers(connectionId, userId, googleFileId);

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		URI uri = UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_FILES_URL + "/" + googleFileId)
			.queryParam("supportsAllDrives", true)
			.build()
			.encode()
			.toUri();

		restClient.delete()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.retrieve()
			.toBodilessEntity();
	}

	private GoogleDriveFileResponse patchFile(Long connectionId, Long userId, String googleFileId,
			GoogleDriveFileMutationRequest request, String emptyResponseMessage) {

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		GoogleDriveFileResponse response = restClient.patch()
			.uri(buildFileUri(googleFileId))
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.body(request)
			.retrieve()
			.body(GoogleDriveFileResponse.class);

		return requireFileResponse(response, emptyResponseMessage);
	}

	private Map<String, String> normalizeAppProperties(Map<String, String> appProperties) {

		if (appProperties == null || appProperties.isEmpty()) {

			return null;
		}

		return Map.copyOf(appProperties);
	}

	private String buildRemoveParents(List<String> currentParents, String destinationParent) {

		if (currentParents == null || currentParents.isEmpty()) {

			return "";
		}

		return currentParents.stream()
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(parent -> !parent.isBlank())
			.filter(parent -> !parent.equals(destinationParent))
			.distinct()
			.reduce("", (left, right) -> left.isBlank() ? right : left + "," + right);
	}

	private URI buildFileUri(String googleFileId) {

		return UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_FILES_URL + "/" + googleFileId)
			.queryParam("supportsAllDrives", true)
			.queryParam("fields", GoogleDriveFieldMasks.FILE_RESOURCE)
			.build()
			.encode()
			.toUri();
	}

	private GoogleDriveFileResponse requireFileResponse(GoogleDriveFileResponse response, String message) {

		if (response == null || response.id() == null || response.id().isBlank()) {

			throw new IllegalStateException(message);
		}

		return response;
	}

	private String normalizeName(String name) {

		if (name == null) {

			throw new IllegalArgumentException("name is required");
		}

		String normalized = name.trim();

		if (normalized.isEmpty()) {

			throw new IllegalArgumentException("name must not be blank");
		}

		if (normalized.length() > 255) {

			throw new IllegalArgumentException("name must not exceed 255 characters");
		}

		return normalized;
	}

	private String normalizeOptionalName(String name) {

		if (name == null) {
			return null;
		}

		String normalized = name.trim();

		if (normalized.isEmpty()) {
			return null;
		}

		if (normalized.length() > 255) {

			throw new IllegalArgumentException("name must not exceed 255 characters");
		}

		return normalized;
	}

	private void validateIdentifiers(Long connectionId, Long userId, String googleFileId) {

		if (connectionId == null) {

			throw new IllegalArgumentException("connectionId is required");
		}

		if (userId == null) {

			throw new IllegalArgumentException("userId is required");
		}

		if (googleFileId == null || googleFileId.isBlank()) {

			throw new IllegalArgumentException("googleFileId is required");
		}
	}

}
