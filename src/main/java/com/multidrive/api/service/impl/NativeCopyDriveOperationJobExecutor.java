package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.exception.DriveOperationCleanupRequiredException;
import com.multidrive.api.exception.DriveOperationReconciliationPendingException;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.DriveOperationJobExecutor;
import com.multidrive.api.service.DriveOperationJobStateService;
import com.multidrive.api.service.DriveOperationLocalStateService;
import com.multidrive.api.service.DriveOperationMarkerFactory;
import com.multidrive.api.service.GoogleDriveFileMutationService;
import com.multidrive.api.service.GoogleDriveFileSearchService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NativeCopyDriveOperationJobExecutor implements DriveOperationJobExecutor {

	private final GoogleDriveFileMutationService googleDriveFileMutationService;

	private final GoogleDriveFileSearchService googleDriveFileSearchService;

	private final DriveOperationCapabilityGuard driveOperationCapabilityGuard;

	private final DriveOperationLocalStateService driveOperationLocalStateService;

	private final DriveOperationJobStateService driveOperationJobStateService;

	private final DriveOperationMarkerFactory driveOperationMarkerFactory;

	public NativeCopyDriveOperationJobExecutor(GoogleDriveFileMutationService googleDriveFileMutationService,
			GoogleDriveFileSearchService googleDriveFileSearchService,
			DriveOperationCapabilityGuard driveOperationCapabilityGuard,
			DriveOperationLocalStateService driveOperationLocalStateService,
			DriveOperationJobStateService driveOperationJobStateService,
			DriveOperationMarkerFactory driveOperationMarkerFactory) {

		this.googleDriveFileMutationService = googleDriveFileMutationService;

		this.googleDriveFileSearchService = googleDriveFileSearchService;

		this.driveOperationCapabilityGuard = driveOperationCapabilityGuard;

		this.driveOperationLocalStateService = driveOperationLocalStateService;

		this.driveOperationJobStateService = driveOperationJobStateService;

		this.driveOperationMarkerFactory = driveOperationMarkerFactory;
	}

	@Override
	public DriveOperationStrategyType strategyType() {

		return DriveOperationStrategyType.NATIVE_COPY;
	}

	@Override
	public void execute(DriveOperationJobExecutionSnapshot job, String workerId) {

		validateJob(job);

		if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {

			return;
		}

		GoogleDriveFileResponse sourceRemoteFile = googleDriveFileMutationService.getFile(job.sourceConnectionId(),
				job.userId(), job.sourceGoogleFileId());

		if (Boolean.TRUE.equals(sourceRemoteFile.trashed())) {

			throw new IllegalArgumentException("Source item is currently in Google Drive trash");
		}

		driveOperationCapabilityGuard.requireAllowed(
				sourceRemoteFile.capabilities() != null ? sourceRemoteFile.capabilities().canCopy() : null, "COPY",
				"Google no longer allows the source file to be copied");

		GoogleDriveFileResponse destinationRemoteFolder = googleDriveFileMutationService
			.getFile(job.destinationConnectionId(), job.userId(), job.destinationParentGoogleFileId());

		if (Boolean.TRUE.equals(destinationRemoteFolder.trashed())) {

			throw new IllegalArgumentException("Destination folder is currently in Google Drive trash");
		}

		driveOperationCapabilityGuard
			.requireAllowed(
					destinationRemoteFolder.capabilities() != null
							? destinationRemoteFolder.capabilities().canAddChildren() : null,
					"COPY", "Google no longer allows files to be added to the destination folder");

		String operationMarker = driveOperationMarkerFactory.create(job);

		List<GoogleDriveFileResponse> existingCopies = googleDriveFileSearchService.findByAppProperty(
				job.destinationConnectionId(), job.userId(), job.destinationSourceType(),
				job.destinationGoogleDriveId(), DriveOperationMarkerFactory.APP_PROPERTY_KEY, operationMarker);

		if (existingCopies.size() > 1) {

			throw new DriveOperationCleanupRequiredException("DUPLICATE_OPERATION_MARKER",
					"Multiple Google Drive files were found for the same copy operation marker");
		}

		GoogleDriveFileResponse copiedRemoteFile = existingCopies.isEmpty() ? null : existingCopies.getFirst();

		if (copiedRemoteFile == null && job.attemptCount() > 1 && isAmbiguousPreviousCopyFailure(job.errorCode())) {

			if (job.attemptCount() >= job.maxAttempts()) {

				throw new DriveOperationCleanupRequiredException("COPY_RECONCILIATION_UNRESOLVED",
						"Previous Google Drive copy attempt had an ambiguous result and no operation marker could be found safely");
			}

			throw new DriveOperationReconciliationPendingException(
					"Waiting for Google Drive copy marker reconciliation before another copy attempt");
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.PLANNING,
				copiedRemoteFile == null ? "Native copy validated and no previous copy marker was found"
						: "Existing Google Drive copy found by operation marker");

		if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {

			return;
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.RUNNING,
				copiedRemoteFile == null ? "Executing native Google Drive copy"
						: "Reconciling previously created Google Drive copy");

		driveOperationJobStateService.markRootItemRunning(job.jobId());

		if (copiedRemoteFile == null) {

			copiedRemoteFile = googleDriveFileMutationService.copyWithAppProperties(job.sourceConnectionId(),
					job.userId(), job.sourceGoogleFileId(), job.destinationParentGoogleFileId(), job.requestedName(),
					Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, operationMarker));
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.VERIFYING,
				"Verifying copied Google Drive file");

		driveOperationJobStateService.markRootItemVerifying(job.jobId());

		GoogleDriveFileResponse verifiedCopy = googleDriveFileMutationService.getFile(job.destinationConnectionId(),
				job.userId(), copiedRemoteFile.id());

		verifyCopy(job, operationMarker, verifiedCopy);

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.COMMITTING,
				"Updating copied file in unified local Drive index");

		GoogleDriveItemSourceType itemSourceType = resolveItemSourceType(job.destinationSourceType());

		Long localCopyItemId = driveOperationLocalStateService.createCopiedItem(job.destinationConnectionId(),
				job.destinationSourceId(), itemSourceType,
				itemSourceType == GoogleDriveItemSourceType.SHARED_DRIVE ? job.destinationGoogleDriveId() : null,
				verifiedCopy);

		driveOperationJobStateService.completeNativeCopy(job.jobId(), workerId, localCopyItemId, verifiedCopy.id());
	}

	private void verifyCopy(DriveOperationJobExecutionSnapshot job, String expectedMarker,
			GoogleDriveFileResponse verifiedCopy) {

		if (verifiedCopy == null || verifiedCopy.id() == null || verifiedCopy.id().isBlank()) {

			throw new DriveOperationCleanupRequiredException("COPY_VERIFICATION_FAILED",
					"Copied Google Drive file could not be retrieved");
		}

		if (Boolean.TRUE.equals(verifiedCopy.trashed())) {

			throw new DriveOperationCleanupRequiredException("COPY_VERIFICATION_TRASHED",
					"Copied Google Drive file is unexpectedly in trash");
		}

		if (!hasParent(verifiedCopy.parents(), job.destinationParentGoogleFileId())) {

			throw new DriveOperationCleanupRequiredException("COPY_DESTINATION_MISMATCH",
					"Copied Google Drive file is not located in the requested destination folder");
		}

		String actualMarker = verifiedCopy.appProperties() != null
				? verifiedCopy.appProperties().get(DriveOperationMarkerFactory.APP_PROPERTY_KEY) : null;

		if (!expectedMarker.equals(actualMarker)) {

			throw new DriveOperationCleanupRequiredException("COPY_MARKER_MISMATCH",
					"Copied Google Drive file does not contain the expected private operation marker");
		}

		if (job.destinationSourceType() == GoogleDriveSourceType.SHARED_DRIVE && job.destinationGoogleDriveId() != null
				&& !job.destinationGoogleDriveId().equals(verifiedCopy.driveId())) {

			throw new DriveOperationCleanupRequiredException("COPY_SHARED_DRIVE_MISMATCH",
					"Copied file was created in an unexpected Shared Drive");
		}
	}

	private boolean isAmbiguousPreviousCopyFailure(String errorCode) {

		if (errorCode == null || errorCode.isBlank()) {

			return false;
		}

		return errorCode.equals("GOOGLE_NETWORK_ERROR") || errorCode.equals("GOOGLE_HTTP_408")
				|| errorCode.equals("GOOGLE_HTTP_429") || errorCode.startsWith("GOOGLE_HTTP_5")
				|| errorCode.equals("COPY_RECONCILIATION_PENDING") || errorCode.equals("WORKER_LEASE_EXPIRED");
	}

	private GoogleDriveItemSourceType resolveItemSourceType(GoogleDriveSourceType sourceType) {

		if (sourceType == GoogleDriveSourceType.SHARED_DRIVE) {

			return GoogleDriveItemSourceType.SHARED_DRIVE;
		}

		return GoogleDriveItemSourceType.MY_DRIVE;
	}

	private boolean hasParent(List<String> parents, String expectedParentId) {

		if (parents == null || expectedParentId == null) {

			return false;
		}

		return parents.stream().anyMatch(expectedParentId::equals);
	}

	private void validateJob(DriveOperationJobExecutionSnapshot job) {

		if (job == null) {

			throw new IllegalArgumentException("job is required");
		}

		if (job.operationType() != DriveOperationType.COPY) {

			throw new IllegalArgumentException("NATIVE_COPY executor only accepts COPY jobs");
		}

		if (job.strategyType() != DriveOperationStrategyType.NATIVE_COPY) {

			throw new IllegalArgumentException("Invalid strategy for native copy executor");
		}

		if (!job.sourceSourceId().equals(job.destinationSourceId())) {

			throw new IllegalArgumentException(
					"Native copy requires source and destination to be the same Drive source");
		}

		if (!job.sourceConnectionId().equals(job.destinationConnectionId())) {

			throw new IllegalArgumentException("Native copy requires the same Google Drive connection");
		}

		if (job.destinationSourceType() == null) {

			throw new IllegalStateException("Destination Drive source type is missing");
		}
	}

}
