package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveCopyRequest;
import com.multidrive.api.dto.DriveCreateFolderRequest;
import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveMoveRequest;
import com.multidrive.api.dto.DriveRenameRequest;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.exception.DriveOperationPlanningException;
import com.multidrive.api.model.DriveOperationPlan;
import com.multidrive.api.model.DriveOperationStrategyType;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.DriveItemLookupService;
import com.multidrive.api.service.DriveOperationCapabilityGuard;
import com.multidrive.api.service.DriveOperationLocalStateService;
import com.multidrive.api.service.DriveOperationPlanner;
import com.multidrive.api.service.DriveOperationService;
import com.multidrive.api.service.GoogleDriveFileMutationService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriveOperationServiceImpl implements DriveOperationService {

	private final GoogleDriveItemRepository googleDriveItemRepository;

	private final GoogleDriveSourceRepository googleDriveSourceRepository;

	private final GoogleDriveFileMutationService googleDriveFileMutationService;

	private final DriveOperationCapabilityGuard driveOperationCapabilityGuard;

	private final DriveOperationLocalStateService driveOperationLocalStateService;

	private final DriveItemLookupService driveItemLookupService;

	private final DriveOperationPlanner driveOperationPlanner;

	public DriveOperationServiceImpl(GoogleDriveItemRepository googleDriveItemRepository,
			GoogleDriveSourceRepository googleDriveSourceRepository,
			GoogleDriveFileMutationService googleDriveFileMutationService,
			DriveOperationCapabilityGuard driveOperationCapabilityGuard,
			DriveOperationLocalStateService driveOperationLocalStateService,
			DriveItemLookupService driveItemLookupService, DriveOperationPlanner driveOperationPlanner) {

		this.googleDriveItemRepository = googleDriveItemRepository;

		this.googleDriveSourceRepository = googleDriveSourceRepository;

		this.googleDriveFileMutationService = googleDriveFileMutationService;

		this.driveOperationCapabilityGuard = driveOperationCapabilityGuard;

		this.driveOperationLocalStateService = driveOperationLocalStateService;

		this.driveItemLookupService = driveItemLookupService;

		this.driveOperationPlanner = driveOperationPlanner;
	}

	@Override
	public DriveItemDetailsResponse createFolder(String googleSubjectId, DriveCreateFolderRequest request) {

		validateGoogleSubjectId(googleSubjectId);

		if (request == null) {

			throw new IllegalArgumentException("request is required");
		}

		if (request.sourceId() == null) {

			throw new IllegalArgumentException("sourceId is required");
		}

		String normalizedName = normalizeName(request.name());

		if (request.parentItemId() == null) {

			return createFolderInSourceRoot(googleSubjectId, request.sourceId(), normalizedName);
		}

		return createFolderInsideFolder(googleSubjectId, request.sourceId(), request.parentItemId(), normalizedName);
	}

	@Override
	public DriveItemDetailsResponse rename(String googleSubjectId, Long itemId, DriveRenameRequest request) {

		validateGoogleSubjectId(googleSubjectId);

		if (request == null) {

			throw new IllegalArgumentException("request is required");
		}

		String normalizedName = normalizeName(request.name());

		GoogleDriveItem item = requireOwnedItem(googleSubjectId, itemId);

		GoogleDriveConnection connection = requireConnection(item);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteFile = googleDriveFileMutationService.getFile(connection.getId(), userId,
				item.getGoogleFileId());

		driveOperationCapabilityGuard.requireAllowed(
				currentRemoteFile.capabilities() != null ? currentRemoteFile.capabilities().canRename() : null,
				"RENAME");

		GoogleDriveFileResponse updatedRemoteFile = googleDriveFileMutationService.rename(connection.getId(), userId,
				item.getGoogleFileId(), normalizedName);

		driveOperationLocalStateService.updateItem(item.getId(), updatedRemoteFile);

		return driveItemLookupService.getItem(googleSubjectId, item.getId());
	}

	@Override
	public DriveItemDetailsResponse move(String googleSubjectId, Long itemId, DriveMoveRequest request) {

		validateGoogleSubjectId(googleSubjectId);

		if (request == null) {

			throw new IllegalArgumentException("request is required");
		}

		if (request.destinationSourceId() == null) {

			throw new IllegalArgumentException("destinationSourceId is required");
		}

		GoogleDriveItem item = requireOwnedItem(googleSubjectId, itemId);

		if (item.isTrashed()) {

			throw new IllegalArgumentException("Cannot move an item while it is in trash");
		}

		DestinationContext destination = resolveDestination(googleSubjectId, request.destinationSourceId(),
				request.destinationParentItemId());

		DriveOperationPlan plan = driveOperationPlanner.planMove(item, destination.source());

		requireNativeStrategy(plan, DriveOperationStrategyType.NATIVE_MOVE);

		validateFolderMoveHierarchy(item, destination);

		GoogleDriveConnection connection = requireConnection(item);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteFile = googleDriveFileMutationService.getFile(connection.getId(), userId,
				item.getGoogleFileId());

		if (Boolean.TRUE.equals(currentRemoteFile.trashed())) {

			throw new IllegalArgumentException("Cannot move an item while it is in Google Drive trash");
		}

		if (hasParent(currentRemoteFile.parents(), destination.parentGoogleFileId())) {

			driveOperationLocalStateService.updateItem(item.getId(), currentRemoteFile);

			return driveItemLookupService.getItem(googleSubjectId, item.getId());
		}

		driveOperationCapabilityGuard.requireAllowed(currentRemoteFile.capabilities() != null
				? currentRemoteFile.capabilities().canMoveItemWithinDrive() : null, "MOVE",
				"Google does not currently allow this item to be moved within the Drive");

		GoogleDriveFileResponse remoteDestinationParent = googleDriveFileMutationService
			.getFile(destination.connection().getId(), destination.userId(), destination.parentGoogleFileId());

		if (Boolean.TRUE.equals(remoteDestinationParent.trashed())) {

			throw new IllegalArgumentException("Destination folder is in trash");
		}

		driveOperationCapabilityGuard
			.requireAllowed(
					remoteDestinationParent.capabilities() != null
							? remoteDestinationParent.capabilities().canAddChildren() : null,
					"MOVE", "Cannot add items to the destination folder");

		GoogleDriveFileResponse movedRemoteFile = googleDriveFileMutationService.move(connection.getId(), userId,
				item.getGoogleFileId(), destination.parentGoogleFileId(), currentRemoteFile.parents());

		driveOperationLocalStateService.updateItem(item.getId(), movedRemoteFile);

		return driveItemLookupService.getItem(googleSubjectId, item.getId());
	}

	@Override
	public DriveItemDetailsResponse copy(String googleSubjectId, Long itemId, DriveCopyRequest request) {

		validateGoogleSubjectId(googleSubjectId);

		if (request == null) {

			throw new IllegalArgumentException("request is required");
		}

		if (request.destinationSourceId() == null) {

			throw new IllegalArgumentException("destinationSourceId is required");
		}

		GoogleDriveItem item = requireOwnedItem(googleSubjectId, itemId);

		if (item.isTrashed()) {

			throw new IllegalArgumentException("Cannot copy an item while it is in trash");
		}

		DestinationContext destination = resolveDestination(googleSubjectId, request.destinationSourceId(),
				request.destinationParentItemId());

		DriveOperationPlan plan = driveOperationPlanner.planCopy(item, destination.source());

		requireNativeStrategy(plan, DriveOperationStrategyType.NATIVE_COPY);

		GoogleDriveConnection connection = requireConnection(item);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteFile = googleDriveFileMutationService.getFile(connection.getId(), userId,
				item.getGoogleFileId());

		if (Boolean.TRUE.equals(currentRemoteFile.trashed())) {

			throw new IllegalArgumentException("Cannot copy an item while it is in Google Drive trash");
		}

		driveOperationCapabilityGuard.requireAllowed(
				currentRemoteFile.capabilities() != null ? currentRemoteFile.capabilities().canCopy() : null, "COPY");

		GoogleDriveFileResponse remoteDestinationParent = googleDriveFileMutationService
			.getFile(destination.connection().getId(), destination.userId(), destination.parentGoogleFileId());

		if (Boolean.TRUE.equals(remoteDestinationParent.trashed())) {

			throw new IllegalArgumentException("Destination folder is in trash");
		}

		driveOperationCapabilityGuard
			.requireAllowed(
					remoteDestinationParent.capabilities() != null
							? remoteDestinationParent.capabilities().canAddChildren() : null,
					"COPY", "Cannot add items to the destination folder");

		String copyName = normalizeOptionalName(request.name());

		GoogleDriveFileResponse copiedRemoteFile = googleDriveFileMutationService.copy(connection.getId(), userId,
				item.getGoogleFileId(), destination.parentGoogleFileId(), copyName);

		GoogleDriveItemSourceType destinationSourceType = resolveItemSourceType(destination.source());

		String destinationDriveId = destinationSourceType == GoogleDriveItemSourceType.SHARED_DRIVE
				? destination.source().getGoogleDriveId() : null;

		Long copiedItemId = driveOperationLocalStateService.createCopiedItem(destination.connection().getId(),
				destination.source().getId(), destinationSourceType, destinationDriveId, copiedRemoteFile);

		return driveItemLookupService.getItem(googleSubjectId, copiedItemId);
	}

	@Override
	public DriveItemDetailsResponse trash(String googleSubjectId, Long itemId) {

		validateGoogleSubjectId(googleSubjectId);

		GoogleDriveItem item = requireOwnedItem(googleSubjectId, itemId);

		GoogleDriveConnection connection = requireConnection(item);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteFile = googleDriveFileMutationService.getFile(connection.getId(), userId,
				item.getGoogleFileId());

		if (Boolean.TRUE.equals(currentRemoteFile.trashed())) {

			driveOperationLocalStateService.markTrashed(item.getId(), currentRemoteFile);

			return driveItemLookupService.getItem(googleSubjectId, item.getId());
		}

		driveOperationCapabilityGuard.requireAllowed(
				currentRemoteFile.capabilities() != null ? currentRemoteFile.capabilities().canTrash() : null, "TRASH");

		GoogleDriveFileResponse trashedRemoteFile = googleDriveFileMutationService.trash(connection.getId(), userId,
				item.getGoogleFileId());

		driveOperationLocalStateService.markTrashed(item.getId(), trashedRemoteFile);

		return driveItemLookupService.getItem(googleSubjectId, item.getId());
	}

	@Override
	public DriveItemDetailsResponse restore(String googleSubjectId, Long itemId) {

		validateGoogleSubjectId(googleSubjectId);

		GoogleDriveItem item = requireOwnedItem(googleSubjectId, itemId);

		GoogleDriveConnection connection = requireConnection(item);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteFile = googleDriveFileMutationService.getFile(connection.getId(), userId,
				item.getGoogleFileId());

		if (!Boolean.TRUE.equals(currentRemoteFile.trashed())) {

			driveOperationLocalStateService.markRestored(item.getId(), currentRemoteFile);

			return driveItemLookupService.getItem(googleSubjectId, item.getId());
		}

		driveOperationCapabilityGuard.requireAllowed(
				currentRemoteFile.capabilities() != null ? currentRemoteFile.capabilities().canUntrash() : null,
				"RESTORE");

		GoogleDriveFileResponse restoredRemoteFile = googleDriveFileMutationService.restore(connection.getId(), userId,
				item.getGoogleFileId());

		driveOperationLocalStateService.markRestored(item.getId(), restoredRemoteFile);

		return driveItemLookupService.getItem(googleSubjectId, item.getId());
	}

	@Override
	public void permanentlyDelete(String googleSubjectId, Long itemId) {

		validateGoogleSubjectId(googleSubjectId);

		GoogleDriveItem item = requireOwnedItem(googleSubjectId, itemId);

		GoogleDriveConnection connection = requireConnection(item);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteFile = googleDriveFileMutationService.getFile(connection.getId(), userId,
				item.getGoogleFileId());

		driveOperationCapabilityGuard.requireAllowed(
				currentRemoteFile.capabilities() != null ? currentRemoteFile.capabilities().canDelete() : null,
				"PERMANENT_DELETE");

		googleDriveFileMutationService.permanentlyDelete(connection.getId(), userId, item.getGoogleFileId());

		driveOperationLocalStateService.deleteSubtree(item.getId());
	}

	private DriveItemDetailsResponse createFolderInSourceRoot(String googleSubjectId, Long sourceId, String name) {

		GoogleDriveSource source = requireOwnedActiveSource(googleSubjectId, sourceId);

		GoogleDriveConnection connection = requireConnection(source);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteParent = googleDriveFileMutationService.getFile(connection.getId(), userId,
				requireRootFolderId(source));

		driveOperationCapabilityGuard.requireAllowed(
				currentRemoteParent.capabilities() != null ? currentRemoteParent.capabilities().canAddChildren() : null,
				"CREATE_FOLDER");

		GoogleDriveFileResponse createdRemoteFile = googleDriveFileMutationService.createFolder(connection.getId(),
				userId, source.getRootFolderId(), name);

		GoogleDriveItemSourceType sourceType = resolveItemSourceType(source);

		String driveId = sourceType == GoogleDriveItemSourceType.SHARED_DRIVE ? source.getGoogleDriveId() : null;

		Long createdItemId = driveOperationLocalStateService.createFolder(connection.getId(), source.getId(),
				sourceType, driveId, createdRemoteFile);

		return driveItemLookupService.getItem(googleSubjectId, createdItemId);
	}

	private DriveItemDetailsResponse createFolderInsideFolder(String googleSubjectId, Long sourceId, Long parentItemId,
			String name) {

		GoogleDriveItem parentItem = requireOwnedItem(googleSubjectId, parentItemId);

		if (parentItem.getCategory() != GoogleDriveItemCategory.FOLDER) {

			throw new IllegalArgumentException("parentItemId must reference a folder");
		}

		if (parentItem.isTrashed()) {

			throw new IllegalArgumentException("Cannot create a folder inside a trashed folder");
		}

		GoogleDriveSource source = requireSource(parentItem);

		if (!source.getId().equals(sourceId)) {

			throw new IllegalArgumentException("parentItemId does not belong to sourceId");
		}

		GoogleDriveConnection connection = requireConnection(parentItem);

		Long userId = requireUserId(connection);

		GoogleDriveFileResponse currentRemoteParent = googleDriveFileMutationService.getFile(connection.getId(), userId,
				parentItem.getGoogleFileId());

		driveOperationCapabilityGuard.requireAllowed(
				currentRemoteParent.capabilities() != null ? currentRemoteParent.capabilities().canAddChildren() : null,
				"CREATE_FOLDER");

		GoogleDriveFileResponse createdRemoteFile = googleDriveFileMutationService.createFolder(connection.getId(),
				userId, parentItem.getGoogleFileId(), name);

		GoogleDriveItemSourceType sourceType = resolveItemSourceType(source);

		String driveId = sourceType == GoogleDriveItemSourceType.SHARED_DRIVE ? source.getGoogleDriveId() : null;

		Long createdItemId = driveOperationLocalStateService.createFolder(connection.getId(), source.getId(),
				sourceType, driveId, createdRemoteFile);

		return driveItemLookupService.getItem(googleSubjectId, createdItemId);
	}

	private DestinationContext resolveDestination(String googleSubjectId, Long destinationSourceId,
			Long destinationParentItemId) {

		if (destinationParentItemId == null) {

			GoogleDriveSource source = requireOwnedActiveSource(googleSubjectId, destinationSourceId);

			GoogleDriveConnection connection = requireConnection(source);

			Long userId = requireUserId(connection);

			return new DestinationContext(source, connection, userId, requireRootFolderId(source), null);
		}

		GoogleDriveItem parent = requireOwnedItem(googleSubjectId, destinationParentItemId);

		if (parent.getCategory() != GoogleDriveItemCategory.FOLDER) {

			throw new IllegalArgumentException("destinationParentItemId must reference a folder");
		}

		if (parent.isTrashed()) {

			throw new IllegalArgumentException("Destination folder is in trash");
		}

		GoogleDriveSource source = requireSource(parent);

		if (!source.getId().equals(destinationSourceId)) {

			throw new IllegalArgumentException("destinationParentItemId does not belong to destinationSourceId");
		}

		GoogleDriveConnection connection = requireConnection(parent);

		Long userId = requireUserId(connection);

		return new DestinationContext(source, connection, userId, parent.getGoogleFileId(), parent);
	}

	private void validateFolderMoveHierarchy(GoogleDriveItem sourceItem, DestinationContext destination) {

		if (sourceItem.getCategory() != GoogleDriveItemCategory.FOLDER) {

			return;
		}

		GoogleDriveItem destinationParent = destination.parentItem();

		if (destinationParent == null) {

			return;
		}

		if (sourceItem.getId().equals(destinationParent.getId())) {

			throw new IllegalArgumentException("A folder cannot be moved into itself");
		}

		boolean descendant = googleDriveItemRepository.isDescendant(sourceItem.getConnection().getId(),
				sourceItem.getSource().getId(), sourceItem.getGoogleFileId(), destinationParent.getGoogleFileId());

		if (descendant) {

			throw new IllegalArgumentException("A folder cannot be moved into one of its descendants");
		}
	}

	private void requireNativeStrategy(DriveOperationPlan plan, DriveOperationStrategyType expectedStrategy) {

		if (plan == null || plan.strategyType() == null) {

			throw new IllegalStateException("Drive operation planner returned no strategy");
		}

		if (plan.strategyType() != expectedStrategy) {

			throw new DriveOperationPlanningException(plan.reason());
		}
	}

	private GoogleDriveSource requireOwnedActiveSource(String googleSubjectId, Long sourceId) {

		if (sourceId == null) {

			throw new IllegalArgumentException("sourceId is required");
		}

		return googleDriveSourceRepository
			.findOwnedSourceForOperation(sourceId, googleSubjectId, GoogleDriveSourceStatus.ACTIVE)
			.orElseThrow(() -> new IllegalArgumentException("Active Google Drive source not found"));
	}

	private GoogleDriveItem requireOwnedItem(String googleSubjectId, Long itemId) {

		if (itemId == null) {

			throw new IllegalArgumentException("itemId is required");
		}

		return googleDriveItemRepository.findOwnedItemForDetails(itemId, googleSubjectId)
			.orElseThrow(() -> new DriveItemNotFoundException(itemId));
	}

	private GoogleDriveItemSourceType resolveItemSourceType(GoogleDriveSource source) {

		if (source.getSourceType() == GoogleDriveSourceType.SHARED_DRIVE) {

			return GoogleDriveItemSourceType.SHARED_DRIVE;
		}

		return GoogleDriveItemSourceType.MY_DRIVE;
	}

	private boolean hasParent(List<String> parents, String parentId) {

		if (parents == null || parents.isEmpty() || parentId == null) {

			return false;
		}

		return parents.stream().anyMatch(parentId::equals);
	}

	private GoogleDriveConnection requireConnection(GoogleDriveItem item) {

		if (item.getConnection() == null || item.getConnection().getId() == null) {

			throw new IllegalStateException("Drive item's Google connection is missing");
		}

		return item.getConnection();
	}

	private GoogleDriveConnection requireConnection(GoogleDriveSource source) {

		if (source.getConnection() == null || source.getConnection().getId() == null) {

			throw new IllegalStateException("Drive source's Google connection is missing");
		}

		return source.getConnection();
	}

	private GoogleDriveSource requireSource(GoogleDriveItem item) {

		if (item.getSource() == null || item.getSource().getId() == null) {

			throw new IllegalStateException("Drive item's source is missing");
		}

		return item.getSource();
	}

	private Long requireUserId(GoogleDriveConnection connection) {

		if (connection.getUser() == null || connection.getUser().getId() == null) {

			throw new IllegalStateException("Application user is missing");
		}

		return connection.getUser().getId();
	}

	private String requireRootFolderId(GoogleDriveSource source) {

		if (source.getRootFolderId() == null || source.getRootFolderId().isBlank()) {

			throw new IllegalStateException("Drive source root folder is missing");
		}

		return source.getRootFolderId();
	}

	private String normalizeName(String name) {

		if (name == null) {

			throw new IllegalArgumentException("name is required");
		}

		String normalized = name.trim();

		if (normalized.isEmpty()) {

			throw new IllegalArgumentException("name must not be blank");
		}

		if (normalized.length() > 255) {

			throw new IllegalArgumentException("name must not exceed 255 characters");
		}

		return normalized;
	}

	private String normalizeOptionalName(String name) {

		if (name == null) {
			return null;
		}

		String normalized = name.trim();

		if (normalized.isEmpty()) {
			return null;
		}

		if (normalized.length() > 255) {

			throw new IllegalArgumentException("name must not exceed 255 characters");
		}

		return normalized;
	}

	private void validateGoogleSubjectId(String googleSubjectId) {

		if (googleSubjectId == null || googleSubjectId.isBlank()) {

			throw new IllegalArgumentException("googleSubjectId is required");
		}
	}

	private record DestinationContext(

			GoogleDriveSource source,

			GoogleDriveConnection connection,

			Long userId,

			String parentGoogleFileId,

			GoogleDriveItem parentItem) {
	}

}
