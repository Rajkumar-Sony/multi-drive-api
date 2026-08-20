package com.multidrive.api.entity;

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

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.Getter;

@Entity
@Table(name = "drive_operation_items")
@Getter
public class DriveOperationItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "job_id", nullable = false)
	private DriveOperationJob job;

	@Column(name = "sequence_no", nullable = false)
	private Integer sequenceNo;

	@Column(name = "source_local_item_id")
	private Long sourceLocalItemId;

	@Column(name = "source_google_file_id", nullable = false, length = 255)
	private String sourceGoogleFileId;

	@Column(name = "source_name", columnDefinition = "TEXT")
	private String sourceName;

	@Column(name = "source_mime_type", length = 255)
	private String sourceMimeType;

	@Column(name = "source_parent_google_file_id", length = 255)
	private String sourceParentGoogleFileId;

	@Column(name = "source_path", columnDefinition = "TEXT")
	private String sourcePath;

	@Column(name = "destination_local_item_id")
	private Long destinationLocalItemId;

	@Column(name = "destination_google_file_id", length = 255)
	private String destinationGoogleFileId;

	@Column(name = "destination_parent_google_file_id", length = 255)
	private String destinationParentGoogleFileId;

	@Column(name = "destination_path", columnDefinition = "TEXT")
	private String destinationPath;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private DriveOperationItemStatus status;

	@Column(name = "size_bytes")
	private Long sizeBytes;

	@Column(name = "transferred_bytes", nullable = false)
	private Long transferredBytes;

	@Column(name = "attempt_count", nullable = false)
	private Integer attemptCount;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "mutation_started", nullable = false)
	private Boolean mutationStarted;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		if (status == null) {
			status = DriveOperationItemStatus.QUEUED;
		}

		if (transferredBytes == null) {
			transferredBytes = 0L;
		}

		if (attemptCount == null) {
			attemptCount = 0;
		}

		if (mutationStarted == null) {
			mutationStarted = false;
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

	public void setJob(DriveOperationJob job) {
		this.job = job;
	}

	public void setSequenceNo(Integer sequenceNo) {
		this.sequenceNo = sequenceNo;
	}

	public void setSourceLocalItemId(Long sourceLocalItemId) {
		this.sourceLocalItemId = sourceLocalItemId;
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

	public void setSourceParentGoogleFileId(String sourceParentGoogleFileId) {
		this.sourceParentGoogleFileId = sourceParentGoogleFileId;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public void setDestinationLocalItemId(Long destinationLocalItemId) {
		this.destinationLocalItemId = destinationLocalItemId;
	}

	public void setDestinationGoogleFileId(String destinationGoogleFileId) {
		this.destinationGoogleFileId = destinationGoogleFileId;
	}

	public void setDestinationParentGoogleFileId(String destinationParentGoogleFileId) {
		this.destinationParentGoogleFileId = destinationParentGoogleFileId;
	}

	public void setDestinationPath(String destinationPath) {
		this.destinationPath = destinationPath;
	}

	public void setStatus(DriveOperationItemStatus status) {
		this.status = status;
	}

	public void setSizeBytes(Long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

	public void setTransferredBytes(Long transferredBytes) {
		this.transferredBytes = transferredBytes;
	}

	public void setAttemptCount(Integer attemptCount) {
		this.attemptCount = attemptCount;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void setMutationStarted(Boolean mutationStarted) {
		this.mutationStarted = mutationStarted;
	}

}
