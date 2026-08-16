package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.entity.User;
import com.multidrive.api.repository.GoogleDriveChangeTrackerRepository;
import com.multidrive.api.service.impl.GoogleDriveChangeProcessingServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleDriveChangeProcessingServiceImplTest {

	@Mock
	private GoogleDriveChangeTrackerRepository googleDriveChangeTrackerRepository;

	@Mock
	private GoogleTokenService googleTokenService;

	@Mock
	private GoogleDriveItemIndexService googleDriveItemIndexService;

	private GoogleDriveChangeProcessingServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveChangeProcessingServiceImpl(googleDriveChangeTrackerRepository, googleTokenService,
				googleDriveItemIndexService);
	}

	@Test
	@SuppressWarnings("unchecked")
	void processChangesFetchesAllPagesIndexesChangesAndAdvancesTracker() {

		GoogleDriveChangeTracker tracker = tracker(GoogleDriveTrackerType.USER, null);
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		server.expect(requestTo(containsString("pageToken=start-token")))
			.andExpect(requestTo(containsString("includeRemoved=true")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess(
					"{\"changes\":[{\"removed\":false,\"fileId\":\"file-1\",\"changeType\":\"file\"}],\"nextPageToken\":\"next-token\"}",
					MediaType.APPLICATION_JSON));
		server.expect(requestTo(containsString("pageToken=next-token")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess(
					"{\"changes\":[{\"removed\":true,\"fileId\":\"file-2\",\"changeType\":\"file\"}],\"newStartPageToken\":\"new-start-token\"}",
					MediaType.APPLICATION_JSON));

		when(googleDriveChangeTrackerRepository.findById(30L)).thenReturn(Optional.of(tracker));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
		when(googleDriveItemIndexService.applyChanges(eq(tracker.getConnection()), eq(GoogleDriveTrackerType.USER),
				eq(null), org.mockito.ArgumentMatchers.anyList()))
			.thenReturn(2);

		List<GoogleDriveChangeResponse> changes = service.processChanges(30L);

		ArgumentCaptor<List<GoogleDriveChangeResponse>> changesCaptor = ArgumentCaptor.forClass(List.class);

		verify(googleDriveItemIndexService).applyChanges(eq(tracker.getConnection()), eq(GoogleDriveTrackerType.USER),
				eq(null), changesCaptor.capture());
		verify(googleDriveChangeTrackerRepository).save(tracker);
		assertThat(changes).hasSize(2);
		assertThat(changesCaptor.getValue()).extracting(GoogleDriveChangeResponse::fileId)
			.containsExactly("file-1", "file-2");
		assertThat(tracker.getPageToken()).isEqualTo("new-start-token");
		assertThat(tracker.getLastSyncedAt()).isNotNull();
		server.verify();
	}

	@Test
	void processChangesAddsDriveIdForSharedDriveTracker() {

		GoogleDriveChangeTracker tracker = tracker(GoogleDriveTrackerType.SHARED_DRIVE, "drive-1");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		server.expect(requestTo(containsString("driveId=drive-1")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(
					withSuccess("{\"changes\":[],\"newStartPageToken\":\"new-token\"}", MediaType.APPLICATION_JSON));

		when(googleDriveChangeTrackerRepository.findById(30L)).thenReturn(Optional.of(tracker));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
		when(googleDriveItemIndexService.applyChanges(eq(tracker.getConnection()),
				eq(GoogleDriveTrackerType.SHARED_DRIVE), eq("drive-1"), org.mockito.ArgumentMatchers.anyList()))
			.thenReturn(0);

		assertThat(service.processChanges(30L)).isEmpty();

		assertThat(tracker.getPageToken()).isEqualTo("new-token");
		server.verify();
	}

	@Test
	void processChangesRejectsInvalidTrackerStateBeforeCallingGoogle() {

		assertThatThrownBy(() -> service.processChanges(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("trackerId is required");

		GoogleDriveChangeTracker inactiveTracker = tracker(GoogleDriveTrackerType.USER, null);
		inactiveTracker.setStatus("INACTIVE");

		when(googleDriveChangeTrackerRepository.findById(30L)).thenReturn(Optional.of(inactiveTracker));

		assertThatThrownBy(() -> service.processChanges(30L)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive change tracker is not active");

		verify(googleTokenService, never()).getValidAccessToken(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong());
	}

	@Test
	void processChangesRejectsMissingNewStartTokenWithoutSavingTracker() {

		GoogleDriveChangeTracker tracker = tracker(GoogleDriveTrackerType.USER, null);
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		server.expect(requestTo(containsString("pageToken=start-token")))
			.andRespond(withSuccess("{\"changes\":[]}", MediaType.APPLICATION_JSON));

		when(googleDriveChangeTrackerRepository.findById(30L)).thenReturn(Optional.of(tracker));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");

		assertThatThrownBy(() -> service.processChanges(30L)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive did not return a new start page token");

		verify(googleDriveItemIndexService, never()).applyChanges(org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.anyList());
		verify(googleDriveChangeTrackerRepository, never()).save(tracker);
		server.verify();
	}

	private GoogleDriveChangeTracker tracker(GoogleDriveTrackerType trackerType, String driveId) {

		User user = new User();
		ReflectionTestUtils.setField(user, "id", 42L);

		GoogleDriveConnection connection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(connection, "id", 20L);
		connection.setUser(user);

		GoogleDriveChangeTracker tracker = new GoogleDriveChangeTracker();
		ReflectionTestUtils.setField(tracker, "id", 30L);
		tracker.setConnection(connection);
		tracker.setTrackerType(trackerType);
		tracker.setDriveId(driveId);
		tracker.setPageToken("start-token");
		tracker.setStatus("ACTIVE");
		tracker.setLastSyncedAt(LocalDateTime.of(2026, 8, 16, 12, 0));

		return tracker;
	}

}
