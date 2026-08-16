package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
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

    public Boolean getCanMoveChildrenOutOfDrive() {
        return canMoveChildrenOutOfDrive;
    }

    public void setCanMoveChildrenOutOfDrive(Boolean value) {
        this.canMoveChildrenOutOfDrive = value;
    }

    public Boolean getCanReadDrive() {
        return canReadDrive;
    }

    public void setCanReadDrive(Boolean value) {
        this.canReadDrive = value;
    }

    public Boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean value) {
        this.canEdit = value;
    }

    public Boolean getCanCopy() {
        return canCopy;
    }

    public void setCanCopy(Boolean value) {
        this.canCopy = value;
    }

    public Boolean getCanComment() {
        return canComment;
    }

    public void setCanComment(Boolean value) {
        this.canComment = value;
    }

    public Boolean getCanAddChildren() {
        return canAddChildren;
    }

    public void setCanAddChildren(Boolean value) {
        this.canAddChildren = value;
    }

    public Boolean getCanDelete() {
        return canDelete;
    }

    public void setCanDelete(Boolean value) {
        this.canDelete = value;
    }

    public Boolean getCanDownload() {
        return canDownload;
    }

    public void setCanDownload(Boolean value) {
        this.canDownload = value;
    }

    public Boolean getCanListChildren() {
        return canListChildren;
    }

    public void setCanListChildren(Boolean value) {
        this.canListChildren = value;
    }

    public Boolean getCanRemoveChildren() {
        return canRemoveChildren;
    }

    public void setCanRemoveChildren(Boolean value) {
        this.canRemoveChildren = value;
    }

    public Boolean getCanRename() {
        return canRename;
    }

    public void setCanRename(Boolean value) {
        this.canRename = value;
    }

    public Boolean getCanTrash() {
        return canTrash;
    }

    public void setCanTrash(Boolean value) {
        this.canTrash = value;
    }

    public Boolean getCanReadRevisions() {
        return canReadRevisions;
    }

    public void setCanReadRevisions(Boolean value) {
        this.canReadRevisions = value;
    }

    public Boolean getCanChangeCopyRequiresWriterPermission() {
        return canChangeCopyRequiresWriterPermission;
    }

    public void setCanChangeCopyRequiresWriterPermission(Boolean value) {
        this.canChangeCopyRequiresWriterPermission = value;
    }

    public Boolean getCanUntrash() {
        return canUntrash;
    }

    public void setCanUntrash(Boolean value) {
        this.canUntrash = value;
    }

    public Boolean getCanModifyContent() {
        return canModifyContent;
    }

    public void setCanModifyContent(Boolean value) {
        this.canModifyContent = value;
    }

    public Boolean getCanDeleteChildren() {
        return canDeleteChildren;
    }

    public void setCanDeleteChildren(Boolean value) {
        this.canDeleteChildren = value;
    }

    public Boolean getCanTrashChildren() {
        return canTrashChildren;
    }

    public void setCanTrashChildren(Boolean value) {
        this.canTrashChildren = value;
    }

    public Boolean getCanMoveItemOutOfDrive() {
        return canMoveItemOutOfDrive;
    }

    public void setCanMoveItemOutOfDrive(Boolean value) {
        this.canMoveItemOutOfDrive = value;
    }

    public Boolean getCanAddMyDriveParent() {
        return canAddMyDriveParent;
    }

    public void setCanAddMyDriveParent(Boolean value) {
        this.canAddMyDriveParent = value;
    }

    public Boolean getCanRemoveMyDriveParent() {
        return canRemoveMyDriveParent;
    }

    public void setCanRemoveMyDriveParent(Boolean value) {
        this.canRemoveMyDriveParent = value;
    }

    public Boolean getCanMoveItemWithinDrive() {
        return canMoveItemWithinDrive;
    }

    public void setCanMoveItemWithinDrive(Boolean value) {
        this.canMoveItemWithinDrive = value;
    }

    public Boolean getCanShare() {
        return canShare;
    }

    public void setCanShare(Boolean value) {
        this.canShare = value;
    }

    public Boolean getCanMoveChildrenWithinDrive() {
        return canMoveChildrenWithinDrive;
    }

    public void setCanMoveChildrenWithinDrive(Boolean value) {
        this.canMoveChildrenWithinDrive = value;
    }

    public Boolean getCanAddFolderFromAnotherDrive() {
        return canAddFolderFromAnotherDrive;
    }

    public void setCanAddFolderFromAnotherDrive(Boolean value) {
        this.canAddFolderFromAnotherDrive = value;
    }

    public Boolean getCanChangeSecurityUpdateEnabled() {
        return canChangeSecurityUpdateEnabled;
    }

    public void setCanChangeSecurityUpdateEnabled(Boolean value) {
        this.canChangeSecurityUpdateEnabled = value;
    }

    public Boolean getCanAcceptOwnership() {
        return canAcceptOwnership;
    }

    public void setCanAcceptOwnership(Boolean value) {
        this.canAcceptOwnership = value;
    }

    public Boolean getCanReadLabels() {
        return canReadLabels;
    }

    public void setCanReadLabels(Boolean value) {
        this.canReadLabels = value;
    }

    public Boolean getCanModifyLabels() {
        return canModifyLabels;
    }

    public void setCanModifyLabels(Boolean value) {
        this.canModifyLabels = value;
    }

    public Boolean getCanModifyEditorContentRestriction() {
        return canModifyEditorContentRestriction;
    }

    public void setCanModifyEditorContentRestriction(Boolean value) {
        this.canModifyEditorContentRestriction = value;
    }

    public Boolean getCanModifyOwnerContentRestriction() {
        return canModifyOwnerContentRestriction;
    }

    public void setCanModifyOwnerContentRestriction(Boolean value) {
        this.canModifyOwnerContentRestriction = value;
    }

    public Boolean getCanRemoveContentRestriction() {
        return canRemoveContentRestriction;
    }

    public void setCanRemoveContentRestriction(Boolean value) {
        this.canRemoveContentRestriction = value;
    }

    public Boolean getCanDisableInheritedPermissions() {
        return canDisableInheritedPermissions;
    }

    public void setCanDisableInheritedPermissions(Boolean value) {
        this.canDisableInheritedPermissions = value;
    }

    public Boolean getCanEnableInheritedPermissions() {
        return canEnableInheritedPermissions;
    }

    public void setCanEnableInheritedPermissions(Boolean value) {
        this.canEnableInheritedPermissions = value;
    }

    public Boolean getCanChangeItemDownloadRestriction() {
        return canChangeItemDownloadRestriction;
    }

    public void setCanChangeItemDownloadRestriction(Boolean value) {
        this.canChangeItemDownloadRestriction = value;
    }

    public Boolean getCanStartApproval() {
        return canStartApproval;
    }

    public void setCanStartApproval(Boolean value) {
        this.canStartApproval = value;
    }
}