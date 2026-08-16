package com.multidrive.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
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

    public Boolean getCanAddChildren() {
        return canAddChildren;
    }

    public void setCanAddChildren(Boolean value) {
        this.canAddChildren = value;
    }

    public Boolean getCanComment() {
        return canComment;
    }

    public void setCanComment(Boolean value) {
        this.canComment = value;
    }

    public Boolean getCanCopy() {
        return canCopy;
    }

    public void setCanCopy(Boolean value) {
        this.canCopy = value;
    }

    public Boolean getCanDeleteDrive() {
        return canDeleteDrive;
    }

    public void setCanDeleteDrive(Boolean value) {
        this.canDeleteDrive = value;
    }

    public Boolean getCanDownload() {
        return canDownload;
    }

    public void setCanDownload(Boolean value) {
        this.canDownload = value;
    }

    public Boolean getCanEdit() {
        return canEdit;
    }

    public void setCanEdit(Boolean value) {
        this.canEdit = value;
    }

    public Boolean getCanListChildren() {
        return canListChildren;
    }

    public void setCanListChildren(Boolean value) {
        this.canListChildren = value;
    }

    public Boolean getCanManageMembers() {
        return canManageMembers;
    }

    public void setCanManageMembers(Boolean value) {
        this.canManageMembers = value;
    }

    public Boolean getCanReadRevisions() {
        return canReadRevisions;
    }

    public void setCanReadRevisions(Boolean value) {
        this.canReadRevisions = value;
    }

    public Boolean getCanRename() {
        return canRename;
    }

    public void setCanRename(Boolean value) {
        this.canRename = value;
    }

    public Boolean getCanRenameDrive() {
        return canRenameDrive;
    }

    public void setCanRenameDrive(Boolean value) {
        this.canRenameDrive = value;
    }

    public Boolean getCanChangeDriveBackground() {
        return canChangeDriveBackground;
    }

    public void setCanChangeDriveBackground(Boolean value) {
        this.canChangeDriveBackground = value;
    }

    public Boolean getCanShare() {
        return canShare;
    }

    public void setCanShare(Boolean value) {
        this.canShare = value;
    }

    public Boolean getCanChangeCopyRequiresWriterPermissionRestriction() {
        return canChangeCopyRequiresWriterPermissionRestriction;
    }

    public void setCanChangeCopyRequiresWriterPermissionRestriction(Boolean value) {
        this.canChangeCopyRequiresWriterPermissionRestriction = value;
    }

    public Boolean getCanChangeDomainUsersOnlyRestriction() {
        return canChangeDomainUsersOnlyRestriction;
    }

    public void setCanChangeDomainUsersOnlyRestriction(Boolean value) {
        this.canChangeDomainUsersOnlyRestriction = value;
    }

    public Boolean getCanChangeDriveMembersOnlyRestriction() {
        return canChangeDriveMembersOnlyRestriction;
    }

    public void setCanChangeDriveMembersOnlyRestriction(Boolean value) {
        this.canChangeDriveMembersOnlyRestriction = value;
    }

    public Boolean getCanChangeSharingFoldersRequiresOrganizerPermissionRestriction() {
        return canChangeSharingFoldersRequiresOrganizerPermissionRestriction;
    }

    public void setCanChangeSharingFoldersRequiresOrganizerPermissionRestriction(
            Boolean value
    ) {
        this.canChangeSharingFoldersRequiresOrganizerPermissionRestriction =
                value;
    }

    public Boolean getCanResetDriveRestrictions() {
        return canResetDriveRestrictions;
    }

    public void setCanResetDriveRestrictions(Boolean value) {
        this.canResetDriveRestrictions = value;
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

    public Boolean getCanChangeDownloadRestriction() {
        return canChangeDownloadRestriction;
    }

    public void setCanChangeDownloadRestriction(Boolean value) {
        this.canChangeDownloadRestriction = value;
    }
}