package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.DriveOperationJobExecutor;
import com.multidrive.api.service.DriveOperationJobStateService;
import com.multidrive.api.service.DriveOperationLocalStateService;
import com.multidrive.api.service.GoogleDriveFileMutationService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NativeMoveDriveOperationJobExecutor implements DriveOperationJobExecutor {

	private final GoogleDriveFileMutationService googleDriveFileMutationService;

	private final DriveOperationCapabilityGuard driveOperationCapabilityGuard;

	private final DriveOperationLocalStateService driveOperationLocalStateService;

	private final DriveOperationJobStateService driveOperationJobStateService;

	public NativeMoveDriveOperationJobExecutor(GoogleDriveFileMutationService googleDriveFileMutationService,
			DriveOperationCapabilityGuard driveOperationCapabilityGuard,
			DriveOperationLocalStateService driveOperationLocalStateService,
			DriveOperationJobStateService driveOperationJobStateService) {

		this.googleDriveFileMutationService = googleDriveFileMutationService;

		this.driveOperationCapabilityGuard = driveOperationCapabilityGuard;

		this.driveOperationLocalStateService = driveOperationLocalStateService;

		this.driveOperationJobStateService = driveOperationJobStateService;
	}

	@Override
	public DriveOperationStrategyType strategyType() {

		return DriveOperationStrategyType.NATIVE_MOVE;
	}

	@Override
	public void execute(DriveOperationJobExecutionSnapshot job, String workerId) {

		validateJob(job);

		if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {

			return;
		}

		GoogleDriveFileResponse currentRemoteFile = googleDriveFileMutationService.getFile(job.sourceConnectionId(),
				job.userId(), job.sourceGoogleFileId());

		if (Boolean.TRUE.equals(currentRemoteFile.trashed())) {

			throw new IllegalArgumentException("Source item is currently in Google Drive trash");
		}

		GoogleDriveFileResponse destinationRemoteFolder = googleDriveFileMutationService
			.getFile(job.destinationConnectionId(), job.userId(), job.destinationParentGoogleFileId());

		if (Boolean.TRUE.equals(destinationRemoteFolder.trashed())) {

			throw new IllegalArgumentException("Destination folder is currently in Google Drive trash");
		}

		boolean alreadyMoved = hasParent(currentRemoteFile.parents(), job.destinationParentGoogleFileId());

		if (!alreadyMoved) {

			driveOperationCapabilityGuard
				.requireAllowed(
						currentRemoteFile.capabilities() != null
								? currentRemoteFile.capabilities().canMoveItemWithinDrive() : null,
						"MOVE", "Google no longer allows the source item to move within this Drive");

			driveOperationCapabilityGuard.requireAllowed(
					destinationRemoteFolder.capabilities() != null
							? destinationRemoteFolder.capabilities().canAddChildren() : null,
					"MOVE", "Google no longer allows items to be added to the destination folder");
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.PLANNING,
				alreadyMoved ? "Remote item is already at the requested destination" : "Native move plan validated");

		if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {

			return;
		}

		if (!alreadyMoved) {

			driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.RUNNING,
					"Executing native Google Drive move");

			driveOperationJobStateService.markRootItemRunning(job.jobId());

			googleDriveFileMutationService.move(job.sourceConnectionId(), job.userId(), job.sourceGoogleFileId(),
					job.destinationParentGoogleFileId(), currentRemoteFile.parents());
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.VERIFYING,
				"Verifying destination parent");

		driveOperationJobStateService.markRootItemVerifying(job.jobId());

		GoogleDriveFileResponse verifiedRemoteFile = googleDriveFileMutationService.getFile(job.sourceConnectionId(),
				job.userId(), job.sourceGoogleFileId());

		if (!hasParent(verifiedRemoteFile.parents(), job.destinationParentGoogleFileId())) {

			throw new IllegalStateException(
					"Google Drive move verification failed because the destination parent was not applied");
		}

		if (Boolean.TRUE.equals(verifiedRemoteFile.trashed())) {

			throw new IllegalStateException("Google Drive move verification failed because the item is now trashed");
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.COMMITTING,
				"Updating unified local Drive index");

		driveOperationLocalStateService.updateItem(job.sourceItemId(), verifiedRemoteFile);

		driveOperationJobStateService.completeNativeMove(job.jobId(), workerId, job.sourceItemId(),
				verifiedRemoteFile.id());
	}

	private void validateJob(DriveOperationJobExecutionSnapshot job) {

		if (job == null) {

			throw new IllegalArgumentException("job is required");
		}

		if (job.operationType() != DriveOperationType.MOVE) {

			throw new IllegalArgumentException("NATIVE_MOVE executor only accepts MOVE jobs");
		}

		if (job.strategyType() != DriveOperationStrategyType.NATIVE_MOVE) {

			throw new IllegalArgumentException("Invalid strategy for native move executor");
		}

		if (!job.sourceSourceId().equals(job.destinationSourceId())) {

			throw new IllegalArgumentException(
					"Native move requires source and destination to be the same Drive source");
		}

		if (!job.sourceConnectionId().equals(job.destinationConnectionId())) {

			throw new IllegalArgumentException("Native move requires the same Google Drive connection");
		}
	}

	private boolean hasParent(List<String> parents, String expectedParentId) {

		if (parents == null || expectedParentId == null) {

			return false;
		}

		return parents.stream().anyMatch(expectedParentId::equals);
	}

}
