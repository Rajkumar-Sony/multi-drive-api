package com.multidrive.api.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import lombok.Getter;

@Entity
@Table(name = "users",
		uniqueConstraints = {
				@UniqueConstraint(name = "uk_users_google_subject_id", columnNames = "google_subject_id") })
@Getter
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "google_subject_id", nullable = false)
	private String googleSubjectId;

	@Column(nullable = false)
	private String email;

	private String name;

	@Column(name = "picture_url")
	private String pictureUrl;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void setGoogleSubjectId(String googleSubjectId) {
		this.googleSubjectId = googleSubjectId;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPictureUrl(String pictureUrl) {
		this.pictureUrl = pictureUrl;
	}

}
