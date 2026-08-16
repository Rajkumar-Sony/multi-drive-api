package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.dto.GoogleDriveInitialSyncResponse;
import com.multidrive.api.dto.GoogleSharedDriveResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.impl.GoogleDriveInitialSyncServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDriveInitialSyncServiceImplTest {

	@Mock
	private GoogleDriveConnectionRepository googleDriveConnectionRepository;

	@Mock
	private GoogleDriveService googleDriveService;

	@Mock
	private GoogleDriveItemIndexService googleDriveItemIndexService;

	private GoogleDriveInitialSyncServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveInitialSyncServiceImpl(googleDriveConnectionRepository, googleDriveService,
				googleDriveItemIndexService);
	}

	@Test
	void syncConnectionPaginatesMyDriveAndSharedDrivesThenDeletesStaleItems() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(connection, "id", 20L);

		GoogleDriveFileResponse myDriveFile = file("file-1");
		GoogleDriveFileResponse secondPageFile = file("file-2");
		GoogleDriveFileResponse sharedDriveFile = file("shared-file-1");

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(googleDriveService.getMyDriveFiles(20L, 42L, 1000, null))
			.thenReturn(new GoogleDriveFilesResponse(List.of(myDriveFile), "my-page-2", null));
		when(googleDriveService.getMyDriveFiles(20L, 42L, 1000, "my-page-2"))
			.thenReturn(new GoogleDriveFilesResponse(List.of(secondPageFile), null, null));
		when(googleDriveService.getSharedDrives(20L, 42L, 100, null)).thenReturn(new GoogleSharedDrivesResponse(
				List.of(sharedDrive("drive-a"), sharedDrive(" "), sharedDrive("drive-b")), null));
		when(googleDriveService.getSharedDriveFiles(20L, 42L, "drive-a", 1000, null))
			.thenReturn(new GoogleDriveFilesResponse(List.of(sharedDriveFile), null, null));
		when(googleDriveService.getSharedDriveFiles(20L, 42L, "drive-b", 1000, null))
			.thenReturn(new GoogleDriveFilesResponse(null, null, null));
		when(googleDriveItemIndexService.upsertItems(eq(connection), eq(List.of(myDriveFile)),
				eq(GoogleDriveItemSourceType.MY_DRIVE), eq(null), org.mockito.ArgumentMatchers.anyString()))
			.thenReturn(1);
		when(googleDriveItemIndexService.upsertItems(eq(connection), eq(List.of(secondPageFile)),
				eq(GoogleDriveItemSourceType.MY_DRIVE), eq(null), org.mockito.ArgumentMatchers.anyString()))
			.thenReturn(1);
		when(googleDriveItemIndexService.upsertItems(eq(connection), eq(List.of(sharedDriveFile)),
				eq(GoogleDriveItemSourceType.SHARED_DRIVE), eq("drive-a"), org.mockito.ArgumentMatchers.anyString()))
			.thenReturn(1);
		when(googleDriveItemIndexService.upsertItems(eq(connection), eq(List.of()),
				eq(GoogleDriveItemSourceType.SHARED_DRIVE), eq("drive-b"), org.mockito.ArgumentMatchers.anyString()))
			.thenReturn(0);
		when(googleDriveItemIndexService.deleteStaleItems(eq(20L), org.mockito.ArgumentMatchers.anyString()))
			.thenReturn(4);

		GoogleDriveInitialSyncResponse response = service.syncConnection(20L, 42L);

		assertThat(response.connectionId()).isEqualTo(20L);
		assertThat(response.myDriveItemCount()).isEqualTo(2);
		assertThat(response.sharedDriveCount()).isEqualTo(2);
		assertThat(response.sharedDriveItemCount()).isEqualTo(1);
		assertThat(response.totalItemCount()).isEqualTo(3);
		assertThat(response.staleItemCount()).isEqualTo(4);
		verify(googleDriveItemIndexService).deleteStaleItems(eq(20L), org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void syncConnectionValidatesIdsAndConnectionOwnership() {

		assertThatThrownBy(() -> service.syncConnection(null, 42L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("connectionId is required");
		assertThatThrownBy(() -> service.syncConnection(20L, null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("userId is required");

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.syncConnection(20L, 42L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google Drive connection not found");
	}

	private GoogleDriveFileResponse file(String id) {

		return new GoogleDriveFileResponse(id, id + ".txt", "text/plain", null, null, List.of("root"), null, null, null,
				"10", null, false, false, null);
	}

	private GoogleSharedDriveResponse sharedDrive(String id) {

		return new GoogleSharedDriveResponse(id, "Team Drive", false, null, null, null);
	}

}
