package com.multidrive.api.mapper;

import com.multidrive.api.dto.GoogleDriveFileCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveSharedDriveCapabilitiesResponse;
import com.multidrive.api.dto.GoogleDriveSharedDriveRestrictionsResponse;
import com.multidrive.api.entity.GoogleDriveItemCapabilities;
import com.multidrive.api.entity.GoogleDriveSourceCapabilities;
import com.multidrive.api.entity.GoogleDriveSourceRestrictions;

import org.springframework.stereotype.Component;

@Component
public class GoogleDriveCapabilityMapper {

    public GoogleDriveItemCapabilities toItemCapabilities(
            GoogleDriveFileCapabilitiesResponse source
    ) {

        GoogleDriveItemCapabilities target =
                new GoogleDriveItemCapabilities();

        if (source == null) {
            return target;
        }

        target.setCanMoveChildrenOutOfDrive(
                source.canMoveChildrenOutOfDrive()
        );

        target.setCanReadDrive(
                source.canReadDrive()
        );

        target.setCanEdit(
                source.canEdit()
        );

        target.setCanCopy(
                source.canCopy()
        );

        target.setCanComment(
                source.canComment()
        );

        target.setCanAddChildren(
                source.canAddChildren()
        );

        target.setCanDelete(
                source.canDelete()
        );

        target.setCanDownload(
                source.canDownload()
        );

        target.setCanListChildren(
                source.canListChildren()
        );

        target.setCanRemoveChildren(
                source.canRemoveChildren()
        );

        target.setCanRename(
                source.canRename()
        );

        target.setCanTrash(
                source.canTrash()
        );

        target.setCanReadRevisions(
                source.canReadRevisions()
        );

        target.setCanChangeCopyRequiresWriterPermission(
                source.canChangeCopyRequiresWriterPermission()
        );

        target.setCanUntrash(
                source.canUntrash()
        );

        target.setCanModifyContent(
                source.canModifyContent()
        );

        target.setCanDeleteChildren(
                source.canDeleteChildren()
        );

        target.setCanTrashChildren(
                source.canTrashChildren()
        );

        target.setCanMoveItemOutOfDrive(
                source.canMoveItemOutOfDrive()
        );

        target.setCanAddMyDriveParent(
                source.canAddMyDriveParent()
        );

        target.setCanRemoveMyDriveParent(
                source.canRemoveMyDriveParent()
        );

        target.setCanMoveItemWithinDrive(
                source.canMoveItemWithinDrive()
        );

        target.setCanShare(
                source.canShare()
        );

        target.setCanMoveChildrenWithinDrive(
                source.canMoveChildrenWithinDrive()
        );

        target.setCanAddFolderFromAnotherDrive(
                source.canAddFolderFromAnotherDrive()
        );

        target.setCanChangeSecurityUpdateEnabled(
                source.canChangeSecurityUpdateEnabled()
        );

        target.setCanAcceptOwnership(
                source.canAcceptOwnership()
        );

        target.setCanReadLabels(
                source.canReadLabels()
        );

        target.setCanModifyLabels(
                source.canModifyLabels()
        );

        target.setCanModifyEditorContentRestriction(
                source.canModifyEditorContentRestriction()
        );

        target.setCanModifyOwnerContentRestriction(
                source.canModifyOwnerContentRestriction()
        );

        target.setCanRemoveContentRestriction(
                source.canRemoveContentRestriction()
        );

        target.setCanDisableInheritedPermissions(
                source.canDisableInheritedPermissions()
        );

        target.setCanEnableInheritedPermissions(
                source.canEnableInheritedPermissions()
        );

        target.setCanChangeItemDownloadRestriction(
                source.canChangeItemDownloadRestriction()
        );

        target.setCanStartApproval(
                source.canStartApproval()
        );

        return target;
    }

    public GoogleDriveSourceCapabilities
    toMyDriveSourceCapabilities(
            GoogleDriveFileCapabilitiesResponse source
    ) {

        GoogleDriveSourceCapabilities target =
                new GoogleDriveSourceCapabilities();

        if (source == null) {
            return target;
        }

        target.setCanAddChildren(
                source.canAddChildren()
        );

        target.setCanComment(
                source.canComment()
        );

        target.setCanCopy(
                source.canCopy()
        );

        target.setCanDownload(
                source.canDownload()
        );

        target.setCanEdit(
                source.canEdit()
        );

        target.setCanListChildren(
                source.canListChildren()
        );

        target.setCanReadRevisions(
                source.canReadRevisions()
        );

        target.setCanRename(
                source.canRename()
        );

        target.setCanShare(
                source.canShare()
        );

        target.setCanDeleteChildren(
                source.canDeleteChildren()
        );

        target.setCanTrashChildren(
                source.canTrashChildren()
        );

        return target;
    }

    public GoogleDriveSourceCapabilities
    toSharedDriveSourceCapabilities(
            GoogleDriveSharedDriveCapabilitiesResponse source
    ) {

        GoogleDriveSourceCapabilities target =
                new GoogleDriveSourceCapabilities();

        if (source == null) {
            return target;
        }

        target.setCanAddChildren(
                source.canAddChildren()
        );

        target.setCanComment(
                source.canComment()
        );

        target.setCanCopy(
                source.canCopy()
        );

        target.setCanDeleteDrive(
                source.canDeleteDrive()
        );

        target.setCanDownload(
                source.canDownload()
        );

        target.setCanEdit(
                source.canEdit()
        );

        target.setCanListChildren(
                source.canListChildren()
        );

        target.setCanManageMembers(
                source.canManageMembers()
        );

        target.setCanReadRevisions(
                source.canReadRevisions()
        );

        target.setCanRename(
                source.canRename()
        );

        target.setCanRenameDrive(
                source.canRenameDrive()
        );

        target.setCanChangeDriveBackground(
                source.canChangeDriveBackground()
        );

        target.setCanShare(
                source.canShare()
        );

        target.setCanChangeCopyRequiresWriterPermissionRestriction(
                source.canChangeCopyRequiresWriterPermissionRestriction()
        );

        target.setCanChangeDomainUsersOnlyRestriction(
                source.canChangeDomainUsersOnlyRestriction()
        );

        target.setCanChangeDriveMembersOnlyRestriction(
                source.canChangeDriveMembersOnlyRestriction()
        );

        target.setCanChangeSharingFoldersRequiresOrganizerPermissionRestriction(
                source.canChangeSharingFoldersRequiresOrganizerPermissionRestriction()
        );

        target.setCanResetDriveRestrictions(
                source.canResetDriveRestrictions()
        );

        target.setCanDeleteChildren(
                source.canDeleteChildren()
        );

        target.setCanTrashChildren(
                source.canTrashChildren()
        );

        target.setCanChangeDownloadRestriction(
                source.canChangeDownloadRestriction()
        );

        return target;
    }

    public GoogleDriveSourceRestrictions
    toSharedDriveRestrictions(
            GoogleDriveSharedDriveRestrictionsResponse source
    ) {

        GoogleDriveSourceRestrictions target =
                new GoogleDriveSourceRestrictions();

        if (source == null) {
            return target;
        }

        target.setCopyRequiresWriterPermission(
                source.copyRequiresWriterPermission()
        );

        target.setDomainUsersOnly(
                source.domainUsersOnly()
        );

        target.setDriveMembersOnly(
                source.driveMembersOnly()
        );

        target.setAdminManagedRestrictions(
                source.adminManagedRestrictions()
        );

        target.setSharingFoldersRequiresOrganizerPermission(
                source.sharingFoldersRequiresOrganizerPermission()
        );

        if (source.downloadRestriction() != null) {

            target.setDownloadRestrictedForReaders(
                    source.downloadRestriction()
                            .restrictedForReaders()
            );

            target.setDownloadRestrictedForWriters(
                    source.downloadRestriction()
                            .restrictedForWriters()
            );
        }

        return target;
    }
}