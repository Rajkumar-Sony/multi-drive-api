package com.multidrive.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleDriveSharedDriveCapabilitiesResponse(

		Boolean canAddChildren,

		Boolean canComment,

		Boolean canCopy,

		Boolean canDeleteDrive,

		Boolean canDownload,

		Boolean canEdit,

		Boolean canListChildren,

		Boolean canManageMembers,

		Boolean canReadRevisions,

		Boolean canRename,

		Boolean canRenameDrive,

		Boolean canChangeDriveBackground,

		Boolean canShare,

		Boolean canChangeCopyRequiresWriterPermissionRestriction,

		Boolean canChangeDomainUsersOnlyRestriction,

		Boolean canChangeDriveMembersOnlyRestriction,

		Boolean canChangeSharingFoldersRequiresOrganizerPermissionRestriction,

		Boolean canResetDriveRestrictions,

		Boolean canDeleteChildren,

		Boolean canTrashChildren,

		Boolean canChangeDownloadRestriction) {
}