package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class GoogleDriveSourceCapabilities {

	@Column(name = "cap_can_add_children")
	private Boolean canAddChildren;

	@Column(name = "cap_can_comment")
	private Boolean canComment;

	@Column(name = "cap_can_copy")
	private Boolean canCopy;

	@Column(name = "cap_can_delete_drive")
	private Boolean canDeleteDrive;

	@Column(name = "cap_can_download")
	private Boolean canDownload;

	@Column(name = "cap_can_edit")
	private Boolean canEdit;

	@Column(name = "cap_can_list_children")
	private Boolean canListChildren;

	@Column(name = "cap_can_manage_members")
	private Boolean canManageMembers;

	@Column(name = "cap_can_read_revisions")
	private Boolean canReadRevisions;

	@Column(name = "cap_can_rename")
	private Boolean canRename;

	@Column(name = "cap_can_rename_drive")
	private Boolean canRenameDrive;

	@Column(name = "cap_can_change_drive_background")
	private Boolean canChangeDriveBackground;

	@Column(name = "cap_can_share")
	private Boolean canShare;

	@Column(name = "cap_can_change_copy_requires_writer_permission_restriction")
	private Boolean canChangeCopyRequiresWriterPermissionRestriction;

	@Column(name = "cap_can_change_domain_users_only_restriction")
	private Boolean canChangeDomainUsersOnlyRestriction;

	@Column(name = "cap_can_change_drive_members_only_restriction")
	private Boolean canChangeDriveMembersOnlyRestriction;

	@Column(name = "cap_can_change_sharing_folders_requires_organizer_permission_restriction")
	private Boolean canChangeSharingFoldersRequiresOrganizerPermissionRestriction;

	@Column(name = "cap_can_reset_drive_restrictions")
	private Boolean canResetDriveRestrictions;

	@Column(name = "cap_can_delete_children")
	private Boolean canDeleteChildren;

	@Column(name = "cap_can_trash_children")
	private Boolean canTrashChildren;

	@Column(name = "cap_can_change_download_restriction")
	private Boolean canChangeDownloadRestriction;

}
