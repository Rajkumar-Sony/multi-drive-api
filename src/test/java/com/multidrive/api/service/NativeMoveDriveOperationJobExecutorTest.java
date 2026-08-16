package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.service.impl.NativeMoveDriveOperationJobExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeMoveDriveOperationJobExecutorTest {

	private static final Long JOB_ID = 10L;

	private static final String WORKER_ID = "worker-test";

	@Mock
	private GoogleDriveFileMutationService googleDriveFileMutationService;

	@Mock
	private DriveOperationLocalStateService driveOperationLocalStateService;

	@Mock
	private DriveOperationJobStateService driveOperationJobStateService;

	@Mock
	private GoogleDriveFileCapabilitiesResponse sourceCapabilities;

	@Mock
	private GoogleDriveFileCapabilitiesResponse destinationCapabilities;

	private NativeMoveDriveOperationJobExecutor executor;

	@BeforeEach
	void setUp() {

		executor = new NativeMoveDriveOperationJobExecutor(googleDriveFileMutationService,
				new DriveOperationCapabilityGuard(), driveOperationLocalStateService, driveOperationJobStateService);
	}

	@Test
	void executeMovesRemoteFileAndCompletesJob() {

		DriveOperationJobExecutionSnapshot job = nativeMoveJob();

		GoogleDriveFileResponse currentFile = file("file-1", List.of("old-parent"), false, sourceCapabilities);

		GoogleDriveFileResponse destinationFolder = file("dest-parent", List.of(), false, destinationCapabilities);

		GoogleDriveFileResponse movedFile = file("file-1", List.of("dest-parent"), false, sourceCapabilities);

		when(driveOperationJobStateService.cancelIfRequested(JOB_ID, WORKER_ID)).thenReturn(false);

		when(sourceCapabilities.canMoveItemWithinDrive()).thenReturn(true);

		when(destinationCapabilities.canAddChildren()).thenReturn(true);

		when(googleDriveFileMutationService.getFile(20L, 99L, "file-1")).thenReturn(currentFile, movedFile);

		when(googleDriveFileMutationService.getFile(20L, 99L, "dest-parent")).thenReturn(destinationFolder);

		executor.execute(job, WORKER_ID);

		verify(googleDriveFileMutationService).move(20L, 99L, "file-1", "dest-parent", List.of("old-parent"));

		verify(driveOperationJobStateService).transition(JOB_ID, WORKER_ID, DriveOperationJobStatus.RUNNING,
				"Executing native Google Drive move");

		verify(driveOperationLocalStateService).updateItem(123L, movedFile);

		verify(driveOperationJobStateService).completeNativeMove(JOB_ID, WORKER_ID, 123L, "file-1");
	}

	@Test
	void executeCommitsAlreadyMovedRemoteFileWithoutMovingAgain() {

		DriveOperationJobExecutionSnapshot job = nativeMoveJob();

		GoogleDriveFileResponse alreadyMovedFile = file("file-1", List.of("dest-parent"), false, sourceCapabilities);

		GoogleDriveFileResponse destinationFolder = file("dest-parent", List.of(), false, destinationCapabilities);

		when(driveOperationJobStateService.cancelIfRequested(JOB_ID, WORKER_ID)).thenReturn(false);

		when(googleDriveFileMutationService.getFile(20L, 99L, "file-1")).thenReturn(alreadyMovedFile, alreadyMovedFile);

		when(googleDriveFileMutationService.getFile(20L, 99L, "dest-parent")).thenReturn(destinationFolder);

		executor.execute(job, WORKER_ID);

		verify(googleDriveFileMutationService, never()).move(20L, 99L, "file-1", "dest-parent", List.of("dest-parent"));

		verify(driveOperationJobStateService, never()).markRootItemRunning(JOB_ID);

		verify(driveOperationLocalStateService).updateItem(123L, alreadyMovedFile);

		verify(driveOperationJobStateService).completeNativeMove(JOB_ID, WORKER_ID, 123L, "file-1");
	}

	private DriveOperationJobExecutionSnapshot nativeMoveJob() {

		return new DriveOperationJobExecutionSnapshot(JOB_ID, 99L, "google-subject-123", DriveOperationType.MOVE,
				DriveOperationStrategyType.NATIVE_MOVE, DriveOperationJobStatus.VALIDATING, 123L, 20L, 30L, "file-1",
				30L, 20L, 456L, "dest-parent", 1, 3, false);
	}

	private GoogleDriveFileResponse file(String id, List<String> parents, boolean trashed,
			GoogleDriveFileCapabilitiesResponse capabilities) {

		return new GoogleDriveFileResponse(id, id, "application/pdf", null, null, parents, null, null, null, "100",
				null, trashed, false, capabilities);
	}

}
