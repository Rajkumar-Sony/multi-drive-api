package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "google_drive_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_google_drive_item",
                        columnNames = {
                                "connection_id",
                                "google_file_id"
                        }
                )
        }
)
public class GoogleDriveItem {

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
            name = "connection_id",
            nullable = false
    )
    private GoogleDriveConnection connection;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "source_id",
            nullable = false
    )
    private GoogleDriveSource source;

    @Column(
            name = "google_file_id",
            nullable = false,
            length = 255
    )
    private String googleFileId;

    @Column(
            name = "name",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String name;

    @Column(
            name = "mime_type",
            nullable = false,
            length = 255
    )
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "category",
            nullable = false,
            length = 30
    )
    private GoogleDriveItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source_type",
            nullable = false,
            length = 30
    )
    private GoogleDriveItemSourceType sourceType;

    @Column(
            name = "parent_id",
            length = 255
    )
    private String parentId;

    @Column(
            name = "drive_id",
            length = 255
    )
    private String driveId;

    @Column(
            name = "web_view_link",
            columnDefinition = "TEXT"
    )
    private String webViewLink;

    @Column(
            name = "thumbnail_link",
            columnDefinition = "TEXT"
    )
    private String thumbnailLink;

    @Column(
            name = "icon_link",
            columnDefinition = "TEXT"
    )
    private String iconLink;

    @Column(
            name = "size_bytes"
    )
    private Long sizeBytes;

    @Column(
            name = "google_created_time"
    )
    private Instant googleCreatedTime;

    @Column(
            name = "google_modified_time"
    )
    private Instant googleModifiedTime;

    @Column(
            name = "trashed",
            nullable = false
    )
    private boolean trashed;

    @Column(
            name = "sync_run_id",
            length = 36
    )
    private String syncRunId;

    @Embedded
    private GoogleDriveItemCapabilities capabilities;

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

        if (category == null) {
            category =
                    GoogleDriveItemCategory.OTHER;
        }

        if (sourceType == null) {
            sourceType =
                    GoogleDriveItemSourceType.MY_DRIVE;
        }

        if (capabilities == null) {
            capabilities =
                    new GoogleDriveItemCapabilities();
        }

        createdAt = now;
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

    public void setId(Long id) {
        this.id = id;
    }

    public GoogleDriveConnection getConnection() {
        return connection;
    }

    public void setConnection(
            GoogleDriveConnection connection
    ) {
        this.connection = connection;
    }

    public GoogleDriveSource getSource() {
        return source;
    }

    public void setSource(
            GoogleDriveSource source
    ) {
        this.source = source;
    }

    public String getGoogleFileId() {
        return googleFileId;
    }

    public void setGoogleFileId(
            String googleFileId
    ) {
        this.googleFileId = googleFileId;
    }

    public String getName() {
        return name;
    }

    public void setName(
            String name
    ) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(
            String mimeType
    ) {
        this.mimeType = mimeType;
    }

    public GoogleDriveItemCategory getCategory() {
        return category;
    }

    public void setCategory(
            GoogleDriveItemCategory category
    ) {
        this.category = category;
    }

    public GoogleDriveItemSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(
            GoogleDriveItemSourceType sourceType
    ) {
        this.sourceType = sourceType;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(
            String parentId
    ) {
        this.parentId = parentId;
    }

    public String getDriveId() {
        return driveId;
    }

    public void setDriveId(
            String driveId
    ) {
        this.driveId = driveId;
    }

    public String getWebViewLink() {
        return webViewLink;
    }

    public void setWebViewLink(
            String webViewLink
    ) {
        this.webViewLink = webViewLink;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    public void setThumbnailLink(
            String thumbnailLink
    ) {
        this.thumbnailLink = thumbnailLink;
    }

    public String getIconLink() {
        return iconLink;
    }

    public void setIconLink(
            String iconLink
    ) {
        this.iconLink = iconLink;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(
            Long sizeBytes
    ) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getGoogleCreatedTime() {
        return googleCreatedTime;
    }

    public void setGoogleCreatedTime(
            Instant googleCreatedTime
    ) {
        this.googleCreatedTime = googleCreatedTime;
    }

    public Instant getGoogleModifiedTime() {
        return googleModifiedTime;
    }

    public void setGoogleModifiedTime(
            Instant googleModifiedTime
    ) {
        this.googleModifiedTime = googleModifiedTime;
    }

    public boolean isTrashed() {
        return trashed;
    }

    public void setTrashed(
            boolean trashed
    ) {
        this.trashed = trashed;
    }

    public String getSyncRunId() {
        return syncRunId;
    }

    public void setSyncRunId(
            String syncRunId
    ) {
        this.syncRunId = syncRunId;
    }

    public GoogleDriveItemCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(
            GoogleDriveItemCapabilities capabilities
    ) {
        this.capabilities = capabilities;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }
}