package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveWatchRequest;
import com.multidrive.api.dto.GoogleDriveWatchResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.entity.GoogleDriveWatchChannel;
import com.multidrive.api.repository.GoogleDriveChangeTrackerRepository;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.repository.GoogleDriveWatchChannelRepository;
import com.multidrive.api.service.GoogleDriveWatchService;
import com.multidrive.api.service.GoogleTokenService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GoogleDriveWatchServiceImpl implements GoogleDriveWatchService {

	private static final String GOOGLE_CHANGES_WATCH_URL = "https://www.googleapis.com/drive/v3/changes/watch";

	private static final String TRACKER_STATUS_ACTIVE = "ACTIVE";

	private static final String CHANNEL_STATUS_ACTIVE = "ACTIVE";

	private static final String CHANNEL_STATUS_EXPIRED = "EXPIRED";

	/*
	 * Google currently allows changes.watch channels for a maximum of 7 days.
	 *
	 * We request 6 days and later renew them before expiry.
	 */
	private static final Duration WATCH_CHANNEL_LIFETIME = Duration.ofDays(6);

	/*
	 * If a channel still has more than 12 hours remaining, we reuse it instead of
	 * creating another channel.
	 */
	private static final Duration REUSE_THRESHOLD = Duration.ofHours(12);

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final GoogleDriveChangeTrackerRepository googleDriveChangeTrackerRepository;

	private final GoogleDriveWatchChannelRepository googleDriveWatchChannelRepository;

	private final GoogleDriveConnectionRepository googleDriveConnectionRepository;

	private final GoogleTokenService googleTokenService;

	private final RestClient restClient;

	private final String webhookUrl;

	public GoogleDriveWatchServiceImpl(GoogleDriveChangeTrackerRepository googleDriveChangeTrackerRepository,

			GoogleDriveWatchChannelRepository googleDriveWatchChannelRepository,

			GoogleDriveConnectionRepository googleDriveConnectionRepository,

			GoogleTokenService googleTokenService,

			@Value("${google.drive.webhook-url:}") String webhookUrl) {

		this.googleDriveChangeTrackerRepository = googleDriveChangeTrackerRepository;

		this.googleDriveWatchChannelRepository = googleDriveWatchChannelRepository;

		this.googleDriveConnectionRepository = googleDriveConnectionRepository;

		this.googleTokenService = googleTokenService;

		this.webhookUrl = webhookUrl;

		this.restClient = RestClient.create();
	}

	@Override
	public List<GoogleDriveWatchChannel> registerWatchChannels(Long connectionId, Long userId) {

		validateIds(connectionId, userId);

		validateWebhookUrl();

		/*
		 * Security check: connection must belong to this application user.
		 */
		googleDriveConnectionRepository.findByIdAndUserId(connectionId, userId)
			.orElseThrow(() -> new IllegalArgumentException("Google Drive connection not found"));

		List<GoogleDriveChangeTracker> trackers = googleDriveChangeTrackerRepository
			.findAllByConnection_IdAndStatus(connectionId, TRACKER_STATUS_ACTIVE);

		if (trackers.isEmpty()) {

			throw new IllegalStateException("No active Google Drive change trackers found");
		}

		String accessToken = googleTokenService.getValidAccessToken(connectionId, userId);

		List<GoogleDriveWatchChannel> channels = new ArrayList<>();

		for (GoogleDriveChangeTracker tracker : trackers) {

			GoogleDriveWatchChannel channel = registerWatchChannel(tracker, accessToken);

			channels.add(channel);
		}

		return channels;
	}

	private GoogleDriveWatchChannel registerWatchChannel(GoogleDriveChangeTracker tracker, String accessToken) {

		if (tracker.getPageToken() == null || tracker.getPageToken().isBlank()) {

			throw new IllegalStateException("Change tracker page token is missing");
		}

		Optional<GoogleDriveWatchChannel> reusableChannel = findReusableActiveChannel(tracker.getId());

		if (reusableChannel.isPresent()) {

			return reusableChannel.get();
		}

		expireOldChannels(tracker.getId());

		String channelId = UUID.randomUUID().toString();

		String rawChannelToken = generateChannelToken();

		String channelTokenHash = hashChannelToken(rawChannelToken);

		long requestedExpirationMillis = Instant.now().plus(WATCH_CHANNEL_LIFETIME).toEpochMilli();

		GoogleDriveWatchRequest request = new GoogleDriveWatchRequest(channelId, "web_hook", webhookUrl.trim(),
				rawChannelToken, String.valueOf(requestedExpirationMillis));

		URI uri = buildWatchUri(tracker);

		GoogleDriveWatchResponse response = restClient.post()
			.uri(uri)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.body(GoogleDriveWatchResponse.class);

		validateWatchResponse(response, channelId);

		long expirationMillis = parseExpiration(response.expiration(), requestedExpirationMillis);

		GoogleDriveWatchChannel channel = new GoogleDriveWatchChannel();

		channel.setTracker(tracker);

		channel.setChannelId(channelId);

		/*
		 * We store only a SHA-256 hash of the channel verification token.
		 *
		 * The raw token is sent only to Google.
		 */
		channel.setChannelToken(channelTokenHash);

		channel.setResourceId(response.resourceId());

		channel.setResourceUri(response.resourceUri());

		channel.setExpiration(LocalDateTime.ofInstant(Instant.ofEpochMilli(expirationMillis), ZoneOffset.UTC));

		channel.setStatus(CHANNEL_STATUS_ACTIVE);

		return googleDriveWatchChannelRepository.save(channel);
	}

	private URI buildWatchUri(GoogleDriveChangeTracker tracker) {

		UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(GOOGLE_CHANGES_WATCH_URL)
			.queryParam("pageToken", tracker.getPageToken())
			.queryParam("supportsAllDrives", true)
			.queryParam("includeItemsFromAllDrives", true)
			.queryParam("includeRemoved", true)
			.queryParam("spaces", "drive");

		if (tracker.getTrackerType() == GoogleDriveTrackerType.SHARED_DRIVE) {

			if (tracker.getDriveId() == null || tracker.getDriveId().isBlank()) {

				throw new IllegalStateException("Shared Drive tracker driveId is missing");
			}

			uriBuilder.queryParam("driveId", tracker.getDriveId());
		}

		return uriBuilder.build().encode().toUri();
	}

	private Optional<GoogleDriveWatchChannel> findReusableActiveChannel(Long trackerId) {

		LocalDateTime reuseAfter = LocalDateTime.now(ZoneOffset.UTC).plus(REUSE_THRESHOLD);

		return googleDriveWatchChannelRepository.findAllByTracker_IdAndStatus(trackerId, CHANNEL_STATUS_ACTIVE)
			.stream()
			.filter(channel -> channel.getExpiration() != null && channel.getExpiration().isAfter(reuseAfter))
			.max(Comparator.comparing(GoogleDriveWatchChannel::getExpiration));
	}

	private void expireOldChannels(Long trackerId) {

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		List<GoogleDriveWatchChannel> channels = googleDriveWatchChannelRepository
			.findAllByTracker_IdAndStatus(trackerId, CHANNEL_STATUS_ACTIVE);

		for (GoogleDriveWatchChannel channel : channels) {

			if (channel.getExpiration() != null && !channel.getExpiration().isAfter(now)) {

				channel.setStatus(CHANNEL_STATUS_EXPIRED);

				googleDriveWatchChannelRepository.save(channel);
			}
		}
	}

	private void validateWatchResponse(GoogleDriveWatchResponse response, String requestedChannelId) {

		if (response == null) {

			throw new IllegalStateException("Google Drive returned an empty watch response");
		}

		if (response.id() == null || response.id().isBlank()) {

			throw new IllegalStateException("Google Drive watch channel ID is missing");
		}

		if (!requestedChannelId.equals(response.id())) {

			throw new IllegalStateException("Google Drive returned an unexpected channel ID");
		}

		if (response.resourceId() == null || response.resourceId().isBlank()) {

			throw new IllegalStateException("Google Drive watch resource ID is missing");
		}
	}

	private long parseExpiration(String expiration, long fallbackExpiration) {

		if (expiration == null || expiration.isBlank()) {

			return fallbackExpiration;
		}

		try {

			return Long.parseLong(expiration);

		}
		catch (NumberFormatException exception) {

			throw new IllegalStateException("Google Drive returned an invalid channel expiration", exception);
		}
	}

	private String generateChannelToken() {

		byte[] randomBytes = new byte[32];

		SECURE_RANDOM.nextBytes(randomBytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	private String hashChannelToken(String channelToken) {

		try {

			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

			byte[] hash = messageDigest.digest(channelToken.getBytes(StandardCharsets.UTF_8));

			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

		}
		catch (NoSuchAlgorithmException exception) {

			throw new IllegalStateException("SHA-256 algorithm is not available", exception);
		}
	}

	private void validateWebhookUrl() {

		if (webhookUrl == null || webhookUrl.isBlank()) {

			throw new IllegalStateException("Google Drive webhook URL is not configured");
		}

		URI uri;

		try {

			uri = URI.create(webhookUrl.trim());

		}
		catch (IllegalArgumentException exception) {

			throw new IllegalStateException("Google Drive webhook URL is invalid", exception);
		}

		if (!"https".equalsIgnoreCase(uri.getScheme())) {

			throw new IllegalStateException("Google Drive webhook URL must use HTTPS");
		}

		if (uri.getHost() == null || uri.getHost().isBlank()) {

			throw new IllegalStateException("Google Drive webhook URL must contain a valid host");
		}
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