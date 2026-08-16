package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.mapper.GoogleDriveCapabilityMapper;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.impl.DriveOperationLocalStateServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveOperationLocalStateServiceImplTest {

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	@Mock
	private GoogleDriveConnectionRepository googleDriveConnectionRepository;

	@Mock
	private GoogleDriveSourceRepository googleDriveSourceRepository;

	private DriveOperationLocalStateServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new DriveOperationLocalStateServiceImpl(googleDriveItemRepository, googleDriveConnectionRepository,
				googleDriveSourceRepository, new GoogleDriveCapabilityMapper(), new GoogleDriveItemCategoryResolver());
	}

	@Test
	void createUploadedItemCreatesOrUpdatesLocalItemFromRemoteState() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L);
		GoogleDriveFileResponse remoteFile = remoteFile("file-id", " ", null, "remote-drive-id", "invalid-size");

		when(googleDriveConnectionRepository.getReferenceById(20L)).thenReturn(connection);
		when(googleDriveSourceRepository.getReferenceById(30L)).thenReturn(source);
		when(googleDriveItemRepository.findByConnection_IdAndGoogleFileId(20L, "file-id")).thenReturn(Optional.empty());
		when(googleDriveItemRepository.saveAndFlush(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
			GoogleDriveItem item = invocation.getArgument(0);
			item.setId(999L);
			return item;
		});

		Long itemId = service.createUploadedItem(20L, 30L, GoogleDriveItemSourceType.SHARED_DRIVE, "requested-drive-id",
				remoteFile);

		ArgumentCaptor<GoogleDriveItem> itemCaptor = ArgumentCaptor.forClass(GoogleDriveItem.class);

		verify(googleDriveItemRepository).saveAndFlush(itemCaptor.capture());

		assertThat(itemId).isEqualTo(999L);
		assertThat(itemCaptor.getValue().getConnection()).isSameAs(connection);
		assertThat(itemCaptor.getValue().getSource()).isSameAs(source);
		assertThat(itemCaptor.getValue().getGoogleFileId()).isEqualTo("file-id");
		assertThat(itemCaptor.getValue().getName()).isEqualTo("Untitled");
		assertThat(itemCaptor.getValue().getMimeType()).isEqualTo("application/octet-stream");
		assertThat(itemCaptor.getValue().getCategory()).isEqualTo(GoogleDriveItemCategory.OTHER);
		assertThat(itemCaptor.getValue().getSourceType()).isEqualTo(GoogleDriveItemSourceType.SHARED_DRIVE);
		assertThat(itemCaptor.getValue().getDriveId()).isEqualTo("remote-drive-id");
		assertThat(itemCaptor.getValue().getParentId()).isEqualTo("parent-id");
		assertThat(itemCaptor.getValue().getSizeBytes()).isNull();
	}

	@Test
	void markTrashedMarksFolderDescendantsButFileRestoreDoesNotTouchDescendants() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L);
		GoogleDriveItem folder = item(123L, connection, source, GoogleDriveItemCategory.FOLDER);
		GoogleDriveItem file = item(124L, connection, source, GoogleDriveItemCategory.OTHER);

		when(googleDriveItemRepository.findById(123L)).thenReturn(Optional.of(folder));
		when(googleDriveItemRepository.findById(124L)).thenReturn(Optional.of(file));
		when(googleDriveItemRepository.saveAndFlush(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.markTrashed(123L,
				remoteFile("folder-id", "Folder", "application/vnd.google-apps.folder", null, null, true));
		service.markRestored(124L, remoteFile("file-id", "File.pdf", "application/pdf", null, "2048"));

		verify(googleDriveItemRepository).markDescendantsTrashed(20L, 30L, "folder-id");
		verify(googleDriveItemRepository, never()).restoreDescendantsAfterParentRestore(20L, 30L, "file-id");
		assertThat(folder.isTrashed()).isTrue();
		assertThat(file.getSizeBytes()).isEqualTo(2048L);
	}

	@Test
	void deleteSubtreeTargetsItemConnectionAndSource() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L);
		GoogleDriveItem folder = item(123L, connection, source, GoogleDriveItemCategory.FOLDER);

		when(googleDriveItemRepository.findById(123L)).thenReturn(Optional.of(folder));

		service.deleteSubtree(123L);

		verify(googleDriveItemRepository).deleteLocalSubtree(123L, 20L, 30L);
	}

	@Test
	void validatesCreateAndLookupInputs() {

		assertThatThrownBy(() -> service.createUploadedItem(null, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				remoteFile("file-id", "File.pdf", "application/pdf", null, "1")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("connectionId is required");

		assertThatThrownBy(() -> service.createUploadedItem(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				remoteFile(" ", "File.pdf", "application/pdf", null, "1")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google Drive file response is incomplete");

		assertThatThrownBy(
				() -> service.updateItem(null, remoteFile("file-id", "File.pdf", "application/pdf", null, "1")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("itemId is required");
	}

	private GoogleDriveConnection connection(Long connectionId) {

		GoogleDriveConnection connection = new GoogleDriveConnection();

		ReflectionTestUtils.setField(connection, "id", connectionId);

		return connection;
	}

	private GoogleDriveSource source(Long sourceId) {

		GoogleDriveSource source = new GoogleDriveSource();

		source.setId(sourceId);

		return source;
	}

	private GoogleDriveItem item(Long itemId, GoogleDriveConnection connection, GoogleDriveSource source,
			GoogleDriveItemCategory category) {

		GoogleDriveItem item = new GoogleDriveItem();

		item.setId(itemId);
		item.setConnection(connection);
		item.setSource(source);
		item.setGoogleFileId(category == GoogleDriveItemCategory.FOLDER ? "folder-id" : "file-id");
		item.setName("Existing");
		item.setMimeType("application/pdf");
		item.setCategory(category);
		item.setSourceType(GoogleDriveItemSourceType.MY_DRIVE);

		return item;
	}

	private GoogleDriveFileResponse remoteFile(String id, String name, String mimeType, String driveId, String size) {

		return remoteFile(id, name, mimeType, driveId, size, false);
	}

	private GoogleDriveFileResponse remoteFile(String id, String name, String mimeType, String driveId, String size,
			boolean trashed) {

		return new GoogleDriveFileResponse(id, name, mimeType, "2026-08-16T12:00:00Z", "not-a-timestamp",
				List.of("parent-id"), "https://example.com/view", null, null, size, driveId, trashed, false, null);
	}

}
