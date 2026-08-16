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

@Entity
@Table(
        name = "drive_operation_items"
)
public class DriveOperationItem {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "job_id",
            nullable = false
    )
    private DriveOperationJob job;

    @Column(
            name = "sequence_no",
            nullable = false
    )
    private Integer sequenceNo;

    @Column(
            name = "source_local_item_id"
    )
    private Long sourceLocalItemId;

    @Column(
            name = "source_google_file_id",
            nullable = false,
            length = 255
    )
    private String sourceGoogleFileId;

    @Column(
            name = "source_name",
            columnDefinition = "TEXT"
    )
    private String sourceName;

    @Column(
            name = "source_mime_type",
            length = 255
    )
    private String sourceMimeType;

    @Column(
            name = "source_parent_google_file_id",
            length = 255
    )
    private String sourceParentGoogleFileId;

    @Column(
            name = "source_path",
            columnDefinition = "TEXT"
    )
    private String sourcePath;

    @Column(
            name = "destination_local_item_id"
    )
    private Long destinationLocalItemId;

    @Column(
            name = "destination_google_file_id",
            length = 255
    )
    private String destinationGoogleFileId;

    @Column(
            name = "destination_parent_google_file_id",
            length = 255
    )
    private String destinationParentGoogleFileId;

    @Column(
            name = "destination_path",
            columnDefinition = "TEXT"
    )
    private String destinationPath;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private DriveOperationItemStatus status;

    @Column(
            name = "size_bytes"
    )
    private Long sizeBytes;

    @Column(
            name = "transferred_bytes",
            nullable = false
    )
    private Long transferredBytes;

    @Column(
            name = "attempt_count",
            nullable = false
    )
    private Integer attemptCount;

    @Column(
            name = "error_code",
            length = 100
    )
    private String errorCode;

    @Column(
            name = "error_message",
            columnDefinition = "TEXT"
    )
    private String errorMessage;

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        LocalDateTime now =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );

        if (status == null) {
            status = DriveOperationItemStatus.QUEUED;
        }

        if (transferredBytes == null) {
            transferredBytes = 0L;
        }

        if (attemptCount == null) {
            attemptCount = 0;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {

        updatedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC
                );
    }

    public Long getId() {
        return id;
    }

    public DriveOperationJob getJob() {
        return job;
    }

    public void setJob(DriveOperationJob job) {
        this.job = job;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public Long getSourceLocalItemId() {
        return sourceLocalItemId;
    }

    public void setSourceLocalItemId(Long sourceLocalItemId) {
        this.sourceLocalItemId = sourceLocalItemId;
    }

    public String getSourceGoogleFileId() {
        return sourceGoogleFileId;
    }

    public void setSourceGoogleFileId(String sourceGoogleFileId) {
        this.sourceGoogleFileId = sourceGoogleFileId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceMimeType() {
        return sourceMimeType;
    }

    public void setSourceMimeType(String sourceMimeType) {
        this.sourceMimeType = sourceMimeType;
    }

    public String getSourceParentGoogleFileId() {
        return sourceParentGoogleFileId;
    }

    public void setSourceParentGoogleFileId(
            String sourceParentGoogleFileId
    ) {
        this.sourceParentGoogleFileId = sourceParentGoogleFileId;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Long getDestinationLocalItemId() {
        return destinationLocalItemId;
    }

    public void setDestinationLocalItemId(Long destinationLocalItemId) {
        this.destinationLocalItemId = destinationLocalItemId;
    }

    public String getDestinationGoogleFileId() {
        return destinationGoogleFileId;
    }

    public void setDestinationGoogleFileId(String destinationGoogleFileId) {
        this.destinationGoogleFileId = destinationGoogleFileId;
    }

    public String getDestinationParentGoogleFileId() {
        return destinationParentGoogleFileId;
    }

    public void setDestinationParentGoogleFileId(
            String destinationParentGoogleFileId
    ) {
        this.destinationParentGoogleFileId =
                destinationParentGoogleFileId;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public DriveOperationItemStatus getStatus() {
        return status;
    }

    public void setStatus(DriveOperationItemStatus status) {
        this.status = status;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Long getTransferredBytes() {
        return transferredBytes;
    }

    public void setTransferredBytes(Long transferredBytes) {
        this.transferredBytes = transferredBytes;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
