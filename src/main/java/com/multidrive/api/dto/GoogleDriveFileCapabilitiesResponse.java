package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveFileCapabilitiesResponse(

		Boolean canMoveChildrenOutOfDrive,

		Boolean canReadDrive,

		Boolean canEdit,

		Boolean canCopy,

		Boolean canComment,

		Boolean canAddChildren,

		Boolean canDelete,

		Boolean canDownload,

		Boolean canListChildren,

		Boolean canRemoveChildren,

		Boolean canRename,

		Boolean canTrash,

		Boolean canReadRevisions,

		Boolean canChangeCopyRequiresWriterPermission,

		Boolean canUntrash,

		Boolean canModifyContent,

		Boolean canDeleteChildren,

		Boolean canTrashChildren,

		Boolean canMoveItemOutOfDrive,

		Boolean canAddMyDriveParent,

		Boolean canRemoveMyDriveParent,

		Boolean canMoveItemWithinDrive,

		Boolean canShare,

		Boolean canMoveChildrenWithinDrive,

		Boolean canAddFolderFromAnotherDrive,

		Boolean canChangeSecurityUpdateEnabled,

		Boolean canAcceptOwnership,

		Boolean canReadLabels,

		Boolean canModifyLabels,

		Boolean canModifyEditorContentRestriction,

		Boolean canModifyOwnerContentRestriction,

		Boolean canRemoveContentRestriction,

		Boolean canDisableInheritedPermissions,

		Boolean canEnableInheritedPermissions,

		Boolean canChangeItemDownloadRestriction,

		Boolean canStartApproval) {
}