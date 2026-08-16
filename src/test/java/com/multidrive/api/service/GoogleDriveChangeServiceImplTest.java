package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleSharedDriveResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.repository.GoogleDriveChangeTrackerRepository;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.impl.GoogleDriveChangeServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleDriveChangeServiceImplTest {

	@Mock
	private GoogleDriveChangeTrackerRepository googleDriveChangeTrackerRepository;

	@Mock
	private GoogleDriveConnectionRepository googleDriveConnectionRepository;

	@Mock
	private GoogleTokenService googleTokenService;

	@Mock
	private GoogleDriveService googleDriveService;

	private GoogleDriveChangeServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveChangeServiceImpl(googleDriveChangeTrackerRepository, googleDriveConnectionRepository,
				googleTokenService, googleDriveService);
	}

	@Test
	void initializeUserTrackerKeepsExistingPageToken() {

		GoogleDriveConnection connection = connection();
		GoogleDriveChangeTracker existingTracker = new GoogleDriveChangeTracker();
		existingTracker.setPageToken("existing-token");

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(googleDriveChangeTrackerRepository.findByConnection_IdAndTrackerTypeAndDriveIdIsNull(20L,
				GoogleDriveTrackerType.USER))
			.thenReturn(Optional.of(existingTracker));

		assertThat(service.initializeUserTracker(20L, 42L)).isSameAs(existingTracker);

		verify(googleTokenService, never()).getValidAccessToken(20L, 42L);
		verify(googleDriveChangeTrackerRepository, never()).save(existingTracker);
	}

	@Test
	void initializeUserTrackerFetchesStartPageTokenAndSavesTracker() {

		GoogleDriveConnection connection = connection();
		GoogleDriveChangeTracker existingTracker = new GoogleDriveChangeTracker();
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		server.expect(requestTo(containsString("supportsAllDrives=true")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess("{\"startPageToken\":\"start-token\",\"kind\":\"drive#startPageToken\"}",
					MediaType.APPLICATION_JSON));

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(googleDriveChangeTrackerRepository.findByConnection_IdAndTrackerTypeAndDriveIdIsNull(20L,
				GoogleDriveTrackerType.USER))
			.thenReturn(Optional.of(existingTracker));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
		when(googleDriveChangeTrackerRepository.save(existingTracker)).thenReturn(existingTracker);

		assertThat(service.initializeUserTracker(20L, 42L)).isSameAs(existingTracker);

		assertThat(existingTracker.getConnection()).isSameAs(connection);
		assertThat(existingTracker.getTrackerType()).isEqualTo(GoogleDriveTrackerType.USER);
		assertThat(existingTracker.getDriveId()).isNull();
		assertThat(existingTracker.getPageToken()).isEqualTo("start-token");
		assertThat(existingTracker.getStatus()).isEqualTo("ACTIVE");
		server.verify();
	}

	@Test
	void initializeSharedDriveTrackersPaginatesDrivesSkipsInvalidIdsAndPreservesExistingToken() {

		GoogleDriveConnection connection = connection();
		GoogleDriveChangeTracker existingTracker = new GoogleDriveChangeTracker();
		existingTracker.setDriveId("drive-a");
		existingTracker.setPageToken("existing-token");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		server.expect(requestTo(containsString("driveId=drive-b")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess("{\"startPageToken\":\"drive-b-token\"}", MediaType.APPLICATION_JSON));

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
		when(googleDriveService.getSharedDrives(20L, 42L, 100, null)).thenReturn(new GoogleSharedDrivesResponse(
				List.of(sharedDrive("drive-a"), sharedDrive(" "), sharedDrive("drive-b")), "page-2"));
		when(googleDriveService.getSharedDrives(20L, 42L, 100, "page-2"))
			.thenReturn(new GoogleSharedDrivesResponse(List.of(), null));
		when(googleDriveChangeTrackerRepository.findByConnection_IdAndTrackerTypeAndDriveId(20L,
				GoogleDriveTrackerType.SHARED_DRIVE, "drive-a"))
			.thenReturn(Optional.of(existingTracker));
		when(googleDriveChangeTrackerRepository.findByConnection_IdAndTrackerTypeAndDriveId(20L,
				GoogleDriveTrackerType.SHARED_DRIVE, "drive-b"))
			.thenReturn(Optional.empty());
		when(googleDriveChangeTrackerRepository.save(org.mockito.ArgumentMatchers.any(GoogleDriveChangeTracker.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		List<GoogleDriveChangeTracker> trackers = service.initializeSharedDriveTrackers(20L, 42L);

		assertThat(trackers).hasSize(2);
		assertThat(trackers.get(0)).isSameAs(existingTracker);
		assertThat(trackers.get(1).getTrackerType()).isEqualTo(GoogleDriveTrackerType.SHARED_DRIVE);
		assertThat(trackers.get(1).getDriveId()).isEqualTo("drive-b");
		assertThat(trackers.get(1).getPageToken()).isEqualTo("drive-b-token");
		assertThat(trackers.get(1).getStatus()).isEqualTo("ACTIVE");
		server.verify();
	}

	@Test
	void initializeTrackerValidatesIdsAndConnectionOwnership() {

		assertThatThrownBy(() -> service.initializeUserTracker(null, 42L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("connectionId is required");
		assertThatThrownBy(() -> service.initializeSharedDriveTrackers(20L, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("userId is required");

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.initializeUserTracker(20L, 42L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google Drive connection not found");
	}

	private GoogleDriveConnection connection() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(connection, "id", 20L);

		return connection;
	}

	private GoogleSharedDriveResponse sharedDrive(String id) {

		return new GoogleSharedDriveResponse(id, "Team Drive", false, null, null, null);
	}

}
