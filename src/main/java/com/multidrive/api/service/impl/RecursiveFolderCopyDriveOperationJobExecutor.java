package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.exception.DriveOperationCleanupRequiredException;
import com.multidrive.api.exception.DriveOperationLeaseLostException;
import com.multidrive.api.exception.DriveOperationReconciliationPendingException;
import com.multidrive.api.model.DriveOperationItemExecutionSnapshot;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.DriveOperationJobExecutionStore;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.DriveOperationJobExecutor;
import com.multidrive.api.service.DriveOperationJobStateService;
import com.multidrive.api.service.DriveOperationLocalStateService;
import com.multidrive.api.service.DriveOperationMarkerFactory;
import com.multidrive.api.service.GoogleDriveFileMutationService;
import com.multidrive.api.service.GoogleDriveFileSearchService;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class RecursiveFolderCopyDriveOperationJobExecutor implements DriveOperationJobExecutor {

	private static final String GOOGLE_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

	private static final int READY_ITEM_BATCH_SIZE = 100;

	private final GoogleDriveFileMutationService googleDriveFileMutationService;

	private final GoogleDriveFileSearchService googleDriveFileSearchService;

	private final DriveOperationCapabilityGuard driveOperationCapabilityGuard;

	private final DriveOperationLocalStateService driveOperationLocalStateService;

	private final DriveOperationJobExecutionStore driveOperationJobExecutionStore;

	private final DriveOperationJobStateService driveOperationJobStateService;

	private final DriveOperationMarkerFactory driveOperationMarkerFactory;

	public RecursiveFolderCopyDriveOperationJobExecutor(GoogleDriveFileMutationService googleDriveFileMutationService,
			GoogleDriveFileSearchService googleDriveFileSearchService,
			DriveOperationCapabilityGuard driveOperationCapabilityGuard,
			DriveOperationLocalStateService driveOperationLocalStateService,
			DriveOperationJobExecutionStore driveOperationJobExecutionStore,
			DriveOperationJobStateService driveOperationJobStateService,
			DriveOperationMarkerFactory driveOperationMarkerFactory) {

		this.googleDriveFileMutationService = googleDriveFileMutationService;
		this.googleDriveFileSearchService = googleDriveFileSearchService;
		this.driveOperationCapabilityGuard = driveOperationCapabilityGuard;
		this.driveOperationLocalStateService = driveOperationLocalStateService;
		this.driveOperationJobExecutionStore = driveOperationJobExecutionStore;
		this.driveOperationJobStateService = driveOperationJobStateService;
		this.driveOperationMarkerFactory = driveOperationMarkerFactory;
	}

	@Override
	public DriveOperationStrategyType strategyType() {

		return DriveOperationStrategyType.RECURSIVE_FOLDER_COPY;
	}

	@Override
	public void execute(DriveOperationJobExecutionSnapshot job, String workerId) {

		validateJob(job);

		if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {
			return;
		}

		validateCurrentRootState(job);

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.PLANNING,
				"Planning recursive folder hierarchy");

		driveOperationJobExecutionStore.planRecursiveFolderCopy(job.jobId(), now());
		driveOperationJobStateService.publishProgress(job.jobId(), "Recursive folder hierarchy planned");

		if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {
			return;
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.RUNNING,
				"Copying recursive folder contents");

		while (true) {

			if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {
				return;
			}

			List<DriveOperationItemExecutionSnapshot> readyItems = driveOperationJobExecutionStore
				.findReadyOperationItems(job.jobId(), READY_ITEM_BATCH_SIZE);

			if (readyItems.isEmpty()) {

				long incompleteItems = driveOperationJobExecutionStore.countIncompleteOperationItems(job.jobId());

				if (incompleteItems == 0) {
					break;
				}

				throw new DriveOperationCleanupRequiredException("RECURSIVE_DEPENDENCY_BLOCKED",
						"Recursive folder copy has incomplete items but no item has a resolved destination parent");
			}

			for (DriveOperationItemExecutionSnapshot item : readyItems) {

				if (driveOperationJobStateService.cancelIfRequested(job.jobId(), workerId)) {
					return;
				}

				processItem(job, item);
			}

			driveOperationJobStateService.publishProgress(job.jobId(), "Recursive folder copy batch completed");
		}

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.VERIFYING,
				"Verifying recursive folder operation");

		if (driveOperationJobExecutionStore.countIncompleteOperationItems(job.jobId()) != 0) {

			throw new DriveOperationCleanupRequiredException("RECURSIVE_VERIFICATION_FAILED",
					"Recursive folder copy still contains incomplete operation items");
		}

		DriveOperationItemExecutionSnapshot rootItem = driveOperationJobExecutionStore
			.findRootOperationItem(job.jobId())
			.orElseThrow(() -> new DriveOperationCleanupRequiredException("RECURSIVE_ROOT_RESULT_MISSING",
					"Recursive folder copy root operation item is missing"));

		if (rootItem.destinationLocalItemId() == null || rootItem.destinationGoogleFileId() == null
				|| rootItem.destinationGoogleFileId().isBlank()) {

			throw new DriveOperationCleanupRequiredException("RECURSIVE_ROOT_RESULT_INCOMPLETE",
					"Recursive folder copy root destination result is incomplete");
		}

		GoogleDriveFileResponse verifiedRoot = googleDriveFileMutationService.getFile(job.destinationConnectionId(),
				job.userId(), rootItem.destinationGoogleFileId());

		verifyDestinationItem(job, rootItem, driveOperationMarkerFactory.create(job, rootItem.sourceGoogleFileId()),
				verifiedRoot);

		driveOperationJobStateService.transition(job.jobId(), workerId, DriveOperationJobStatus.COMMITTING,
				"Committing recursive folder copy result");

		driveOperationJobStateService.completeRecursiveCopy(job.jobId(), workerId, rootItem.destinationLocalItemId(),
				rootItem.destinationGoogleFileId());
	}

	private void processItem(DriveOperationJobExecutionSnapshot job, DriveOperationItemExecutionSnapshot item) {

		boolean folder = GOOGLE_FOLDER_MIME_TYPE.equals(item.sourceMimeType());
		String operationMarker = driveOperationMarkerFactory.create(job, item.sourceGoogleFileId());
		GoogleDriveFileResponse destinationFile = null;

		if (Boolean.TRUE.equals(item.mutationStarted())) {

			List<GoogleDriveFileResponse> existing = googleDriveFileSearchService.findByAppProperty(
					job.destinationConnectionId(), job.userId(), job.destinationSourceType(),
					job.destinationGoogleDriveId(), DriveOperationMarkerFactory.APP_PROPERTY_KEY, operationMarker);

			if (existing.size() > 1) {

				throw new DriveOperationCleanupRequiredException("DUPLICATE_RECURSIVE_MARKER",
						"Multiple destination files were found for one recursive operation item");
			}

			if (existing.size() == 1) {
				destinationFile = existing.getFirst();
			}
			else {
				throw new DriveOperationReconciliationPendingException(
						"A previous recursive copy mutation may have reached Google Drive but its operation marker cannot yet be reconciled");
			}
		}

		if (destinationFile == null) {

			GoogleDriveFileResponse sourceRemoteFile = googleDriveFileMutationService.getFile(job.sourceConnectionId(),
					job.userId(), item.sourceGoogleFileId());

			if (Boolean.TRUE.equals(sourceRemoteFile.trashed())) {

				throw new IllegalArgumentException(
						"Source item entered Google Drive trash while recursive copy was running: "
								+ item.sourcePath());
			}

			if (folder) {
				driveOperationCapabilityGuard.requireAllowed(sourceRemoteFile.capabilities() != null
						? sourceRemoteFile.capabilities().canListChildren() : null, "COPY_FOLDER",
						"Cannot read the source folder contents");
			}
			else {
				driveOperationCapabilityGuard.requireAllowed(
						sourceRemoteFile.capabilities() != null ? sourceRemoteFile.capabilities().canCopy() : null,
						"COPY", "Cannot copy the source file");
			}

			markItemRunning(job, item);
			markMutationStarted(job, item);

			String destinationName = resolveDestinationName(job, item);
			Map<String, String> appProperties = Map.of(DriveOperationMarkerFactory.APP_PROPERTY_KEY, operationMarker);

			if (folder) {
				destinationFile = googleDriveFileMutationService.createFolderWithAppProperties(
						job.destinationConnectionId(), job.userId(), item.destinationParentGoogleFileId(),
						destinationName, appProperties);
			}
			else {
				destinationFile = googleDriveFileMutationService.copyWithAppProperties(job.sourceConnectionId(),
						job.userId(), item.sourceGoogleFileId(), item.destinationParentGoogleFileId(), destinationName,
						appProperties);
			}
		}
		else {
			markItemRunning(job, item);
		}

		markItemVerifying(job, item);

		GoogleDriveFileResponse verifiedDestination = googleDriveFileMutationService
			.getFile(job.destinationConnectionId(), job.userId(), destinationFile.id());

		verifyDestinationItem(job, item, operationMarker, verifiedDestination);

		GoogleDriveItemSourceType destinationItemSourceType = resolveDestinationItemSourceType(
				job.destinationSourceType());
		String destinationDriveId = destinationItemSourceType == GoogleDriveItemSourceType.SHARED_DRIVE
				? job.destinationGoogleDriveId() : null;

		Long destinationLocalItemId = folder
				? driveOperationLocalStateService.createFolder(job.destinationConnectionId(), job.destinationSourceId(),
						destinationItemSourceType, destinationDriveId, verifiedDestination)
				: driveOperationLocalStateService.createCopiedItem(job.destinationConnectionId(),
						job.destinationSourceId(), destinationItemSourceType, destinationDriveId, verifiedDestination);

		driveOperationJobExecutionStore.completeRecursiveOperationItem(job.jobId(), item.id(),
				item.sourceGoogleFileId(), destinationLocalItemId, verifiedDestination.id(), folder, now());
	}

	private void validateCurrentRootState(DriveOperationJobExecutionSnapshot job) {

		GoogleDriveFileResponse sourceRoot = googleDriveFileMutationService.getFile(job.sourceConnectionId(),
				job.userId(), job.sourceGoogleFileId());

		if (!GOOGLE_FOLDER_MIME_TYPE.equals(sourceRoot.mimeType())) {
			throw new IllegalArgumentException("RECURSIVE_FOLDER_COPY source is no longer a folder");
		}

		if (Boolean.TRUE.equals(sourceRoot.trashed())) {
			throw new IllegalArgumentException("Source folder is currently in Google Drive trash");
		}

		driveOperationCapabilityGuard.requireAllowed(
				sourceRoot.capabilities() != null ? sourceRoot.capabilities().canListChildren() : null, "COPY_FOLDER",
				"Cannot read the source folder hierarchy");

		GoogleDriveFileResponse destinationParent = googleDriveFileMutationService
			.getFile(job.destinationConnectionId(), job.userId(), job.destinationParentGoogleFileId());

		if (Boolean.TRUE.equals(destinationParent.trashed())) {
			throw new IllegalArgumentException("Destination folder is currently in Google Drive trash");
		}

		driveOperationCapabilityGuard.requireAllowed(
				destinationParent.capabilities() != null ? destinationParent.capabilities().canAddChildren() : null,
				"COPY_FOLDER", "Cannot create the copied folder in the requested destination");
	}

	private void verifyDestinationItem(DriveOperationJobExecutionSnapshot job, DriveOperationItemExecutionSnapshot item,
			String expectedMarker, GoogleDriveFileResponse destinationFile) {

		if (destinationFile == null || destinationFile.id() == null || destinationFile.id().isBlank()) {
			throw new DriveOperationCleanupRequiredException("RECURSIVE_DESTINATION_MISSING",
					"Copied destination item could not be retrieved");
		}

		if (Boolean.TRUE.equals(destinationFile.trashed())) {
			throw new DriveOperationCleanupRequiredException("RECURSIVE_DESTINATION_TRASHED",
					"Copied destination item is unexpectedly in trash");
		}

		if (!hasParent(destinationFile.parents(), item.destinationParentGoogleFileId())) {
			throw new DriveOperationCleanupRequiredException("RECURSIVE_PARENT_MISMATCH",
					"Copied destination item is not located under its expected destination parent");
		}

		String actualMarker = destinationFile.appProperties() != null
				? destinationFile.appProperties().get(DriveOperationMarkerFactory.APP_PROPERTY_KEY) : null;

		if (!expectedMarker.equals(actualMarker)) {
			throw new DriveOperationCleanupRequiredException("RECURSIVE_MARKER_MISMATCH",
					"Copied destination item does not contain the expected operation marker");
		}

		if (job.destinationSourceType() == GoogleDriveSourceType.SHARED_DRIVE && job.destinationGoogleDriveId() != null
				&& !job.destinationGoogleDriveId().equals(destinationFile.driveId())) {
			throw new DriveOperationCleanupRequiredException("RECURSIVE_SHARED_DRIVE_MISMATCH",
					"Copied destination item was created in an unexpected Shared Drive");
		}
	}

	private void markItemRunning(DriveOperationJobExecutionSnapshot job, DriveOperationItemExecutionSnapshot item) {

		boolean updated = driveOperationJobExecutionStore.markOperationItemRunning(job.jobId(), item.id(), now());
		if (!updated) {
			throw new DriveOperationLeaseLostException(job.jobId());
		}
	}

	private void markMutationStarted(DriveOperationJobExecutionSnapshot job, DriveOperationItemExecutionSnapshot item) {

		boolean updated = driveOperationJobExecutionStore.markOperationItemMutationStarted(job.jobId(), item.id(),
				now());
		if (!updated) {
			throw new DriveOperationLeaseLostException(job.jobId());
		}
	}

	private void markItemVerifying(DriveOperationJobExecutionSnapshot job, DriveOperationItemExecutionSnapshot item) {

		boolean updated = driveOperationJobExecutionStore.markOperationItemVerifying(job.jobId(), item.id(), now());
		if (!updated) {
			throw new DriveOperationLeaseLostException(job.jobId());
		}
	}

	private String resolveDestinationName(DriveOperationJobExecutionSnapshot job,
			DriveOperationItemExecutionSnapshot item) {

		if (item.sequenceNo() == 0 && job.requestedName() != null && !job.requestedName().isBlank()) {
			return job.requestedName();
		}

		if (item.sourceName() == null || item.sourceName().isBlank()) {
			return "Untitled";
		}

		return item.sourceName();
	}

	private GoogleDriveItemSourceType resolveDestinationItemSourceType(GoogleDriveSourceType sourceType) {

		return sourceType == GoogleDriveSourceType.SHARED_DRIVE ? GoogleDriveItemSourceType.SHARED_DRIVE
				: GoogleDriveItemSourceType.MY_DRIVE;
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
			throw new IllegalArgumentException("Recursive folder executor only accepts COPY jobs");
		}

		if (job.strategyType() != DriveOperationStrategyType.RECURSIVE_FOLDER_COPY) {
			throw new IllegalArgumentException("Invalid strategy for recursive folder copy executor");
		}

		if (!job.sourceSourceId().equals(job.destinationSourceId())) {
			throw new IllegalArgumentException(
					"This recursive copy executor currently supports same-source folder copies only");
		}

		if (!job.sourceConnectionId().equals(job.destinationConnectionId())) {
			throw new IllegalArgumentException(
					"This recursive copy executor currently requires the same Google connection");
		}

		if (!GOOGLE_FOLDER_MIME_TYPE.equals(job.sourceMimeType())) {
			throw new IllegalArgumentException("RECURSIVE_FOLDER_COPY requires a folder source");
		}
	}

	private LocalDateTime now() {

		return LocalDateTime.now(ZoneOffset.UTC);
	}

}
