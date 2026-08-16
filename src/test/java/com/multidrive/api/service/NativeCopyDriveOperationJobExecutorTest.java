package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.exception.DriveOperationCleanupRequiredException;
import com.multidrive.api.exception.DriveOperationReconciliationPendingException;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.service.impl.NativeCopyDriveOperationJobExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NativeCopyDriveOperationJobExecutorTest {

	private static final Long JOB_ID = 10L;

	private static final String WORKER_ID = "worker-test";

	@Mock
	private GoogleDriveFileMutationService googleDriveFileMutationService;

	@Mock
	private GoogleDriveFileSearchService googleDriveFileSearchService;

	@Mock
	private DriveOperationLocalStateService driveOperationLocalStateService;

	@Mock
	private DriveOperationJobStateService driveOperationJobStateService;

	@Mock
	private GoogleDriveFileCapabilitiesResponse sourceCapabilities;

	@Mock
	private GoogleDriveFileCapabilitiesResponse destinationCapabilities;

	private DriveOperationMarkerFactory markerFactory;

	private NativeCopyDriveOperationJobExecutor executor;

	@BeforeEach
	void setUp() {

		markerFactory = new DriveOperationMarkerFactory();

		executor = new NativeCopyDriveOperationJobExecutor(googleDriveFileMutationService, googleDriveFileSearchService,
				new DriveOperationCapabilityGuard(), driveOperationLocalStateService, driveOperationJobStateService,
				markerFactory);
	}

	@Test
	void executeCopiesRemoteFileWithDeterministicMarkerAndCompletesJob() {

		DriveOperationJobExecutionSnapshot job = nativeCopyJob(1, 3, null);
		String marker = markerFactory.create(job);
		GoogleDriveFileResponse sourceFile = file("file-1", List.of("old-parent"), false, sourceCapabilities, null);
		GoogleDriveFileResponse destinationFolder = file("dest-parent", List.of(), false, destinationCapabilities,
				null);
		GoogleDriveFileResponse copiedFile = file("copy-1", List.of("dest-parent"), false, sourceCapabilities,
				Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker));

		when(driveOperationJobStateService.cancelIfRequested(JOB_ID, WORKER_ID)).thenReturn(false);
		when(sourceCapabilities.canCopy()).thenReturn(true);
		when(destinationCapabilities.canAddChildren()).thenReturn(true);
		when(googleDriveFileMutationService.getFile(20L, 99L, "file-1")).thenReturn(sourceFile);
		when(googleDriveFileMutationService.getFile(20L, 99L, "dest-parent")).thenReturn(destinationFolder);
		when(googleDriveFileSearchService.findByAppProperty(20L, 99L, GoogleDriveSourceType.MY_DRIVE, null,
				DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker))
			.thenReturn(List.of());
		when(googleDriveFileMutationService.copyWithAppProperties(20L, 99L, "file-1", "dest-parent", "Copied file",
				Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker)))
			.thenReturn(copiedFile);
		when(googleDriveFileMutationService.getFile(20L, 99L, "copy-1")).thenReturn(copiedFile);
		when(driveOperationLocalStateService.createCopiedItem(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				copiedFile))
			.thenReturn(777L);

		executor.execute(job, WORKER_ID);

		verify(driveOperationJobStateService).transition(JOB_ID, WORKER_ID, DriveOperationJobStatus.RUNNING,
				"Executing native Google Drive copy");
		verify(driveOperationLocalStateService).createCopiedItem(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				copiedFile);
		verify(driveOperationJobStateService).completeNativeCopy(JOB_ID, WORKER_ID, 777L, "copy-1");
	}

	@Test
	void executeReusesExistingCopyFoundByMarker() {

		DriveOperationJobExecutionSnapshot job = nativeCopyJob(2, 3, "GOOGLE_NETWORK_ERROR");
		String marker = markerFactory.create(job);
		GoogleDriveFileResponse sourceFile = file("file-1", List.of("old-parent"), false, sourceCapabilities, null);
		GoogleDriveFileResponse destinationFolder = file("dest-parent", List.of(), false, destinationCapabilities,
				null);
		GoogleDriveFileResponse existingCopy = file("copy-1", List.of("dest-parent"), false, sourceCapabilities,
				Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker));

		when(driveOperationJobStateService.cancelIfRequested(JOB_ID, WORKER_ID)).thenReturn(false);
		when(sourceCapabilities.canCopy()).thenReturn(true);
		when(destinationCapabilities.canAddChildren()).thenReturn(true);
		when(googleDriveFileMutationService.getFile(20L, 99L, "file-1")).thenReturn(sourceFile);
		when(googleDriveFileMutationService.getFile(20L, 99L, "dest-parent")).thenReturn(destinationFolder);
		when(googleDriveFileSearchService.findByAppProperty(20L, 99L, GoogleDriveSourceType.MY_DRIVE, null,
				DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker))
			.thenReturn(List.of(existingCopy));
		when(googleDriveFileMutationService.getFile(20L, 99L, "copy-1")).thenReturn(existingCopy);
		when(driveOperationLocalStateService.createCopiedItem(20L, 30L, GoogleDriveItemSourceType.MY_DRIVE, null,
				existingCopy))
			.thenReturn(777L);

		executor.execute(job, WORKER_ID);

		verify(googleDriveFileMutationService, never()).copyWithAppProperties(eq(20L), eq(99L), eq("file-1"),
				eq("dest-parent"), eq("Copied file"), eq(Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker)));
		verify(driveOperationJobStateService).completeNativeCopy(JOB_ID, WORKER_ID, 777L, "copy-1");
	}

	@Test
	void executePausesAfterAmbiguousPreviousFailureWithoutCreatingAnotherCopy() {

		DriveOperationJobExecutionSnapshot job = nativeCopyJob(2, 3, "GOOGLE_NETWORK_ERROR");
		String marker = markerFactory.create(job);

		stubValidatedRemoteFiles();
		when(googleDriveFileSearchService.findByAppProperty(20L, 99L, GoogleDriveSourceType.MY_DRIVE, null,
				DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker))
			.thenReturn(List.of());

		assertThatThrownBy(() -> executor.execute(job, WORKER_ID))
			.isInstanceOf(DriveOperationReconciliationPendingException.class)
			.hasMessage("Waiting for Google Drive copy marker reconciliation before another copy attempt");

		verify(googleDriveFileMutationService, never()).copyWithAppProperties(eq(20L), eq(99L), eq("file-1"),
				eq("dest-parent"), eq("Copied file"), eq(Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker)));
	}

	@Test
	void executeRequiresCleanupWhenMarkerSearchFindsDuplicates() {

		DriveOperationJobExecutionSnapshot job = nativeCopyJob(1, 3, null);
		String marker = markerFactory.create(job);

		stubValidatedRemoteFiles();
		when(googleDriveFileSearchService.findByAppProperty(20L, 99L, GoogleDriveSourceType.MY_DRIVE, null,
				DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker))
			.thenReturn(List.of(
					file("copy-1", List.of("dest-parent"), false, sourceCapabilities,
							Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker)),
					file("copy-2", List.of("dest-parent"), false, sourceCapabilities,
							Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, marker))));

		assertThatThrownBy(() -> executor.execute(job, WORKER_ID))
			.isInstanceOf(DriveOperationCleanupRequiredException.class)
			.hasMessage("Multiple Google Drive files were found for the same copy operation marker");
	}

	private void stubValidatedRemoteFiles() {

		when(driveOperationJobStateService.cancelIfRequested(JOB_ID, WORKER_ID)).thenReturn(false);
		when(sourceCapabilities.canCopy()).thenReturn(true);
		when(destinationCapabilities.canAddChildren()).thenReturn(true);
		when(googleDriveFileMutationService.getFile(20L, 99L, "file-1"))
			.thenReturn(file("file-1", List.of("old-parent"), false, sourceCapabilities, null));
		when(googleDriveFileMutationService.getFile(20L, 99L, "dest-parent"))
			.thenReturn(file("dest-parent", List.of(), false, destinationCapabilities, null));
	}

	private DriveOperationJobExecutionSnapshot nativeCopyJob(int attemptCount, int maxAttempts, String errorCode) {

		return new DriveOperationJobExecutionSnapshot(JOB_ID, 99L, "google-subject-123", DriveOperationType.COPY,
				DriveOperationStrategyType.NATIVE_COPY, DriveOperationJobStatus.VALIDATING, 123L, 20L, 30L, "file-1",
				"Source file", "application/pdf", 30L, 20L, GoogleDriveSourceType.MY_DRIVE, null, 456L, "dest-parent",
				"Copied file", attemptCount, maxAttempts, false, errorCode, LocalDateTime.parse("2026-08-17T00:00:00"));
	}

	private GoogleDriveFileResponse file(String id, List<String> parents, boolean trashed,
			GoogleDriveFileCapabilitiesResponse capabilities, Map<String, String> appProperties) {

		return new GoogleDriveFileResponse(id, id, "application/pdf", null, null, parents, null, null, null, "100",
				null, trashed, false, appProperties, capabilities);
	}

}
