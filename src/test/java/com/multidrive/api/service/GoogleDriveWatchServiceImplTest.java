package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.entity.GoogleDriveWatchChannel;
import com.multidrive.api.repository.GoogleDriveChangeTrackerRepository;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.repository.GoogleDriveWatchChannelRepository;
import com.multidrive.api.service.impl.GoogleDriveWatchServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

@ExtendWith(MockitoExtension.class)
class GoogleDriveWatchServiceImplTest {

	@Mock
	private GoogleDriveChangeTrackerRepository googleDriveChangeTrackerRepository;

	@Mock
	private GoogleDriveWatchChannelRepository googleDriveWatchChannelRepository;

	@Mock
	private GoogleDriveConnectionRepository googleDriveConnectionRepository;

	@Mock
	private GoogleTokenService googleTokenService;

	private GoogleDriveWatchServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveWatchServiceImpl(googleDriveChangeTrackerRepository, googleDriveWatchChannelRepository,
				googleDriveConnectionRepository, googleTokenService, "https://example.com/google-drive/webhook");
	}

	@Test
	void registerWatchChannelsReusesExistingChannelWhenItHasEnoughLifetime() {

		GoogleDriveChangeTracker tracker = tracker(GoogleDriveTrackerType.USER, null);
		GoogleDriveWatchChannel reusableChannel = new GoogleDriveWatchChannel();
		reusableChannel.setExpiration(LocalDateTime.now(ZoneOffset.UTC).plusHours(13));

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L))
			.thenReturn(Optional.of(new GoogleDriveConnection()));
		when(googleDriveChangeTrackerRepository.findAllByConnection_IdAndStatus(20L, "ACTIVE"))
			.thenReturn(List.of(tracker));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
		when(googleDriveWatchChannelRepository.findAllByTracker_IdAndStatus(30L, "ACTIVE"))
			.thenReturn(List.of(reusableChannel));

		assertThat(service.registerWatchChannels(20L, 42L)).containsExactly(reusableChannel);

		verify(googleDriveWatchChannelRepository, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void registerWatchChannelsExpiresStaleChannelAndStoresNewHashedChannelToken() {

		GoogleDriveChangeTracker tracker = tracker(GoogleDriveTrackerType.SHARED_DRIVE, "drive-a");
		GoogleDriveWatchChannel expiredChannel = new GoogleDriveWatchChannel();
		expiredChannel.setExpiration(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
		expiredChannel.setStatus("ACTIVE");

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		AtomicReference<CapturedWatchRequest> capturedRequest = new AtomicReference<>();
		long expirationMillis = Instant.now().plusSeconds(3600).toEpochMilli();

		server.expect(requestTo(containsString("pageToken=page-token")))
			.andExpect(requestTo(containsString("driveId=drive-a")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(request -> capturedRequest.set(parseRequestBody(request)))
			.andRespond(request -> {
				String channelId = capturedRequest.get().channelId();
				String json = """
						{
						  "kind": "api#channel",
						  "id": "%s",
						  "resourceId": "resource-1",
						  "resourceUri": "https://www.googleapis.com/drive/v3/changes",
						  "expiration": "%d"
						}
						""".formatted(channelId, expirationMillis);
				MockClientHttpResponse response = new MockClientHttpResponse(json.getBytes(StandardCharsets.UTF_8),
						HttpStatus.OK);
				response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				return response;
			});

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L))
			.thenReturn(Optional.of(new GoogleDriveConnection()));
		when(googleDriveChangeTrackerRepository.findAllByConnection_IdAndStatus(20L, "ACTIVE"))
			.thenReturn(List.of(tracker));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
		when(googleDriveWatchChannelRepository.findAllByTracker_IdAndStatus(30L, "ACTIVE"))
			.thenReturn(List.of(expiredChannel), List.of(expiredChannel));
		when(googleDriveWatchChannelRepository.save(org.mockito.ArgumentMatchers.any(GoogleDriveWatchChannel.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		List<GoogleDriveWatchChannel> channels = service.registerWatchChannels(20L, 42L);

		ArgumentCaptor<GoogleDriveWatchChannel> channelCaptor = ArgumentCaptor.forClass(GoogleDriveWatchChannel.class);

		assertThat(expiredChannel.getStatus()).isEqualTo("EXPIRED");
		verify(googleDriveWatchChannelRepository).save(expiredChannel);
		verify(googleDriveWatchChannelRepository, org.mockito.Mockito.times(2)).save(channelCaptor.capture());
		GoogleDriveWatchChannel savedChannel = channelCaptor.getAllValues().get(1);
		assertThat(channels).containsExactly(savedChannel);
		assertThat(savedChannel.getTracker()).isSameAs(tracker);
		assertThat(savedChannel.getChannelId()).isEqualTo(capturedRequest.get().channelId());
		assertThat(savedChannel.getChannelToken()).isNotEqualTo(capturedRequest.get().rawToken());
		assertThat(savedChannel.getChannelToken()).isNotBlank();
		assertThat(savedChannel.getResourceId()).isEqualTo("resource-1");
		assertThat(savedChannel.getStatus()).isEqualTo("ACTIVE");
		assertThat(savedChannel.getExpiration())
			.isEqualTo(LocalDateTime.ofInstant(Instant.ofEpochMilli(expirationMillis), ZoneOffset.UTC));
		server.verify();
	}

	@Test
	void registerWatchChannelsValidatesWebhookUrlConnectionAndTrackerState() {

		GoogleDriveWatchServiceImpl missingWebhookService = new GoogleDriveWatchServiceImpl(
				googleDriveChangeTrackerRepository, googleDriveWatchChannelRepository, googleDriveConnectionRepository,
				googleTokenService, " ");

		assertThatThrownBy(() -> missingWebhookService.registerWatchChannels(20L, 42L))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive webhook URL is not configured");

		GoogleDriveWatchServiceImpl insecureWebhookService = new GoogleDriveWatchServiceImpl(
				googleDriveChangeTrackerRepository, googleDriveWatchChannelRepository, googleDriveConnectionRepository,
				googleTokenService, "http://example.com/webhook");

		assertThatThrownBy(() -> insecureWebhookService.registerWatchChannels(20L, 42L))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive webhook URL must use HTTPS");

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.registerWatchChannels(20L, 42L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google Drive connection not found");
	}

	@Test
	void registerWatchChannelsRejectsMissingTrackerPageToken() {

		GoogleDriveChangeTracker tracker = tracker(GoogleDriveTrackerType.USER, null);
		tracker.setPageToken(" ");

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L))
			.thenReturn(Optional.of(new GoogleDriveConnection()));
		when(googleDriveChangeTrackerRepository.findAllByConnection_IdAndStatus(20L, "ACTIVE"))
			.thenReturn(List.of(tracker));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");

		assertThatThrownBy(() -> service.registerWatchChannels(20L, 42L)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Change tracker page token is missing");
	}

	private CapturedWatchRequest parseRequestBody(org.springframework.http.client.ClientHttpRequest request) {

		String body = ((MockClientHttpRequest) request).getBodyAsString(StandardCharsets.UTF_8);

		String channelId = extractJsonString(body, "id");
		String rawToken = extractJsonString(body, "token");

		assertThat(extractJsonString(body, "type")).isEqualTo("web_hook");
		assertThat(extractJsonString(body, "address")).isEqualTo("https://example.com/google-drive/webhook");
		assertThat(rawToken).isNotBlank();
		assertThat(extractJsonString(body, "expiration")).isNotBlank();

		return new CapturedWatchRequest(channelId, rawToken);
	}

	private String extractJsonString(String json, String fieldName) {

		Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");

		Matcher matcher = pattern.matcher(json);

		if (!matcher.find()) {

			throw new AssertionError("Missing JSON field: " + fieldName);
		}

		return matcher.group(1);
	}

	private GoogleDriveChangeTracker tracker(GoogleDriveTrackerType trackerType, String driveId) {

		GoogleDriveChangeTracker tracker = new GoogleDriveChangeTracker();
		ReflectionTestUtils.setField(tracker, "id", 30L);
		tracker.setTrackerType(trackerType);
		tracker.setDriveId(driveId);
		tracker.setPageToken("page-token");
		tracker.setStatus("ACTIVE");

		return tracker;
	}

	private record CapturedWatchRequest(String channelId, String rawToken) {
	}

}
