package com.multidrive.api.service;

import com.multidrive.api.dto.DriveCopyRequest;
import com.multidrive.api.dto.DriveCreateFolderRequest;
import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveMoveRequest;
import com.multidrive.api.dto.DriveRenameRequest;
import com.multidrive.api.dto.GoogleDriveFileCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.exception.DriveOperationPlanningException;
import com.multidrive.api.model.DriveOperationPlan;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.impl.DriveOperationServiceImpl;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriveOperationServiceImplTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	private static final Long USER_ID = 42L;

	@Mock
	private GoogleDriveItemRepository googleDriveItemRepository;

	@Mock
	private GoogleDriveSourceRepository googleDriveSourceRepository;

	@Mock
	private GoogleDriveFileMutationService googleDriveFileMutationService;

	@Mock
	private DriveOperationLocalStateService driveOperationLocalStateService;

	@Mock
	private DriveItemLookupService driveItemLookupService;

	@Mock
	private DriveOperationPlanner driveOperationPlanner;

	private DriveOperationServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new DriveOperationServiceImpl(googleDriveItemRepository, googleDriveSourceRepository,
				googleDriveFileMutationService, new DriveOperationCapabilityGuard(), driveOperationLocalStateService,
				driveItemLookupService, driveOperationPlanner);
	}

	@Test
	void createFolderInSourceRootCreatesRemoteAndIndexesLocalItem() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		DriveItemDetailsResponse response = itemResponse(999L);

		when(googleDriveSourceRepository.findOwnedSourceForOperation(30L, GOOGLE_SUBJECT_ID,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(Optional.of(source));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "root-id"))
			.thenReturn(remoteFile("root-id", List.of(), false, capabilitiesWithAddChildren()));
		when(googleDriveFileMutationService.createFolder(20L, USER_ID, "root-id", "Project Docs"))
			.thenReturn(remoteFile("created-folder", List.of("root-id"), false, capabilitiesWithAddChildren()));
		when(driveOperationLocalStateService.createFolder(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				remoteFile("created-folder", List.of("root-id"), false, capabilitiesWithAddChildren())))
			.thenReturn(999L);
		when(driveItemLookupService.getItem(GOOGLE_SUBJECT_ID, 999L)).thenReturn(response);

		assertThat(service.createFolder(GOOGLE_SUBJECT_ID, new DriveCreateFolderRequest(30L, null, " Project Docs ")))
			.isSameAs(response);

		verify(googleDriveFileMutationService).createFolder(20L, USER_ID, "root-id", "Project Docs");
		verify(driveOperationLocalStateService).createFolder(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				remoteFile("created-folder", List.of("root-id"), false, capabilitiesWithAddChildren()));
	}

	@Test
	void renameRequiresRemoteCapabilityAndUpdatesLocalState() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		GoogleDriveItem item = item(123L, source, connection, GoogleDriveItemCategory.OTHER, false);
		GoogleDriveFileResponse renamedFile = remoteFile("file-id", List.of("parent-id"), false,
				capabilitiesWithRename());
		DriveItemDetailsResponse response = itemResponse(123L);

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "file-id"))
			.thenReturn(remoteFile("file-id", List.of("parent-id"), false, capabilitiesWithRename()));
		when(googleDriveFileMutationService.rename(20L, USER_ID, "file-id", "Renamed.pdf")).thenReturn(renamedFile);
		when(driveItemLookupService.getItem(GOOGLE_SUBJECT_ID, 123L)).thenReturn(response);

		assertThat(service.rename(GOOGLE_SUBJECT_ID, 123L, new DriveRenameRequest(" Renamed.pdf "))).isSameAs(response);

		verify(driveOperationLocalStateService).updateItem(123L, renamedFile);
	}

	@Test
	void moveSkipsRemoteMutationWhenFileAlreadyHasDestinationParent() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		GoogleDriveItem item = item(123L, source, connection, GoogleDriveItemCategory.OTHER, false);
		GoogleDriveItem parent = item(456L, source, connection, GoogleDriveItemCategory.FOLDER, false);
		GoogleDriveFileResponse currentFile = remoteFile("file-id", List.of("destination-parent"), false,
				capabilitiesWithMove());
		DriveItemDetailsResponse response = itemResponse(123L);

		parent.setGoogleFileId("destination-parent");
		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveItemRepository.findOwnedItemForDetails(456L, GOOGLE_SUBJECT_ID))
			.thenReturn(Optional.of(parent));
		when(driveOperationPlanner.planMove(item, source))
			.thenReturn(new DriveOperationPlan(DriveOperationStrategyType.NATIVE_MOVE, "Native move"));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "file-id")).thenReturn(currentFile);
		when(driveItemLookupService.getItem(GOOGLE_SUBJECT_ID, 123L)).thenReturn(response);

		assertThat(service.move(GOOGLE_SUBJECT_ID, 123L, new DriveMoveRequest(30L, 456L))).isSameAs(response);

		verify(driveOperationLocalStateService).updateItem(123L, currentFile);
		verify(googleDriveFileMutationService, never()).move(20L, USER_ID, "file-id", "destination-parent",
				List.of("destination-parent"));
	}

	@Test
	void copyRejectsNonNativePlanBeforeRemoteMutation() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		GoogleDriveItem item = item(123L, source, connection, GoogleDriveItemCategory.FOLDER, false);

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveSourceRepository.findOwnedSourceForOperation(30L, GOOGLE_SUBJECT_ID,
				GoogleDriveSourceStatus.ACTIVE))
			.thenReturn(Optional.of(source));
		when(driveOperationPlanner.planCopy(item, source))
			.thenReturn(new DriveOperationPlan(DriveOperationStrategyType.RECURSIVE_FOLDER_COPY,
					"Folder copy requires recursive folder-tree processing"));

		assertThatThrownBy(() -> service.copy(GOOGLE_SUBJECT_ID, 123L, new DriveCopyRequest(30L, null, null)))
			.isInstanceOf(DriveOperationPlanningException.class)
			.hasMessage("Folder copy requires recursive folder-tree processing");

		verify(googleDriveFileMutationService, never()).copy(20L, USER_ID, "file-id", "root-id", null);
	}

	@Test
	void trashAndRestoreOnlyCallRemoteMutationWhenRemoteStateRequiresIt() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		GoogleDriveItem item = item(123L, source, connection, GoogleDriveItemCategory.OTHER, false);
		GoogleDriveFileResponse trashedFile = remoteFile("file-id", List.of("parent-id"), true,
				capabilitiesWithTrashRestoreDelete());
		GoogleDriveFileResponse restoredFile = remoteFile("file-id", List.of("parent-id"), false,
				capabilitiesWithTrashRestoreDelete());
		DriveItemDetailsResponse response = itemResponse(123L);

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "file-id")).thenReturn(trashedFile, restoredFile);
		when(driveItemLookupService.getItem(GOOGLE_SUBJECT_ID, 123L)).thenReturn(response);

		assertThat(service.trash(GOOGLE_SUBJECT_ID, 123L)).isSameAs(response);
		assertThat(service.restore(GOOGLE_SUBJECT_ID, 123L)).isSameAs(response);

		verify(driveOperationLocalStateService).markTrashed(123L, trashedFile);
		verify(driveOperationLocalStateService).markRestored(123L, restoredFile);
		verify(googleDriveFileMutationService, never()).trash(20L, USER_ID, "file-id");
		verify(googleDriveFileMutationService, never()).restore(20L, USER_ID, "file-id");
	}

	@Test
	void permanentlyDeleteDeletesRemoteFileThenLocalSubtree() {

		GoogleDriveConnection connection = connection(20L);
		GoogleDriveSource source = source(30L, connection, GoogleDriveSourceType.MY_DRIVE, "root-id");
		GoogleDriveItem item = item(123L, source, connection, GoogleDriveItemCategory.OTHER, false);

		when(googleDriveItemRepository.findOwnedItemForDetails(123L, GOOGLE_SUBJECT_ID)).thenReturn(Optional.of(item));
		when(googleDriveFileMutationService.getFile(20L, USER_ID, "file-id"))
			.thenReturn(remoteFile("file-id", List.of("parent-id"), false, capabilitiesWithTrashRestoreDelete()));

		service.permanentlyDelete(GOOGLE_SUBJECT_ID, 123L);

		verify(googleDriveFileMutationService).permanentlyDelete(20L, USER_ID, "file-id");
		verify(driveOperationLocalStateService).deleteSubtree(123L);
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

	private GoogleDriveItem item(Long itemId, GoogleDriveSource source, GoogleDriveConnection connection,
			GoogleDriveItemCategory category, boolean trashed) {

		GoogleDriveItem item = new GoogleDriveItem();

		item.setId(itemId);
		item.setSource(source);
		item.setConnection(connection);
		item.setCategory(category);
		item.setGoogleFileId("file-id");
		item.setName("Report.pdf");
		item.setMimeType("application/pdf");
		item.setParentId("parent-id");
		item.setSizeBytes(1024L);
		item.setTrashed(trashed);

		return item;
	}

	private GoogleDriveFileResponse remoteFile(String id, List<String> parents, boolean trashed,
			GoogleDriveFileCapabilitiesResponse capabilities) {

		return new GoogleDriveFileResponse(id, id, "application/pdf", null, null, parents, null, null, null, "1024",
				null, trashed, false, capabilities);
	}

	private GoogleDriveFileCapabilitiesResponse capabilitiesWithAddChildren() {

		return capabilities(null, true, null, null, null, null);
	}

	private GoogleDriveFileCapabilitiesResponse capabilitiesWithRename() {

		return capabilities(null, null, true, null, null, null);
	}

	private GoogleDriveFileCapabilitiesResponse capabilitiesWithMove() {

		return capabilities(null, true, null, null, null, true);
	}

	private GoogleDriveFileCapabilitiesResponse capabilitiesWithTrashRestoreDelete() {

		return capabilities(true, null, null, true, true, null);
	}

	private GoogleDriveFileCapabilitiesResponse capabilities(Boolean canCopy, Boolean canAddChildren, Boolean canRename,
			Boolean canTrash, Boolean canUntrash, Boolean canMoveItemWithinDrive) {

		return new GoogleDriveFileCapabilitiesResponse(null, null, null, canCopy, null, canAddChildren, true, null,
				null, null, canRename, canTrash, null, null, canUntrash, null, null, null, null, null, null,
				canMoveItemWithinDrive, null, null, null, null, null, null, null, null, null, null, null, null, null,
				null);
	}

	private DriveItemDetailsResponse itemResponse(Long itemId) {

		return new DriveItemDetailsResponse(itemId, 20L, "user@example.com", 30L, "My Drive",
				GoogleDriveItemSourceType.MY_DRIVE, null, "root-id", "file-id", "parent-id", "Report.pdf",
				"application/pdf", GoogleDriveItemCategory.OTHER, false, false, null, null, null, 1024L, null, null,
				null);
	}

}
