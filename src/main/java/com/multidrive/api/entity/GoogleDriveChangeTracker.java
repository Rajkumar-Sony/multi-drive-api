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
@Table(name = "google_drive_change_trackers")
public class GoogleDriveChangeTracker {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "connection_id", nullable = false)
	private GoogleDriveConnection connection;

	@Enumerated(EnumType.STRING)
	@Column(name = "tracker_type", nullable = false, length = 30)
	private GoogleDriveTrackerType trackerType;

	@Column(name = "drive_id", length = 255)
	private String driveId;

	@Column(name = "page_token", columnDefinition = "TEXT")
	private String pageToken;

	@Column(name = "last_synced_at")
	private LocalDateTime lastSyncedAt;

	@Column(name = "status", nullable = false, length = 30)
	private String status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {

		LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

		if (status == null || status.isBlank()) {
			status = "ACTIVE";
		}

		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	public void preUpdate() {

		updatedAt = LocalDateTime.now(ZoneOffset.UTC);
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

	public void setConnection(GoogleDriveConnection connection) {
		this.connection = connection;
	}

	public GoogleDriveTrackerType getTrackerType() {
		return trackerType;
	}

	public void setTrackerType(GoogleDriveTrackerType trackerType) {
		this.trackerType = trackerType;
	}

	public String getDriveId() {
		return driveId;
	}

	public void setDriveId(String driveId) {
		this.driveId = driveId;
	}

	public String getPageToken() {
		return pageToken;
	}

	public void setPageToken(String pageToken) {
		this.pageToken = pageToken;
	}

	public LocalDateTime getLastSyncedAt() {
		return lastSyncedAt;
	}

	public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
		this.lastSyncedAt = lastSyncedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}