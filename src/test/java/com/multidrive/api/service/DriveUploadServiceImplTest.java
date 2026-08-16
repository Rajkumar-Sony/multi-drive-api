package com.multidrive.api.service;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveUploadRequest;
import com.multidrive.api.dto.GoogleDriveFileCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.impl.DriveUploadServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveUploadServiceImplTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	private static final Long USER_ID = 42L;

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	@Mock
	private GoogleDriveSourceRepository googleDriveSourceRepository;

	@Mock
	private GoogleDriveFileMutationService googleDriveFileMutationService;

	@Mock
	private GoogleDriveResumableUploadService googleDriveResumableUploadService;

	@Mock
	private DriveOperationLocalStateService driveOperationLocalStateService;

	@Mock
	private DriveItemLookupService driveItemLookupService;

	private DriveUploadServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new DriveUploadServiceImpl(googleDriveItemRepository, googleDriveSourceRepository,
				googleDriveFileMutationService, googleDriveResumableUploadService, new DriveOperationCapabilityGuard(),
				driveOperationLocalStateService, driveItemLookupService);
	}

	@Test
	void uploadToSourceRootSanitizesOriginalNameDefaultsContentTypeAndIndexesLocalItem() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		MockMultipartFile file = new MockMultipartFile("file", "../../report.txt", "bad content type",
				"hello".getBytes());
		GoogleDriveFileResponse remoteFile = remoteFile("uploaded-file");
		DriveItemDetailsResponse response = itemResponse(999L);

		when(googleDriveSourceRepository.findOwnedSourceForOperation(30L, GOOGLE_SUBJECT_ID,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(Optional.of(source));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "root-id"))
			.thenReturn(remoteFile("root-id", capabilitiesWithAddChildren()));
		when(googleDriveResumableUploadService.upload(org.mockito.ArgumentMatchers.eq(20L),
				org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.eq("root-id"),
				org.mockito.ArgumentMatchers.eq("report.txt"),
				org.mockito.ArgumentMatchers.eq("application/octet-stream"), org.mockito.ArgumentMatchers.eq(5L),
				org.mockito.ArgumentMatchers.any()))
			.thenReturn(remoteFile);
		when(driveOperationLocalStateService.createUploadedItem(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				remoteFile))
			.thenReturn(999L);
		when(driveItemLookupService.getItem(GOOGLE_SUBJECT_ID, 999L)).thenReturn(response);

		assertThat(service.upload(GOOGLE_SUBJECT_ID, new DriveUploadRequest(30L, null, null, file))).isSameAs(response);

		ArgumentCaptor<InputStreamProvider> providerCaptor = ArgumentCaptor.forClass(InputStreamProvider.class);

		verify(googleDriveResumableUploadService).upload(org.mockito.ArgumentMatchers.eq(20L),
				org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.eq("root-id"),
				org.mockito.ArgumentMatchers.eq("report.txt"),
				org.mockito.ArgumentMatchers.eq("application/octet-stream"), org.mockito.ArgumentMatchers.eq(5L),
				providerCaptor.capture());
		assertThat(providerCaptor.getValue()).isNotNull();
		verify(driveOperationLocalStateService).createUploadedItem(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				remoteFile);
	}

	@Test
	void uploadToFolderUsesRequestedNameAndSharedDriveId() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.SHARED_DRIVE, "drive-root");
		GoogleDriveItem parent = parentFolder(456L, source, connection);
		MockMultipartFile file = new MockMultipartFile("file", "ignored.txt", "text/plain", "hello".getBytes());
		GoogleDriveFileResponse remoteFile = remoteFile("uploaded-file");

		when(googleDriveItemRepository.findOwnedItemForDetails(456L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(parent));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "parent-folder"))
			.thenReturn(remoteFile("parent-folder", capabilitiesWithAddChildren()));
		when(googleDriveResumableUploadService.upload(org.mockito.ArgumentMatchers.eq(20L),
				org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.eq("parent-folder"),
				org.mockito.ArgumentMatchers.eq("Requested.txt"), org.mockito.ArgumentMatchers.eq("text/plain"),
				org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.any()))
			.thenReturn(remoteFile);
		when(driveOperationLocalStateService.createUploadedItem(20L, 30L, GoogleDriveItemSourceType.SHARED_DRIVE,
				"shared-drive-id", remoteFile))
			.thenReturn(999L);
		when(driveItemLookupService.getItem(GOOGLE_SUBJECT_ID, 999L)).thenReturn(itemResponse(999L));

		service.upload(GOOGLE_SUBJECT_ID, new DriveUploadRequest(30L, 456L, " Requested.txt ", file));

		verify(driveOperationLocalStateService).createUploadedItem(20L, 30L, GoogleDriveItemSourceType.SHARED_DRIVE,
				"shared-drive-id", remoteFile);
	}

	@Test
	void uploadRejectsInvalidRequestAndInvalidFolderDestination() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		GoogleDriveItem nonFolder = parentFolder(456L, source, connection);

		nonFolder.setCategory(GoogleDriveItemCategory.OTHER);

		MockMultipartFile file = new MockMultipartFile("file", "report.txt", "text/plain", "hello".getBytes());

		assertThatThrownBy(() -> service.upload(" ", new DriveUploadRequest(30L, null, null, file)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("googleSubjectId is required");

		assertThatThrownBy(() -> service.upload(GOOGLE_SUBJECT_ID, new DriveUploadRequest(null, null, null, file)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("sourceId is required");

		when(googleDriveItemRepository.findOwnedItemForDetails(456L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(nonFolder));

		assertThatThrownBy(() -> service.upload(GOOGLE_SUBJECT_ID, new DriveUploadRequest(30L, 456L, null, file)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("parentItemId must reference a folder");
	}

	private GoogleDriveConnection connection(Long connectionId) {

		com.multidrive.api.entity.User user = new com.multidrive.api.entity.User();
		GoogleDriveConnection connection = new GoogleDriveConnection();

		ReflectionTestUtils.setField(user, "id", USER_ID);
		ReflectionTestUtils.setField(connection, "id", connectionId);
		connection.setUser(user);

		return connection;
	}

	private GoogleDriveSource source(Long sourceId, GoogleDriveConnection connection, GoogleDriveSourceType sourceType,
			String rootFolderId) {

		GoogleDriveSource source = new GoogleDriveSource();

		source.setId(sourceId);
		source.setConnection(connection);
		source.setSourceType(sourceType);
		source.setGoogleDriveId("shared-drive-id");
		source.setRootFolderId(rootFolderId);

		return source;
	}

	private GoogleDriveItem parentFolder(Long itemId, GoogleDriveSource source, GoogleDriveConnection connection) {

		GoogleDriveItem item = new GoogleDriveItem();

		item.setId(itemId);
		item.setConnection(connection);
		item.setSource(source);
		item.setCategory(GoogleDriveItemCategory.FOLDER);
		item.setSourceType(source.getSourceType() == GoogleDriveSourceType.SHARED_DRIVE
				? GoogleDriveItemSourceType.SHARED_DRIVE : GoogleDriveItemSourceType.MY_DRIVE);
		item.setGoogleFileId("parent-folder");

		return item;
	}

	private GoogleDriveFileResponse remoteFile(String id) {

		return remoteFile(id, null);
	}

	private GoogleDriveFileResponse remoteFile(String id, GoogleDriveFileCapabilitiesResponse capabilities) {

		return new GoogleDriveFileResponse(id, id, "text/plain", null, null, List.of("parent-id"), null, null, null,
				"5", null, false, false, capabilities);
	}

	private GoogleDriveFileCapabilitiesResponse capabilitiesWithAddChildren() {

		return new GoogleDriveFileCapabilitiesResponse(null, null, null, null, null, true, null, null, null, null, null,
				null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null, null, null, null, null, null, null, null);
	}

	private DriveItemDetailsResponse itemResponse(Long itemId) {

		return new DriveItemDetailsResponse(itemId, 20L, "user@example.com", 30L, "My Drive",
				GoogleDriveItemSourceType.MY_DRIVE, null, "root-id", "file-id", "parent-id", "Report.pdf",
				"application/pdf", GoogleDriveItemCategory.OTHER, false, false, null, null, null, 1024L, null, null,
				null);
	}

}
