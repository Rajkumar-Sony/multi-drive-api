package com.multidrive.api.dto;

public record DriveItemCapabilitiesResponse(

		Boolean readDrive,

		Boolean edit,

		Boolean modifyContent,

		Boolean rename,

		Boolean copy,

		Boolean download,

		Boolean comment,

		Boolean share,

		Boolean trash,

		Boolean untrash,

		Boolean delete,

		Boolean addChildren,

		Boolean deleteChildren,

		Boolean trashChildren,

		Boolean listChildren,

		Boolean removeChildren,

		Boolean readRevisions,

		Boolean readLabels,

		Boolean modifyLabels,

		Boolean moveWithinDrive,

		Boolean moveOutOfDrive,

		Boolean moveChildrenWithinDrive,

		Boolean moveChildrenOutOfDrive,

		Boolean addFolderFromAnotherDrive,

		Boolean addMyDriveParent,

		Boolean removeMyDriveParent,

		Boolean changeSecurityUpdateEnabled,

		Boolean changeItemDownloadRestriction,

		Boolean acceptOwnership,

		Boolean disableInheritedPermissions,

		Boolean enableInheritedPermissions,

		Boolean startApproval) {
}