package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.mapper.GoogleDriveCapabilityMapper;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.impl.GoogleDriveItemIndexServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDriveItemIndexServiceImplTest {

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	@Mock
	private GoogleDriveSourceRepository googleDriveSourceRepository;

	private GoogleDriveItemIndexServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveItemIndexServiceImpl(googleDriveItemRepository, googleDriveSourceRepository,
				new GoogleDriveCapabilityMapper());
	}

	@Test
	@SuppressWarnings("unchecked")
	void upsertItemsFiltersInvalidFilesResolvesSourcesAndNormalizesDriveMetadata() {

		GoogleDriveConnection connection = connection();
		GoogleDriveSource myDrive = source(1L, GoogleDriveSourceType.MY_DRIVE, null);
		GoogleDriveSource sharedDrive = source(2L, GoogleDriveSourceType.SHARED_DRIVE, "drive-a");
		GoogleDriveItem existingItem = new GoogleDriveItem();
		existingItem.setGoogleFileId("existing-file");
		existingItem.setSyncRunId("old-sync");

		GoogleDriveFileResponse existingFile = file("existing-file", " ", " ", "2026-08-16T10:00:00Z", "bad-time",
				List.of("parent-1"), null, "bad-size", false, null);
		GoogleDriveFileResponse sharedFile = file("shared-file", "Shared.png", "image/png", null, null,
				List.of("shared-parent"), "drive-a", "1024", true, true);

		when(googleDriveSourceRepository.findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(20L,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(List.of(myDrive, sharedDrive));
		when(googleDriveItemRepository.findAllByConnection_IdAndGoogleFileIdIn(org.mockito.ArgumentMatchers.eq(20L),
				org.mockito.ArgumentMatchers.anySet()))
			.thenReturn(List.of(existingItem));

		int upsertedCount = service.upsertItems(connection,
				Arrays.asList(existingFile, null,
						file(" ", "Ignored", null, null, null, null, null, null, false, false), sharedFile),
				GoogleDriveItemSourceType.MY_DRIVE, null, "sync-1");

		ArgumentCaptor<Iterable<GoogleDriveItem>> itemsCaptor = ArgumentCaptor.forClass(Iterable.class);

		verify(googleDriveItemRepository).saveAll(itemsCaptor.capture());
		List<GoogleDriveItem> savedItems = toList(itemsCaptor.getValue());

		assertThat(upsertedCount).isEqualTo(2);
		assertThat(savedItems).hasSize(2);
		assertThat(savedItems.get(0)).isSameAs(existingItem);
		assertThat(savedItems.get(0).getName()).isEqualTo("Untitled");
		assertThat(savedItems.get(0).getMimeType()).isEqualTo("application/octet-stream");
		assertThat(savedItems.get(0).getCategory()).isEqualTo(GoogleDriveItemCategory.OTHER);
		assertThat(savedItems.get(0).getParentId()).isEqualTo("parent-1");
		assertThat(savedItems.get(0).getSizeBytes()).isNull();
		assertThat(savedItems.get(0).getGoogleCreatedTime()).isEqualTo(Instant.parse("2026-08-16T10:00:00Z"));
		assertThat(savedItems.get(0).getGoogleModifiedTime()).isNull();
		assertThat(savedItems.get(0).getSyncRunId()).isEqualTo("sync-1");
		assertThat(savedItems.get(1).getSource()).isSameAs(sharedDrive);
		assertThat(savedItems.get(1).getCategory()).isEqualTo(GoogleDriveItemCategory.IMAGE);
		assertThat(savedItems.get(1).getSourceType()).isEqualTo(GoogleDriveItemSourceType.SHARED_DRIVE);
		assertThat(savedItems.get(1).getDriveId()).isEqualTo("drive-a");
		assertThat(savedItems.get(1).getSizeBytes()).isEqualTo(1024L);
		assertThat(savedItems.get(1).isTrashed()).isTrue();
		assertThat(savedItems.get(1).getExplicitlyTrashed()).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void applyChangesDeletesRemovedFilesAndUpsertsLatestFileChangeOnly() {

		GoogleDriveConnection connection = connection();
		GoogleDriveSource myDrive = source(1L, GoogleDriveSourceType.MY_DRIVE, null);
		GoogleDriveItem existingItem = new GoogleDriveItem();
		existingItem.setGoogleFileId("file-1");
		existingItem.setSyncRunId("existing-sync");

		GoogleDriveFileResponse firstVersion = file("file-1", "Old.txt", "text/plain", null, null, null, null, "1",
				false, false);
		GoogleDriveFileResponse latestVersion = file("file-1", "Latest.txt", "text/plain", null, null, null, null, "2",
				false, false);

		when(googleDriveSourceRepository.findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(20L,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(List.of(myDrive));
		when(googleDriveItemRepository.findAllByConnection_IdAndGoogleFileIdIn(org.mockito.ArgumentMatchers.eq(20L),
				org.mockito.ArgumentMatchers.anySet()))
			.thenReturn(List.of(existingItem));

		int processedCount = service.applyChanges(connection, GoogleDriveTrackerType.USER, null,
				List.of(new GoogleDriveChangeResponse(false, "file-1", null, null, "file", firstVersion, null),
						new GoogleDriveChangeResponse(false, "file-1", null, null, "file", latestVersion, null),
						new GoogleDriveChangeResponse(true, "deleted-file", null, null, "file", null, null),
						new GoogleDriveChangeResponse(false, "drive-1", null, null, "drive", null, null),
						new GoogleDriveChangeResponse(false, "missing-file-data", null, null, "file", null, null)));

		ArgumentCaptor<Iterable<GoogleDriveItem>> itemsCaptor = ArgumentCaptor.forClass(Iterable.class);

		verify(googleDriveItemRepository).deleteByConnection_IdAndGoogleFileId(20L, "deleted-file");
		verify(googleDriveItemRepository).saveAll(itemsCaptor.capture());

		List<GoogleDriveItem> savedItems = toList(itemsCaptor.getValue());

		assertThat(processedCount).isEqualTo(3);
		assertThat(savedItems).containsExactly(existingItem);
		assertThat(existingItem.getName()).isEqualTo("Latest.txt");
		assertThat(existingItem.getSizeBytes()).isEqualTo(2L);
		assertThat(existingItem.getSyncRunId()).isEqualTo("existing-sync");
	}

	@Test
	void applyChangesRejectsMissingSharedDriveTrackerId() {

		assertThatThrownBy(
				() -> service.applyChanges(connection(), GoogleDriveTrackerType.SHARED_DRIVE, " ", List.of()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("trackerDriveId is required for Shared Drive changes");
	}

	@Test
	void deleteStaleItemsValidatesInputsAndDelegatesToRepository() {

		when(googleDriveItemRepository.deleteStaleItems(20L, "sync-1")).thenReturn(3);

		assertThat(service.deleteStaleItems(20L, "sync-1")).isEqualTo(3);
		assertThatThrownBy(() -> service.deleteStaleItems(null, "sync-1")).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("connectionId is required");
		assertThatThrownBy(() -> service.deleteStaleItems(20L, " ")).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("syncRunId is required");
	}

	@Test
	void upsertItemsRejectsMissingActiveSource() {

		when(googleDriveSourceRepository.findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(20L,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(List.of());

		assertThatThrownBy(() -> service.upsertItems(connection(),
				List.of(file("file-1", "Name", "text/plain", null, null, null, null, null, false, false)),
				GoogleDriveItemSourceType.MY_DRIVE, null, "sync-1"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Active My Drive source was not found");
	}

	private GoogleDriveConnection connection() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(connection, "id", 20L);

		return connection;
	}

	private GoogleDriveSource source(Long id, GoogleDriveSourceType sourceType, String driveId) {

		GoogleDriveSource source = new GoogleDriveSource();
		ReflectionTestUtils.setField(source, "id", id);
		source.setSourceType(sourceType);
		source.setGoogleDriveId(driveId);
		source.setName(sourceType.name());
		source.setStatus(GoogleDriveSourceStatus.ACTIVE);

		return source;
	}

	private GoogleDriveFileResponse file(String id, String name, String mimeType, String createdTime,
			String modifiedTime, List<String> parents, String driveId, String size, Boolean trashed,
			Boolean explicitlyTrashed) {

		return new GoogleDriveFileResponse(id, name, mimeType, createdTime, modifiedTime, parents, "web", "thumb",
				"icon", size, driveId, trashed, explicitlyTrashed, null);
	}

	private List<GoogleDriveItem> toList(Iterable<GoogleDriveItem> items) {

		List<GoogleDriveItem> result = new ArrayList<>();

		items.forEach(result::add);

		return result;
	}

}
