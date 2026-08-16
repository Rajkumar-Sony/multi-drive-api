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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "google_drive_sources"
)
public class GoogleDriveSource {

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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "source_type",
            nullable = false,
            length = 30
    )
    private GoogleDriveSourceType sourceType;

    @Column(
            name = "google_drive_id",
            length = 255
    )
    private String googleDriveId;

    @Column(
            name = "root_folder_id",
            length = 255
    )
    private String rootFolderId;

    @Column(
            name = "name",
            length = 255
    )
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private GoogleDriveSourceStatus status;

    @Column(
            name = "hidden"
    )
    private Boolean hidden;

    @Column(
            name = "google_created_time"
    )
    private Instant googleCreatedTime;

    @Embedded
    private GoogleDriveSourceCapabilities capabilities;

    @Embedded
    private GoogleDriveSourceRestrictions restrictions;

    @Column(
            name = "discovery_run_id",
            length = 36
    )
    private String discoveryRunId;

    @Column(
            name = "last_discovered_at"
    )
    private LocalDateTime lastDiscoveredAt;

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
            status =
                    GoogleDriveSourceStatus.ACTIVE;
        }

        if (capabilities == null) {
            capabilities =
                    new GoogleDriveSourceCapabilities();
        }

        if (restrictions == null) {
            restrictions =
                    new GoogleDriveSourceRestrictions();
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

    public void setId(
            Long id
    ) {
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

    public GoogleDriveSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(
            GoogleDriveSourceType sourceType
    ) {
        this.sourceType = sourceType;
    }

    public String getGoogleDriveId() {
        return googleDriveId;
    }

    public void setGoogleDriveId(
            String googleDriveId
    ) {
        this.googleDriveId = googleDriveId;
    }

    public String getRootFolderId() {
        return rootFolderId;
    }

    public void setRootFolderId(
            String rootFolderId
    ) {
        this.rootFolderId = rootFolderId;
    }

    public String getName() {
        return name;
    }

    public void setName(
            String name
    ) {
        this.name = name;
    }

    public GoogleDriveSourceStatus getStatus() {
        return status;
    }

    public void setStatus(
            GoogleDriveSourceStatus status
    ) {
        this.status = status;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(
            Boolean hidden
    ) {
        this.hidden = hidden;
    }

    public Instant getGoogleCreatedTime() {
        return googleCreatedTime;
    }

    public void setGoogleCreatedTime(
            Instant googleCreatedTime
    ) {
        this.googleCreatedTime = googleCreatedTime;
    }

    public GoogleDriveSourceCapabilities getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(
            GoogleDriveSourceCapabilities capabilities
    ) {
        this.capabilities = capabilities;
    }

    public GoogleDriveSourceRestrictions getRestrictions() {
        return restrictions;
    }

    public void setRestrictions(
            GoogleDriveSourceRestrictions restrictions
    ) {
        this.restrictions = restrictions;
    }

    public String getDiscoveryRunId() {
        return discoveryRunId;
    }

    public void setDiscoveryRunId(
            String discoveryRunId
    ) {
        this.discoveryRunId = discoveryRunId;
    }

    public LocalDateTime getLastDiscoveredAt() {
        return lastDiscoveredAt;
    }

    public void setLastDiscoveredAt(
            LocalDateTime lastDiscoveredAt
    ) {
        this.lastDiscoveredAt = lastDiscoveredAt;
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