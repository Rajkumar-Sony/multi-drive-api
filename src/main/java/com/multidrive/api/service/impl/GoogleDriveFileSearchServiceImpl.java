package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.service.GoogleDriveFileSearchService;
import com.multidrive.api.service.GoogleTokenService;
import com.multidrive.api.util.GoogleDriveFieldMasks;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleDriveFileSearchServiceImpl implements GoogleDriveFileSearchService {

	private static final String GOOGLE_DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files";

	private static final int SEARCH_PAGE_SIZE = 100;

	private final GoogleTokenService googleTokenService;

	private RestClient restClient;

	public GoogleDriveFileSearchServiceImpl(GoogleTokenService googleTokenService) {

		this.googleTokenService = googleTokenService;

		this.restClient = RestClient.create();
	}

	@Override
	public List<GoogleDriveFileResponse> findByAppProperty(Long connectionId, Long userId,
			GoogleDriveSourceType sourceType, String googleDriveId, String propertyKey, String propertyValue) {

		validate(connectionId, userId, sourceType, googleDriveId, propertyKey, propertyValue);

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		String query = "appProperties has { key='" + escapeQueryLiteral(propertyKey) + "' and value='"
				+ escapeQueryLiteral(propertyValue) + "' } and trashed = false";

		List<GoogleDriveFileResponse> matches = new ArrayList<>();

		String pageToken = null;

		do {

			GoogleDriveFilesResponse response = fetchPage(accessToken, sourceType, googleDriveId, query, pageToken);

			if (Boolean.TRUE.equals(response.incompleteSearch())) {

				throw new IllegalStateException("Google Drive operation-marker search returned incomplete results");
			}

			if (response.files() != null) {

				matches.addAll(response.files());
			}

			if (matches.size() > 1) {

				return List.copyOf(matches);
			}

			pageToken = response.nextPageToken();

		}
		while (pageToken != null && !pageToken.isBlank());

		return List.copyOf(matches);
	}

	private GoogleDriveFilesResponse fetchPage(String accessToken, GoogleDriveSourceType sourceType,
			String googleDriveId, String query, String pageToken) {

		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(GOOGLE_DRIVE_FILES_URL)
			.queryParam("q", query)
			.queryParam("spaces", "drive")
			.queryParam("pageSize", SEARCH_PAGE_SIZE)
			.queryParam("supportsAllDrives", true)
			.queryParam("includeItemsFromAllDrives", true)
			.queryParam("fields", GoogleDriveFieldMasks.FILE_LIST);

		if (sourceType == GoogleDriveSourceType.SHARED_DRIVE) {

			builder.queryParam("corpora", "drive").queryParam("driveId", googleDriveId);

		}
		else {

			builder.queryParam("corpora", "user");
		}

		if (pageToken != null && !pageToken.isBlank()) {

			builder.queryParam("pageToken", pageToken);
		}

		URI uri = builder.build().encode().toUri();

		GoogleDriveFilesResponse response = restClient.get()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.retrieve()
			.body(GoogleDriveFilesResponse.class);

		if (response == null) {

			throw new IllegalStateException("Google Drive returned an empty operation-marker search response");
		}

		return response;
	}

	private String escapeQueryLiteral(String value) {

		return value.replace("\\", "\\\\").replace("'", "\\'");
	}

	private void validate(Long connectionId, Long userId, GoogleDriveSourceType sourceType, String googleDriveId,
			String propertyKey, String propertyValue) {

		if (connectionId == null) {

			throw new IllegalArgumentException("connectionId is required");
		}

		if (userId == null) {

			throw new IllegalArgumentException("userId is required");
		}

		if (sourceType == null) {

			throw new IllegalArgumentException("sourceType is required");
		}

		if (sourceType == GoogleDriveSourceType.SHARED_DRIVE && (googleDriveId == null || googleDriveId.isBlank())) {

			throw new IllegalArgumentException("googleDriveId is required for Shared Drive search");
		}

		if (propertyKey == null || propertyKey.isBlank()) {

			throw new IllegalArgumentException("propertyKey is required");
		}

		if (propertyValue == null || propertyValue.isBlank()) {

			throw new IllegalArgumentException("propertyValue is required");
		}
	}

}
