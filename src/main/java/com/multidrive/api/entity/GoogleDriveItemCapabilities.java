package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class GoogleDriveItemCapabilities {

	@Column(name = "cap_can_move_children_out_of_drive")
	private Boolean canMoveChildrenOutOfDrive;

	@Column(name = "cap_can_read_drive")
	private Boolean canReadDrive;

	@Column(name = "cap_can_edit")
	private Boolean canEdit;

	@Column(name = "cap_can_copy")
	private Boolean canCopy;

	@Column(name = "cap_can_comment")
	private Boolean canComment;

	@Column(name = "cap_can_add_children")
	private Boolean canAddChildren;

	@Column(name = "cap_can_delete")
	private Boolean canDelete;

	@Column(name = "cap_can_download")
	private Boolean canDownload;

	@Column(name = "cap_can_list_children")
	private Boolean canListChildren;

	@Column(name = "cap_can_remove_children")
	private Boolean canRemoveChildren;

	@Column(name = "cap_can_rename")
	private Boolean canRename;

	@Column(name = "cap_can_trash")
	private Boolean canTrash;

	@Column(name = "cap_can_read_revisions")
	private Boolean canReadRevisions;

	@Column(name = "cap_can_change_copy_requires_writer_permission")
	private Boolean canChangeCopyRequiresWriterPermission;

	@Column(name = "cap_can_untrash")
	private Boolean canUntrash;

	@Column(name = "cap_can_modify_content")
	private Boolean canModifyContent;

	@Column(name = "cap_can_delete_children")
	private Boolean canDeleteChildren;

	@Column(name = "cap_can_trash_children")
	private Boolean canTrashChildren;

	@Column(name = "cap_can_move_item_out_of_drive")
	private Boolean canMoveItemOutOfDrive;

	@Column(name = "cap_can_add_my_drive_parent")
	private Boolean canAddMyDriveParent;

	@Column(name = "cap_can_remove_my_drive_parent")
	private Boolean canRemoveMyDriveParent;

	@Column(name = "cap_can_move_item_within_drive")
	private Boolean canMoveItemWithinDrive;

	@Column(name = "cap_can_share")
	private Boolean canShare;

	@Column(name = "cap_can_move_children_within_drive")
	private Boolean canMoveChildrenWithinDrive;

	@Column(name = "cap_can_add_folder_from_another_drive")
	private Boolean canAddFolderFromAnotherDrive;

	@Column(name = "cap_can_change_security_update_enabled")
	private Boolean canChangeSecurityUpdateEnabled;

	@Column(name = "cap_can_accept_ownership")
	private Boolean canAcceptOwnership;

	@Column(name = "cap_can_read_labels")
	private Boolean canReadLabels;

	@Column(name = "cap_can_modify_labels")
	private Boolean canModifyLabels;

	@Column(name = "cap_can_modify_editor_content_restriction")
	private Boolean canModifyEditorContentRestriction;

	@Column(name = "cap_can_modify_owner_content_restriction")
	private Boolean canModifyOwnerContentRestriction;

	@Column(name = "cap_can_remove_content_restriction")
	private Boolean canRemoveContentRestriction;

	@Column(name = "cap_can_disable_inherited_permissions")
	private Boolean canDisableInheritedPermissions;

	@Column(name = "cap_can_enable_inherited_permissions")
	private Boolean canEnableInheritedPermissions;

	@Column(name = "cap_can_change_item_download_restriction")
	private Boolean canChangeItemDownloadRestriction;

	@Column(name = "cap_can_start_approval")
	private Boolean canStartApproval;

}
