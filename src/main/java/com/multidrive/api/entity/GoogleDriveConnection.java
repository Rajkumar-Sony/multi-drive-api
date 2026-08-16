package com.multidrive.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import lombok.Getter;

@Entity
@Table(name = "google_drive_connections",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_user_google_account", columnNames = { "user_id", "google_subject_id" }) })
@Getter
public class GoogleDriveConnection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "google_subject_id", nullable = false)
	private String googleSubjectId;

	@Column(name = "google_email", nullable = false)
	private String googleEmail;

	@Column(name = "encrypted_access_token", columnDefinition = "TEXT")
	private String encryptedAccessToken;

	@Column(name = "encrypted_refresh_token", columnDefinition = "TEXT")
	private String encryptedRefreshToken;

	@Column(name = "access_token_expiry")
	private LocalDateTime accessTokenExpiry;

	@Column(columnDefinition = "TEXT")
	private String scopes;

	@Column(nullable = false)
	private String status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();

		this.createdAt = now;
		this.updatedAt = now;

		if (this.status == null) {
			this.status = "CONNECTED";
		}
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setGoogleSubjectId(String googleSubjectId) {
		this.googleSubjectId = googleSubjectId;
	}

	public void setGoogleEmail(String googleEmail) {
		this.googleEmail = googleEmail;
	}

	public void setEncryptedAccessToken(String encryptedAccessToken) {
		this.encryptedAccessToken = encryptedAccessToken;
	}

	public void setEncryptedRefreshToken(String encryptedRefreshToken) {
		this.encryptedRefreshToken = encryptedRefreshToken;
	}

	public void setAccessTokenExpiry(LocalDateTime accessTokenExpiry) {
		this.accessTokenExpiry = accessTokenExpiry;
	}

	public void setScopes(String scopes) {
		this.scopes = scopes;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
