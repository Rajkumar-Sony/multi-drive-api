package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.dto.DriveOperationJobSubmitRequest;
import com.multidrive.api.dto.DriveOperationJobsPageResponse;
import com.multidrive.api.entity.DriveConflictStrategy;
import com.multidrive.api.entity.DriveOperationItem;
import com.multidrive.api.entity.DriveOperationItemStatus;
import com.multidrive.api.entity.DriveOperationJob;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.exception.DriveOperationJobNotFoundException;
import com.multidrive.api.exception.DriveOperationPlanningException;
import com.multidrive.api.model.DriveOperationPlan;
import com.multidrive.api.repository.DriveOperationItemRepository;
import com.multidrive.api.repository.DriveOperationJobRepository;
import com.multidrive.api.repository.GoogleDriveItemRepository;
import com.multidrive.api.repository.GoogleDriveSourceRepository;
import com.multidrive.api.service.DriveOperationJobService;
import com.multidrive.api.service.DriveOperationPlanner;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class DriveOperationJobServiceImpl implements DriveOperationJobService {

	private static final int DEFAULT_PAGE = 0;

	private static final int DEFAULT_PAGE_SIZE = 20;

	private static final int MAX_PAGE_SIZE = 100;

	private static final int DEFAULT_MAX_ATTEMPTS = 3;

	private static final String ROOT_FOLDER_ALIAS = "root";

	private final DriveOperationJobRepository driveOperationJobRepository;

	private final DriveOperationItemRepository driveOperationItemRepository;

	private final GoogleDriveItemRepository googleDriveItemRepository;

	private final GoogleDriveSourceRepository googleDriveSourceRepository;

	private final DriveOperationPlanner driveOperationPlanner;

	private final TransactionTemplate transactionTemplate;

	public DriveOperationJobServiceImpl(DriveOperationJobRepository driveOperationJobRepository,
			DriveOperationItemRepository driveOperationItemRepository,
			GoogleDriveItemRepository googleDriveItemRepository,
			GoogleDriveSourceRepository googleDriveSourceRepository, DriveOperationPlanner driveOperationPlanner,
			PlatformTransactionManager transactionManager) {

		this.driveOperationJobRepository = driveOperationJobRepository;

		this.driveOperationItemRepository = driveOperationItemRepository;

		this.googleDriveItemRepository = googleDriveItemRepository;

		this.googleDriveSourceRepository = googleDriveSourceRepository;

		this.driveOperationPlanner = driveOperationPlanner;

		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@Override
	public DriveOperationJobResponse submit(String googleSubjectId, String idempotencyKey,
			DriveOperationJobSubmitRequest request) {

		validateGoogleSubjectId(googleSubjectId);

		validateSubmitRequest(request);

		String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);

		if (normalizedIdempotencyKey != null) {

			DriveOperationJobResponse existing = findExistingIdempotentJob(googleSubjectId, normalizedIdempotencyKey);

			if (existing != null) {
				return existing;
			}
		}

		try {

			Long jobId = transactionTemplate
				.execute(status -> createJob(googleSubjectId, normalizedIdempotencyKey, request));

			if (jobId == null) {

				throw new IllegalStateException("Drive operation job was not created");
			}

			return getJob(googleSubjectId, jobId);

		}
		catch (DataIntegrityViolationException exception) {

			if (normalizedIdempotencyKey == null) {
				throw exception;
			}

			DriveOperationJobResponse existing = findExistingIdempotentJob(googleSubjectId, normalizedIdempotencyKey);

			if (existing != null) {
				return existing;
			}

			throw exception;
		}
	}

	@Override
	public DriveOperationJobResponse getJob(String googleSubjectId, Long jobId) {

		validateGoogleSubjectId(googleSubjectId);

		if (jobId == null) {

			throw new IllegalArgumentException("jobId is required");
		}

		return driveOperationJobRepository.findResponseByIdAndGoogleSubjectId(jobId, googleSubjectId)
			.orElseThrow(() -> new DriveOperationJobNotFoundException(jobId));
	}

	@Override
	public DriveOperationJobsPageResponse getJobs(String googleSubjectId, DriveOperationJobStatus status, Integer page,
			Integer size) {

		validateGoogleSubjectId(googleSubjectId);

		Pageable pageable = createPageable(page, size);

		Page<DriveOperationJobResponse> result = status == null
				? driveOperationJobRepository.findResponsesByGoogleSubjectId(googleSubjectId, pageable)
				: driveOperationJobRepository.findResponsesByGoogleSubjectIdAndStatus(googleSubjectId, status,
						pageable);

		return new DriveOperationJobsPageResponse(result.getContent(), result.getNumber(), result.getSize(),
				result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
	}

	@Override
	public DriveOperationJobResponse cancel(String googleSubjectId, Long jobId) {

		validateGoogleSubjectId(googleSubjectId);

		if (jobId == null) {

			throw new IllegalArgumentException("jobId is required");
		}

		transactionTemplate.executeWithoutResult(status -> {

			DriveOperationJob job = driveOperationJobRepository.findByIdAndUser_GoogleSubjectId(jobId, googleSubjectId)
				.orElseThrow(() -> new DriveOperationJobNotFoundException(jobId));

			if (isTerminal(job.getStatus())) {
				return;
			}

			job.setCancelRequested(true);

			if (job.getStatus() == DriveOperationJobStatus.QUEUED) {

				job.setStatus(DriveOperationJobStatus.CANCELLED);

				job.setCompletedAt(LocalDateTime.now(ZoneOffset.UTC));
			}

			driveOperationJobRepository.save(job);
		});

		return getJob(googleSubjectId, jobId);
	}

	private Long createJob(String googleSubjectId, String idempotencyKey, DriveOperationJobSubmitRequest request) {

		GoogleDriveItem sourceItem = requireOwnedItem(googleSubjectId, request.sourceItemId());

		if (sourceItem.isTrashed()) {

			throw new IllegalArgumentException("Cannot submit a job for an item in trash");
		}

		GoogleDriveConnection sourceConnection = requireConnection(sourceItem);

		GoogleDriveSource source = requireSource(sourceItem);

		Destination destination = resolveDestination(googleSubjectId, request.destinationSourceId(),
				request.destinationParentItemId());

		DriveOperationPlan plan = planOperation(request.operationType(), sourceItem, destination.source());

		validateMoveHierarchy(request.operationType(), sourceItem, destination);

		DriveConflictStrategy conflictStrategy = request.conflictStrategy() == null ? DriveConflictStrategy.KEEP_BOTH
				: request.conflictStrategy();

		String requestedName = normalizeOptionalName(request.name());

		DriveOperationJob job = new DriveOperationJob();

		job.setUser(sourceConnection.getUser());
		job.setOperationType(request.operationType());
		job.setStrategyType(plan.strategyType());
		job.setStatus(DriveOperationJobStatus.QUEUED);
		job.setConflictStrategy(conflictStrategy);
		job.setSourceItemId(sourceItem.getId());
		job.setSourceConnectionId(sourceConnection.getId());
		job.setSourceSourceId(source.getId());
		job.setSourceGoogleFileId(sourceItem.getGoogleFileId());
		job.setSourceName(sourceItem.getName());
		job.setSourceMimeType(sourceItem.getMimeType());
		job.setDestinationSourceId(destination.source().getId());
		job.setDestinationConnectionId(requireConnection(destination.source()).getId());
		job.setDestinationParentItemId(destination.parentItemId());
		job.setDestinationParentGoogleFileId(destination.parentGoogleFileId());
		job.setRequestedName(requestedName);
		job.setTotalItems(1L);
		job.setCompletedItems(0L);
		job.setFailedItems(0L);
		job.setTotalBytes(sourceItem.getSizeBytes());
		job.setTransferredBytes(0L);
		job.setAttemptCount(0);
		job.setMaxAttempts(DEFAULT_MAX_ATTEMPTS);
		job.setCancelRequested(false);
		job.setIdempotencyKey(idempotencyKey);

		DriveOperationJob savedJob = driveOperationJobRepository.saveAndFlush(job);

		DriveOperationItem operationItem = new DriveOperationItem();

		operationItem.setJob(savedJob);
		operationItem.setSequenceNo(0);
		operationItem.setSourceLocalItemId(sourceItem.getId());
		operationItem.setSourceGoogleFileId(sourceItem.getGoogleFileId());
		operationItem.setSourceName(sourceItem.getName());
		operationItem.setSourceMimeType(sourceItem.getMimeType());
		operationItem.setSourceParentGoogleFileId(sourceItem.getParentId());
		operationItem.setDestinationParentGoogleFileId(destination.parentGoogleFileId());
		operationItem.setStatus(DriveOperationItemStatus.QUEUED);
		operationItem.setSizeBytes(sourceItem.getSizeBytes());
		operationItem.setTransferredBytes(0L);
		operationItem.setAttemptCount(0);

		driveOperationItemRepository.save(operationItem);

		return savedJob.getId();
	}

	private DriveOperationJobResponse findExistingIdempotentJob(String googleSubjectId, String idempotencyKey) {

		return driveOperationJobRepository
			.findResponseByGoogleSubjectIdAndIdempotencyKey(googleSubjectId, idempotencyKey)
			.orElse(null);
	}

	private Destination resolveDestination(String googleSubjectId, Long destinationSourceId,
			Long destinationParentItemId) {

		if (destinationParentItemId == null) {

			GoogleDriveSource source = requireOwnedActiveSource(googleSubjectId, destinationSourceId);

			return new Destination(null, requireRootFolderId(source), source, null);
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

		return new Destination(parent.getId(), parent.getGoogleFileId(), source, parent);
	}

	private DriveOperationPlan planOperation(DriveOperationType operationType, GoogleDriveItem sourceItem,
			GoogleDriveSource destinationSource) {

		DriveOperationPlan plan = operationType == DriveOperationType.MOVE
				? driveOperationPlanner.planMove(sourceItem, destinationSource)
				: driveOperationPlanner.planCopy(sourceItem, destinationSource);

		if (plan == null || plan.strategyType() == null) {

			throw new DriveOperationPlanningException("Drive operation planner returned no strategy");
		}

		return plan;
	}

	private void validateMoveHierarchy(DriveOperationType operationType, GoogleDriveItem sourceItem,
			Destination destination) {

		if (operationType != DriveOperationType.MOVE || sourceItem.getCategory() != GoogleDriveItemCategory.FOLDER
				|| destination.parentItem() == null
				|| !sourceItem.getSource().getId().equals(destination.source().getId())) {

			return;
		}

		GoogleDriveItem destinationParent = destination.parentItem();

		if (sourceItem.getId().equals(destinationParent.getId())) {

			throw new IllegalArgumentException("A folder cannot be moved into itself");
		}

		boolean descendant = googleDriveItemRepository.isDescendant(sourceItem.getConnection().getId(),
				sourceItem.getSource().getId(), sourceItem.getGoogleFileId(), destinationParent.getGoogleFileId());

		if (descendant) {

			throw new IllegalArgumentException("A folder cannot be moved into one of its descendants");
		}
	}

	private GoogleDriveSource requireOwnedActiveSource(String googleSubjectId, Long sourceId) {

		if (sourceId == null) {

			throw new IllegalArgumentException("destinationSourceId is required");
		}

		return googleDriveSourceRepository
			.findOwnedSourceForOperation(sourceId, googleSubjectId, GoogleDriveSourceStatus.ACTIVE)
			.orElseThrow(() -> new IllegalArgumentException("Active Google Drive source not found"));
	}

	private GoogleDriveItem requireOwnedItem(String googleSubjectId, Long itemId) {

		if (itemId == null) {

			throw new IllegalArgumentException("sourceItemId is required");
		}

		return googleDriveItemRepository.findOwnedItemForDetails(itemId, googleSubjectId)
			.orElseThrow(() -> new DriveItemNotFoundException(itemId));
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

	private String requireRootFolderId(GoogleDriveSource source) {

		if (source.getRootFolderId() == null || source.getRootFolderId().isBlank()) {

			return ROOT_FOLDER_ALIAS;
		}

		return source.getRootFolderId();
	}

	private Pageable createPageable(Integer page, Integer size) {

		int normalizedPage = page == null ? DEFAULT_PAGE : page;

		int normalizedSize = size == null ? DEFAULT_PAGE_SIZE : size;

		if (normalizedPage < 0) {

			throw new IllegalArgumentException("page must be greater than or equal to 0");
		}

		if (normalizedSize < 1 || normalizedSize > MAX_PAGE_SIZE) {

			throw new IllegalArgumentException("size must be between 1 and 100");
		}

		return PageRequest.of(normalizedPage, normalizedSize,
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
	}

	private String normalizeIdempotencyKey(String idempotencyKey) {

		if (idempotencyKey == null) {
			return null;
		}

		String normalized = idempotencyKey.trim();

		if (normalized.isEmpty()) {
			return null;
		}

		if (normalized.length() > 128) {

			throw new IllegalArgumentException("Idempotency-Key must not exceed 128 characters");
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

	private boolean isTerminal(DriveOperationJobStatus status) {

		return status == DriveOperationJobStatus.COMPLETED || status == DriveOperationJobStatus.PARTIAL
				|| status == DriveOperationJobStatus.FAILED || status == DriveOperationJobStatus.CANCELLED
				|| status == DriveOperationJobStatus.CLEANUP_REQUIRED;
	}

	private void validateSubmitRequest(DriveOperationJobSubmitRequest request) {

		if (request == null) {

			throw new IllegalArgumentException("request is required");
		}

		if (request.operationType() == null) {

			throw new IllegalArgumentException("operationType is required");
		}

		if (request.sourceItemId() == null) {

			throw new IllegalArgumentException("sourceItemId is required");
		}

		if (request.destinationSourceId() == null) {

			throw new IllegalArgumentException("destinationSourceId is required");
		}
	}

	private void validateGoogleSubjectId(String googleSubjectId) {

		if (googleSubjectId == null || googleSubjectId.isBlank()) {

			throw new IllegalArgumentException("googleSubjectId is required");
		}
	}

	private record Destination(

			Long parentItemId,

			String parentGoogleFileId,

			GoogleDriveSource source,

			GoogleDriveItem parentItem) {
	}

}
