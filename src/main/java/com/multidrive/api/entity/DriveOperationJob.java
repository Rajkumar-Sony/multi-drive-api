package com.multidrive.api.entity;

import com.multidrive.api.model.DriveOperationStrategyType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.Getter;

@Entity
@Table(name = "drive_operation_jobs")
@Getter
public class DriveOperationJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(name = "operation_type", nullable = false, length = 30)
	private DriveOperationType operationType;

	@Enumerated(EnumType.STRING)
	@Column(name = "strategy_type", nullable = false, length = 50)
	private DriveOperationStrategyType strategyType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private DriveOperationJobStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "conflict_strategy", nullable = false, length = 30)
	private DriveConflictStrategy conflictStrategy;

	@Column(name = "source_item_id", nullable = false)
	private Long sourceItemId;

	@Column(name = "source_connection_id", nullable = false)
	private Long sourceConnectionId;

	@Column(name = "source_source_id", nullable = false)
	private Long sourceSourceId;

	@Column(name = "source_google_file_id", nullable = false, length = 255)
	private String sourceGoogleFileId;

	@Column(name = "source_name", nullable = false, columnDefinition = "TEXT")
	private String sourceName;

	@Column(name = "source_mime_type", length = 255)
	private String sourceMimeType;

	@Column(name = "destination_source_id", nullable = false)
	private Long destinationSourceId;

	@Column(name = "destination_connection_id", nullable = false)
	private Long destinationConnectionId;

	@Column(name = "destination_parent_item_id")
	private Long destinationParentItemId;

	@Column(name = "destination_parent_google_file_id", nullable = false, length = 255)
	private String destinationParentGoogleFileId;

	@Column(name = "requested_name", columnDefinition = "TEXT")
	private String requestedName;

	@Column(name = "result_item_id")
	private Long resultItemId;

	@Column(name = "result_google_file_id", length = 255)
	private String resultGoogleFileId;

	@Column(name = "total_items", nullable = false)
	private Long totalItems;

	@Column(name = "completed_items", nullable = false)
	private Long completedItems;

	@Column(name = "failed_items", nullable = false)
	private Long failedItems;

	@Column(name = "total_bytes")
	private Long totalBytes;

	@Column(name = "transferred_bytes", nullable = false)
	private Long transferredBytes;

	@Column(name = "attempt_count", nullable = false)
	private Integer attemptCount;

	@Column(name = "max_attempts", nullable = false)
	private Integer maxAttempts;

	@Column(name = "next_attempt_at")
	private LocalDateTime nextAttemptAt;

	@Column(name = "cancel_requested", nullable = false)
	private Boolean cancelRequested;

	@Column(name = "idempotency_key", length = 128)
	private String idempotencyKey;

	@Column(name = "worker_id", length = 64)
	private String workerId;

	@Column(name = "lease_expires_at")
	private LocalDateTime leaseExpiresAt;

	@Column(name = "last_heartbeat_at")
	private LocalDateTime lastHeartbeatAt;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	@PrePersist
	public void prePersist() {

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		if (status == null) {
			status = DriveOperationJobStatus.QUEUED;
		}

		if (conflictStrategy == null) {
			conflictStrategy = DriveConflictStrategy.KEEP_BOTH;
		}

		if (totalItems == null) {
			totalItems = 1L;
		}

		if (completedItems == null) {
			completedItems = 0L;
		}

		if (failedItems == null) {
			failedItems = 0L;
		}

		if (transferredBytes == null) {
			transferredBytes = 0L;
		}

		if (attemptCount == null) {
			attemptCount = 0;
		}

		if (maxAttempts == null) {
			maxAttempts = 3;
		}

		if (cancelRequested == null) {
			cancelRequested = false;
		}

		if (createdAt == null) {
			createdAt = now;
		}

		updatedAt = now;
	}

	@PreUpdate
	public void preUpdate() {

		updatedAt = LocalDateTime.now(ZoneOffset.UTC);
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setOperationType(DriveOperationType operationType) {
		this.operationType = operationType;
	}

	public void setStrategyType(DriveOperationStrategyType strategyType) {
		this.strategyType = strategyType;
	}

	public void setStatus(DriveOperationJobStatus status) {
		this.status = status;
	}

	public void setConflictStrategy(DriveConflictStrategy conflictStrategy) {
		this.conflictStrategy = conflictStrategy;
	}

	public void setSourceItemId(Long sourceItemId) {
		this.sourceItemId = sourceItemId;
	}

	public void setSourceConnectionId(Long sourceConnectionId) {
		this.sourceConnectionId = sourceConnectionId;
	}

	public void setSourceSourceId(Long sourceSourceId) {
		this.sourceSourceId = sourceSourceId;
	}

	public void setSourceGoogleFileId(String sourceGoogleFileId) {
		this.sourceGoogleFileId = sourceGoogleFileId;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public void setSourceMimeType(String sourceMimeType) {
		this.sourceMimeType = sourceMimeType;
	}

	public void setDestinationSourceId(Long destinationSourceId) {
		this.destinationSourceId = destinationSourceId;
	}

	public void setDestinationConnectionId(Long destinationConnectionId) {
		this.destinationConnectionId = destinationConnectionId;
	}

	public void setDestinationParentItemId(Long destinationParentItemId) {
		this.destinationParentItemId = destinationParentItemId;
	}

	public void setDestinationParentGoogleFileId(String destinationParentGoogleFileId) {
		this.destinationParentGoogleFileId = destinationParentGoogleFileId;
	}

	public void setRequestedName(String requestedName) {
		this.requestedName = requestedName;
	}

	public void setResultItemId(Long resultItemId) {
		this.resultItemId = resultItemId;
	}

	public void setResultGoogleFileId(String resultGoogleFileId) {
		this.resultGoogleFileId = resultGoogleFileId;
	}

	public void setTotalItems(Long totalItems) {
		this.totalItems = totalItems;
	}

	public void setCompletedItems(Long completedItems) {
		this.completedItems = completedItems;
	}

	public void setFailedItems(Long failedItems) {
		this.failedItems = failedItems;
	}

	public void setTotalBytes(Long totalBytes) {
		this.totalBytes = totalBytes;
	}

	public void setTransferredBytes(Long transferredBytes) {
		this.transferredBytes = transferredBytes;
	}

	public void setAttemptCount(Integer attemptCount) {
		this.attemptCount = attemptCount;
	}

	public void setMaxAttempts(Integer maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public void setNextAttemptAt(LocalDateTime nextAttemptAt) {
		this.nextAttemptAt = nextAttemptAt;
	}

	public void setCancelRequested(Boolean cancelRequested) {
		this.cancelRequested = cancelRequested;
	}

	public void setIdempotencyKey(String idempotencyKey) {
		this.idempotencyKey = idempotencyKey;
	}

	public void setWorkerId(String workerId) {
		this.workerId = workerId;
	}

	public void setLeaseExpiresAt(LocalDateTime leaseExpiresAt) {
		this.leaseExpiresAt = leaseExpiresAt;
	}

	public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) {
		this.lastHeartbeatAt = lastHeartbeatAt;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setStartedAt(LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

}
