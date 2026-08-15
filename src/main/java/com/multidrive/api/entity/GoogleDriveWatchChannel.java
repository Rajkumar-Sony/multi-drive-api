package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "google_drive_watch_channels"
)
public class GoogleDriveWatchChannel {

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
            name = "tracker_id",
            nullable = false
    )
    private GoogleDriveChangeTracker tracker;

    @Column(
            name = "channel_id",
            nullable = false,
            unique = true,
            length = 64
    )
    private String channelId;

    @Column(
            name = "channel_token",
            nullable = false,
            length = 256
    )
    private String channelToken;

    @Column(
            name = "resource_id",
            length = 255
    )
    private String resourceId;

    @Column(
            name = "resource_uri",
            columnDefinition = "TEXT"
    )
    private String resourceUri;

    @Column(
            name = "expiration",
            nullable = false
    )
    private LocalDateTime expiration;

    @Column(
            name = "last_message_number"
    )
    private Long lastMessageNumber;

    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private String status;

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

        if (status == null || status.isBlank()) {
            status = "ACTIVE";
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

    public void setId(
            Long id
    ) {
        this.id = id;
    }

    public GoogleDriveChangeTracker getTracker() {
        return tracker;
    }

    public void setTracker(
            GoogleDriveChangeTracker tracker
    ) {
        this.tracker = tracker;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(
            String channelId
    ) {
        this.channelId = channelId;
    }

    public String getChannelToken() {
        return channelToken;
    }

    public void setChannelToken(
            String channelToken
    ) {
        this.channelToken = channelToken;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(
            String resourceId
    ) {
        this.resourceId = resourceId;
    }

    public String getResourceUri() {
        return resourceUri;
    }

    public void setResourceUri(
            String resourceUri
    ) {
        this.resourceUri = resourceUri;
    }

    public LocalDateTime getExpiration() {
        return expiration;
    }

    public void setExpiration(
            LocalDateTime expiration
    ) {
        this.expiration = expiration;
    }

    public Long getLastMessageNumber() {
        return lastMessageNumber;
    }

    public void setLastMessageNumber(
            Long lastMessageNumber
    ) {
        this.lastMessageNumber = lastMessageNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status
    ) {
        this.status = status;
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