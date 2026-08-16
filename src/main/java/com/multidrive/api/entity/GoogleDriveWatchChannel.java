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

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "google_drive_watch_channels")
@Getter
@Setter
public class GoogleDriveWatchChannel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "tracker_id", nullable = false)
	private GoogleDriveChangeTracker tracker;

	@Column(name = "channel_id", nullable = false, unique = true, length = 64)
	private String channelId;

	@Column(name = "channel_token", nullable = false, length = 256)
	private String channelToken;

	@Column(name = "resource_id", length = 255)
	private String resourceId;

	@Column(name = "resource_uri", columnDefinition = "TEXT")
	private String resourceUri;

	@Column(name = "expiration", nullable = false)
	private LocalDateTime expiration;

	@Column(name = "last_message_number")
	private Long lastMessageNumber;

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

}
