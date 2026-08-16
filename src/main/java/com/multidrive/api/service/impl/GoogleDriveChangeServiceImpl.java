package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleSharedDriveResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.dto.GoogleStartPageTokenResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.repository.GoogleDriveChangeTrackerRepository;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.GoogleDriveChangeService;
import com.multidrive.api.service.GoogleDriveService;
import com.multidrive.api.service.GoogleTokenService;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleDriveChangeServiceImpl implements GoogleDriveChangeService {

	private static final String GOOGLE_START_PAGE_TOKEN_URL = "https://www.googleapis.com/drive/v3/changes/startPageToken";

	private static final String TRACKER_STATUS_ACTIVE = "ACTIVE";

	private static final int SHARED_DRIVE_PAGE_SIZE = 100;

	private final GoogleDriveChangeTrackerRepository googleDriveChangeTrackerRepository;

	private final GoogleDriveConnectionRepository googleDriveConnectionRepository;

	private final GoogleTokenService googleTokenService;

	private final GoogleDriveService googleDriveService;

	private final RestClient restClient;

	public GoogleDriveChangeServiceImpl(GoogleDriveChangeTrackerRepository googleDriveChangeTrackerRepository,

			GoogleDriveConnectionRepository googleDriveConnectionRepository,

			GoogleTokenService googleTokenService,

			GoogleDriveService googleDriveService) {

		this.googleDriveChangeTrackerRepository = googleDriveChangeTrackerRepository;

		this.googleDriveConnectionRepository = googleDriveConnectionRepository;

		this.googleTokenService = googleTokenService;

		this.googleDriveService = googleDriveService;

		this.restClient = RestClient.create();
	}

	@Override
	public GoogleDriveChangeTracker initializeUserTracker(Long connectionId, Long userId) {

		validateIds(connectionId, userId);

		GoogleDriveConnection connection = findConnection(connectionId, userId);

		Optional<GoogleDriveChangeTracker> existingTracker = googleDriveChangeTrackerRepository
			.findByConnection_IdAndTrackerTypeAndDriveIdIsNull(connectionId, GoogleDriveTrackerType.USER);

		/*
		 * Do not replace an existing page token.
		 *
		 * That token represents our current sync position. Replacing it could skip
		 * unprocessed Drive changes.
		 */
		if (existingTracker.isPresent() && hasPageToken(existingTracker.get())) {

			return existingTracker.get();
		}

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		String startPageToken = fetchStartPageToken(accessToken, null);

		GoogleDriveChangeTracker tracker = existingTracker.orElseGet(GoogleDriveChangeTracker::new);

		tracker.setConnection(connection);

		tracker.setTrackerType(GoogleDriveTrackerType.USER);

		tracker.setDriveId(null);

		tracker.setPageToken(startPageToken);

		tracker.setStatus(TRACKER_STATUS_ACTIVE);

		return googleDriveChangeTrackerRepository.save(tracker);
	}

	@Override
	public List<GoogleDriveChangeTracker> initializeSharedDriveTrackers(Long connectionId, Long userId) {

		validateIds(connectionId, userId);

		GoogleDriveConnection connection = findConnection(connectionId, userId);

		/*
		 * Get one valid access token that will be used for startPageToken calls for
		 * Shared Drives.
		 */
		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		List<GoogleDriveChangeTracker> trackers = new ArrayList<>();

		String nextPageToken = null;

		do {

			/*
			 * Google Shared Drives are paginated.
			 *
			 * We request the maximum supported page size and continue until nextPageToken
			 * is absent.
			 */
			GoogleSharedDrivesResponse response = googleDriveService.getSharedDrives(connectionId, userId,
					SHARED_DRIVE_PAGE_SIZE, nextPageToken);

			if (response.drives() != null) {

				for (GoogleSharedDriveResponse sharedDrive : response.drives()) {

					if (sharedDrive == null || sharedDrive.id() == null || sharedDrive.id().isBlank()) {

						continue;
					}

					GoogleDriveChangeTracker tracker = initializeSharedDriveTracker(connection, accessToken,
							sharedDrive.id());

					trackers.add(tracker);
				}
			}

			nextPageToken = normalizePageToken(response.nextPageToken());

		}
		while (nextPageToken != null);

		return trackers;
	}

	private GoogleDriveChangeTracker initializeSharedDriveTracker(GoogleDriveConnection connection, String accessToken,
			String driveId) {

		Optional<GoogleDriveChangeTracker> existingTracker = googleDriveChangeTrackerRepository
			.findByConnection_IdAndTrackerTypeAndDriveId(connection.getId(), GoogleDriveTrackerType.SHARED_DRIVE,
					driveId);

		/*
		 * Existing tracker with a valid page token must keep its current sync position.
		 */
		if (existingTracker.isPresent() && hasPageToken(existingTracker.get())) {

			return existingTracker.get();
		}

		String startPageToken = fetchStartPageToken(accessToken, driveId);

		GoogleDriveChangeTracker tracker = existingTracker.orElseGet(GoogleDriveChangeTracker::new);

		tracker.setConnection(connection);

		tracker.setTrackerType(GoogleDriveTrackerType.SHARED_DRIVE);

		tracker.setDriveId(driveId);

		tracker.setPageToken(startPageToken);

		tracker.setStatus(TRACKER_STATUS_ACTIVE);

		return googleDriveChangeTrackerRepository.save(tracker);
	}

	private String fetchStartPageToken(String accessToken, String driveId) {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(GOOGLE_START_PAGE_TOKEN_URL)
			.queryParam("supportsAllDrives", true);

		/*
		 * driveId is only added for Shared Drive trackers.
		 *
		 * When driveId is null, Google returns the starting token for the user's change
		 * log.
		 */
		if (driveId != null && !driveId.isBlank()) {

			uriBuilder.queryParam("driveId", driveId);
		}

		URI uri = uriBuilder.build().encode().toUri();

		GoogleStartPageTokenResponse response = restClient.get()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.retrieve()
			.body(GoogleStartPageTokenResponse.class);

		if (response == null) {

			throw new IllegalStateException("Google Drive returned an empty start page token response");
		}

		if (response.startPageToken() == null || response.startPageToken().isBlank()) {

			throw new IllegalStateException("Google Drive did not return a start page token");
		}

		return response.startPageToken();
	}

	private GoogleDriveConnection findConnection(Long connectionId, Long userId) {

		return googleDriveConnectionRepository.findByIdAndUserId(connectionId, userId)
			.orElseThrow(() -> new IllegalArgumentException("Google Drive connection not found"));
	}

	private boolean hasPageToken(GoogleDriveChangeTracker tracker) {

		return tracker.getPageToken() != null && !tracker.getPageToken().isBlank();
	}

	private String normalizePageToken(String pageToken) {

		if (pageToken == null || pageToken.isBlank()) {

			return null;
		}

		return pageToken;
	}

	private void validateIds(Long connectionId, Long userId) {

		if (connectionId == null) {

			throw new IllegalArgumentException("connectionId is required");
		}

		if (userId == null) {

			throw new IllegalArgumentException("userId is required");
		}
	}

}