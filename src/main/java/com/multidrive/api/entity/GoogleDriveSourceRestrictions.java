package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class GoogleDriveSourceRestrictions {

	@Column(name = "restriction_copy_requires_writer_permission")
	private Boolean copyRequiresWriterPermission;

	@Column(name = "restriction_domain_users_only")
	private Boolean domainUsersOnly;

	@Column(name = "restriction_drive_members_only")
	private Boolean driveMembersOnly;

	@Column(name = "restriction_admin_managed")
	private Boolean adminManagedRestrictions;

	@Column(name = "restriction_sharing_folders_requires_organizer")
	private Boolean sharingFoldersRequiresOrganizerPermission;

	@Column(name = "restriction_download_readers")
	private Boolean downloadRestrictedForReaders;

	@Column(name = "restriction_download_writers")
	private Boolean downloadRestrictedForWriters;

}
