package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.model.DriveOperationItemExecutionSnapshot;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;
import com.multidrive.api.service.impl.RecursiveFolderCopyDriveOperationJobExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecursiveFolderCopyDriveOperationJobExecutorTest {

	private static final Long JOB_ID = 10L;

	private static final String WORKER_ID = "worker-test";

	private static final String GOOGLE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

	@Mock
	private GoogleDriveFileMutationService googleDriveFileMutationService;

	@Mock
	private GoogleDriveFileSearchService googleDriveFileSearchService;

	@Mock
	private DriveOperationLocalStateService driveOperationLocalStateService;

	@Mock
	private DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	@Mock
	private DriveOperationJobStateService driveOperationJobStateService;

	@Mock
	private GoogleDriveFileCapabilitiesResponse sourceCapabilities;

	@Mock
	private GoogleDriveFileCapabilitiesResponse destinationCapabilities;

	private DriveOperationMarkerFactory markerFactory;

	private RecursiveFolderCopyDriveOperationJobExecutor executor;

	@BeforeEach
	void setUp() {

		markerFactory = new DriveOperationMarkerFactory();
		executor = new RecursiveFolderCopyDriveOperationJobExecutor(googleDriveFileMutationService,
				googleDriveFileSearchService, new DriveOperationCapabilityGuard(), driveOperationLocalStateService,
				driveOperationJobExecutionStore, driveOperationJobStateService, markerFactory);
	}

	@Test
	void executeCreatesRootFolderAndCompletesRecursiveCopy() {

		DriveOperationJobExecutionSnapshot job = recursiveJob();
		String marker = markerFactory.create(job, "source-root");
		DriveOperationItemExecutionSnapshot readyRoot = item(1L, 0, "source-root", "Root", GOOGLE_FOLDER_MIME_TYPE,
				"dest-parent", null, null, false);
		DriveOperationItemExecutionSnapshot completedRoot = item(1L, 0, "source-root", "Root", GOOGLE_FOLDER_MIME_TYPE,
				"dest-parent", 777L, "copy-root", true);
		GoogleDriveFileResponse sourceRoot = file("source-root", GOOGLE_FOLDER_MIME_TYPE, List.of("old-parent"), false,
				sourceCapabilities, null);
		GoogleDriveFileResponse destinationParent = file("dest-parent", GOOGLE_FOLDER_MIME_TYPE, List.of(), false,
				destinationCapabilities, null);
		GoogleDriveFileResponse copiedRoot = file("copy-root", GOOGLE_FOLDER_MIME_TYPE, List.of("dest-parent"), false,
				sourceCapabilities, Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker));

		when(driveOperationJobStateService.cancelIfRequested(JOB_ID, WORKER_ID)).thenReturn(false);
		when(sourceCapabilities.canListChildren()).thenReturn(true);
		when(destinationCapabilities.canAddChildren()).thenReturn(true);
		when(googleDriveFileMutationService.getFile(20L, 99L, "source-root")).thenReturn(sourceRoot, sourceRoot);
		when(googleDriveFileMutationService.getFile(20L, 99L, "dest-parent")).thenReturn(destinationParent);
		when(driveOperationJobExecutionStore.findReadyOperationItems(JOB_ID, 100)).thenReturn(List.of(readyRoot),
				List.of());
		when(driveOperationJobExecutionStore.countIncompleteOperationItems(JOB_ID)).thenReturn(0L);
		when(googleDriveFileMutationService.createFolderWithAppProperties(20L, 99L, "dest-parent", "Copied Root",
				Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker)))
			.thenReturn(copiedRoot);
		when(googleDriveFileMutationService.getFile(20L, 99L, "copy-root")).thenReturn(copiedRoot, copiedRoot);
		when(driveOperationJobExecutionStore.markOperationItemRunning(eq(JOB_ID), eq(1L), any(LocalDateTime.class)))
			.thenReturn(true);
		when(driveOperationJobExecutionStore.markOperationItemMutationStarted(eq(JOB_ID), eq(1L),
				any(LocalDateTime.class)))
			.thenReturn(true);
		when(driveOperationJobExecutionStore.markOperationItemVerifying(eq(JOB_ID), eq(1L), any(LocalDateTime.class)))
			.thenReturn(true);
		when(driveOperationLocalStateService.createFolder(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				copiedRoot))
			.thenReturn(777L);
		when(driveOperationJobExecutionStore.findRootOperationItem(JOB_ID)).thenReturn(Optional.of(completedRoot));

		executor.execute(job, WORKER_ID);

		verify(driveOperationJobExecutionStore).planRecursiveFolderCopy(eq(JOB_ID), any(LocalDateTime.class));
		verify(driveOperationJobExecutionStore).completeRecursiveOperationItem(eq(JOB_ID), eq(1L), eq("source-root"),
				eq(777L), eq("copy-root"), eq(true), any(LocalDateTime.class));
		verify(driveOperationJobStateService).completeRecursiveCopy(JOB_ID, WORKER_ID, 777L, "copy-root");
	}

	@Test
	void executeReconcilesMutationStartedItemByMarkerBeforeCreatingAgain() {

		DriveOperationJobExecutionSnapshot job = recursiveJob();
		String marker = markerFactory.create(job, "source-root");
		DriveOperationItemExecutionSnapshot readyRoot = item(1L, 0, "source-root", "Root", GOOGLE_FOLDER_MIME_TYPE,
				"dest-parent", null, null, true);
		DriveOperationItemExecutionSnapshot completedRoot = item(1L, 0, "source-root", "Root", GOOGLE_FOLDER_MIME_TYPE,
				"dest-parent", 777L, "copy-root", true);
		GoogleDriveFileResponse sourceRoot = file("source-root", GOOGLE_FOLDER_MIME_TYPE, List.of("old-parent"), false,
				sourceCapabilities, null);
		GoogleDriveFileResponse destinationParent = file("dest-parent", GOOGLE_FOLDER_MIME_TYPE, List.of(), false,
				destinationCapabilities, null);
		GoogleDriveFileResponse existingRoot = file("copy-root", GOOGLE_FOLDER_MIME_TYPE, List.of("dest-parent"), false,
				sourceCapabilities, Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker));

		when(driveOperationJobStateService.cancelIfRequested(JOB_ID, WORKER_ID)).thenReturn(false);
		when(sourceCapabilities.canListChildren()).thenReturn(true);
		when(destinationCapabilities.canAddChildren()).thenReturn(true);
		when(googleDriveFileMutationService.getFile(20L, 99L, "source-root")).thenReturn(sourceRoot);
		when(googleDriveFileMutationService.getFile(20L, 99L, "dest-parent")).thenReturn(destinationParent);
		when(driveOperationJobExecutionStore.findReadyOperationItems(JOB_ID, 100)).thenReturn(List.of(readyRoot),
				List.of());
		when(googleDriveFileSearchService.findByAppProperty(20L, 99L, GoogleDriveSourceType.MY_DRIVE, null,
				DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker))
			.thenReturn(List.of(existingRoot));
		when(driveOperationJobExecutionStore.countIncompleteOperationItems(JOB_ID)).thenReturn(0L);
		when(driveOperationJobExecutionStore.markOperationItemRunning(eq(JOB_ID), eq(1L), any(LocalDateTime.class)))
			.thenReturn(true);
		when(driveOperationJobExecutionStore.markOperationItemVerifying(eq(JOB_ID), eq(1L), any(LocalDateTime.class)))
			.thenReturn(true);
		when(googleDriveFileMutationService.getFile(20L, 99L, "copy-root")).thenReturn(existingRoot, existingRoot);
		when(driveOperationLocalStateService.createFolder(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				existingRoot))
			.thenReturn(777L);
		when(driveOperationJobExecutionStore.findRootOperationItem(JOB_ID)).thenReturn(Optional.of(completedRoot));

		executor.execute(job, WORKER_ID);

		verify(googleDriveFileMutationService, never()).createFolderWithAppProperties(eq(20L), eq(99L),
				eq("dest-parent"), eq("Copied Root"), eq(Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker)));
		verify(driveOperationJobStateService).completeRecursiveCopy(JOB_ID, WORKER_ID, 777L, "copy-root");
	}

	private DriveOperationJobExecutionSnapshot recursiveJob() {

		return new DriveOperationJobExecutionSnapshot(JOB_ID, 99L, "google-subject-123", DriveOperationType.COPY,
				DriveOperationStrategyType.RECURSIVE_FOLDER_COPY, DriveOperationJobStatus.VALIDATING, 123L, 20L, 30L,
				"source-root", "Root", GOOGLE_FOLDER_MIME_TYPE, 30L, 20L, GoogleDriveSourceType.MY_DRIVE, null, 456L,
				"dest-parent", "Copied Root", 1, 3, false, null, LocalDateTime.parse("2026-08-17T00:00:00"));
	}

	private DriveOperationItemExecutionSnapshot item(Long id, Integer sequenceNo, String sourceGoogleFileId,
			String sourceName, String sourceMimeType, String destinationParentGoogleFileId, Long destinationLocalItemId,
			String destinationGoogleFileId, Boolean mutationStarted) {

		return new DriveOperationItemExecutionSnapshot(id, sequenceNo, 123L, sourceGoogleFileId, sourceName,
				sourceMimeType, "old-parent", sourceName, destinationParentGoogleFileId, destinationLocalItemId,
				destinationGoogleFileId, 0L, 0, null, mutationStarted);
	}

	private GoogleDriveFileResponse file(String id, String mimeType, List<String> parents, boolean trashed,
			GoogleDriveFileCapabilitiesResponse capabilities, Map<String, String> appProperties) {

		return new GoogleDriveFileResponse(id, id, mimeType, null, null, parents, null, null, null, "0", null, trashed,
				false, appProperties, capabilities);
	}

}
