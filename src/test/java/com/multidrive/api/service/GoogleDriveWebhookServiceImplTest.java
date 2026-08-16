package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.dto.GoogleDriveRealtimeEventResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.entity.GoogleDriveWatchChannel;
import com.multidrive.api.entity.User;
import com.multidrive.api.repository.GoogleDriveWatchChannelRepository;
import com.multidrive.api.service.impl.GoogleDriveWebhookServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDriveWebhookServiceImplTest {

	@Mock
	private GoogleDriveWatchChannelRepository googleDriveWatchChannelRepository;

	@Mock
	private GoogleDriveChangeProcessingService googleDriveChangeProcessingService;

	@Mock
	private GoogleDriveSseService googleDriveSseService;

	private GoogleDriveWebhookServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveWebhookServiceImpl(googleDriveWatchChannelRepository,
				googleDriveChangeProcessingService, googleDriveSseService);
	}

	@Test
	void handleNotificationIgnoresUnknownChannel() {

		when(googleDriveWatchChannelRepository.findByChannelIdAndStatus("channel-1", "ACTIVE"))
			.thenReturn(Optional.empty());

		service.handleNotification("channel-1", "plain-token", "resource-1", "change", "1");

		verifyNoInteractions(googleDriveChangeProcessingService, googleDriveSseService);
	}

	@Test
	void handleNotificationExpiresStaleChannelWithoutProcessingChanges() {

		GoogleDriveWatchChannel channel = activeChannel();
		channel.setExpiration(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));

		when(googleDriveWatchChannelRepository.findByChannelIdAndStatus("channel-1", "ACTIVE"))
			.thenReturn(Optional.of(channel));

		service.handleNotification("channel-1", "plain-token", "resource-1", "change", "2");

		assertThat(channel.getStatus()).isEqualTo("EXPIRED");
		verify(googleDriveWatchChannelRepository).save(channel);
		verifyNoInteractions(googleDriveChangeProcessingService, googleDriveSseService);
	}

	@Test
	void handleNotificationUpdatesSyncMessageWithoutProcessingChanges() {

		GoogleDriveWatchChannel channel = activeChannel();

		when(googleDriveWatchChannelRepository.findByChannelIdAndStatus("channel-1", "ACTIVE"))
			.thenReturn(Optional.of(channel));

		service.handleNotification("channel-1", "plain-token", "resource-1", "sync", "3");

		assertThat(channel.getLastMessageNumber()).isEqualTo(3L);
		verify(googleDriveWatchChannelRepository).save(channel);
		verifyNoInteractions(googleDriveChangeProcessingService, googleDriveSseService);
	}

	@Test
	void handleNotificationIgnoresDuplicateMessageNumber() {

		GoogleDriveWatchChannel channel = activeChannel();
		channel.setLastMessageNumber(5L);

		when(googleDriveWatchChannelRepository.findByChannelIdAndStatus("channel-1", "ACTIVE"))
			.thenReturn(Optional.of(channel));

		service.handleNotification("channel-1", "plain-token", "resource-1", "change", "5");

		verify(googleDriveWatchChannelRepository, never()).save(channel);
		verifyNoInteractions(googleDriveChangeProcessingService, googleDriveSseService);
	}

	@Test
	void handleNotificationProcessesChangesThenPublishesRealtimeEvent() {

		GoogleDriveWatchChannel channel = activeChannel();
		GoogleDriveChangeResponse change = new GoogleDriveChangeResponse(false, "file-1", null, null, "file", null,
				null);

		when(googleDriveWatchChannelRepository.findByChannelIdAndStatus("channel-1", "ACTIVE"))
			.thenReturn(Optional.of(channel));
		when(googleDriveChangeProcessingService.processChanges(30L)).thenReturn(List.of(change));

		service.handleNotification("channel-1", "plain-token", "resource-1", "change", "6");

		ArgumentCaptor<GoogleDriveRealtimeEventResponse> eventCaptor = ArgumentCaptor
			.forClass(GoogleDriveRealtimeEventResponse.class);

		assertThat(channel.getLastMessageNumber()).isEqualTo(6L);
		verify(googleDriveWatchChannelRepository).save(channel);
		verify(googleDriveSseService).publishDriveChanges(org.mockito.ArgumentMatchers.eq(42L), eventCaptor.capture());
		assertThat(eventCaptor.getValue().eventType()).isEqualTo("DRIVE_CHANGES");
		assertThat(eventCaptor.getValue().connectionId()).isEqualTo(20L);
		assertThat(eventCaptor.getValue().trackerId()).isEqualTo(30L);
		assertThat(eventCaptor.getValue().changeCount()).isEqualTo(1);
	}

	@Test
	void handleNotificationRejectsInvalidHeadersAndSecurityMismatches() {

		assertThatThrownBy(() -> service.handleNotification(" ", "plain-token", "resource-1", "change", "1"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("X-Goog-Channel-ID is required");

		GoogleDriveWatchChannel channel = activeChannel();

		when(googleDriveWatchChannelRepository.findByChannelIdAndStatus("channel-1", "ACTIVE"))
			.thenReturn(Optional.of(channel));

		assertThatThrownBy(() -> service.handleNotification("channel-1", "wrong-token", "resource-1", "change", "1"))
			.isInstanceOf(SecurityException.class)
			.hasMessage("Google Drive channel token is invalid");
		assertThatThrownBy(
				() -> service.handleNotification("channel-1", "plain-token", "other-resource", "change", "1"))
			.isInstanceOf(SecurityException.class)
			.hasMessage("Google Drive resource ID does not match");
		assertThatThrownBy(() -> service.handleNotification("channel-1", "plain-token", "resource-1", "change", "0"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google Drive message number must be positive");
	}

	private GoogleDriveWatchChannel activeChannel() {

		User user = new User();
		ReflectionTestUtils.setField(user, "id", 42L);

		GoogleDriveConnection connection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(connection, "id", 20L);
		connection.setUser(user);

		GoogleDriveChangeTracker tracker = new GoogleDriveChangeTracker();
		ReflectionTestUtils.setField(tracker, "id", 30L);
		tracker.setConnection(connection);
		tracker.setTrackerType(GoogleDriveTrackerType.USER);
		tracker.setStatus("ACTIVE");

		GoogleDriveWatchChannel channel = new GoogleDriveWatchChannel();
		channel.setTracker(tracker);
		channel.setChannelId("channel-1");
		channel.setChannelToken(hash("plain-token"));
		channel.setResourceId("resource-1");
		channel.setExpiration(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
		channel.setStatus("ACTIVE");

		return channel;
	}

	private String hash(String channelToken) {

		try {

			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

			byte[] hash = messageDigest.digest(channelToken.getBytes(StandardCharsets.UTF_8));

			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

		}
		catch (Exception exception) {

			throw new IllegalStateException(exception);
		}
	}

}
