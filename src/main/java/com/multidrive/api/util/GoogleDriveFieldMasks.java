package com.multidrive.api.util;

public final class GoogleDriveFieldMasks {

    private GoogleDriveFieldMasks() {
    }

    public static final String FILE_CAPABILITIES =
            "canMoveChildrenOutOfDrive,"
                    + "canReadDrive,"
                    + "canEdit,"
                    + "canCopy,"
                    + "canComment,"
                    + "canAddChildren,"
                    + "canDelete,"
                    + "canDownload,"
                    + "canListChildren,"
                    + "canRemoveChildren,"
                    + "canRename,"
                    + "canTrash,"
                    + "canReadRevisions,"
                    + "canChangeCopyRequiresWriterPermission,"
                    + "canUntrash,"
                    + "canModifyContent,"
                    + "canDeleteChildren,"
                    + "canTrashChildren,"
                    + "canMoveItemOutOfDrive,"
                    + "canAddMyDriveParent,"
                    + "canRemoveMyDriveParent,"
                    + "canMoveItemWithinDrive,"
                    + "canShare,"
                    + "canMoveChildrenWithinDrive,"
                    + "canAddFolderFromAnotherDrive,"
                    + "canChangeSecurityUpdateEnabled,"
                    + "canAcceptOwnership,"
                    + "canReadLabels,"
                    + "canModifyLabels,"
                    + "canModifyEditorContentRestriction,"
                    + "canModifyOwnerContentRestriction,"
                    + "canRemoveContentRestriction,"
                    + "canDisableInheritedPermissions,"
                    + "canEnableInheritedPermissions,"
                    + "canChangeItemDownloadRestriction,"
                    + "canStartApproval";

    public static final String FILE_RESOURCE =
            "id,"
                    + "name,"
                    + "mimeType,"
                    + "createdTime,"
                    + "modifiedTime,"
                    + "parents,"
                    + "webViewLink,"
                    + "thumbnailLink,"
                    + "iconLink,"
                    + "size,"
                    + "driveId,"
                    + "trashed,"
                    + "explicitlyTrashed,"
                    + "capabilities("
                    + FILE_CAPABILITIES
                    + ")";

    public static final String FILE_LIST =
            "nextPageToken,"
                    + "incompleteSearch,"
                    + "files("
                    + FILE_RESOURCE
                    + ")";

    public static final String CHANGE_LIST =
            "nextPageToken,"
                    + "newStartPageToken,"
                    + "changes("
                    + "removed,"
                    + "fileId,"
                    + "time,"
                    + "driveId,"
                    + "changeType,"
                    + "file("
                    + FILE_RESOURCE
                    + "),"
                    + "drive("
                    + "id,"
                    + "name"
                    + ")"
                    + ")";

    public static final String MY_DRIVE_ROOT =
            "id,"
                    + "name,"
                    + "createdTime,"
                    + "capabilities("
                    + FILE_CAPABILITIES
                    + ")";

    public static final String SHARED_DRIVE_CAPABILITIES =
            "canAddChildren,"
                    + "canComment,"
                    + "canCopy,"
                    + "canDeleteDrive,"
                    + "canDownload,"
                    + "canEdit,"
                    + "canListChildren,"
                    + "canManageMembers,"
                    + "canReadRevisions,"
                    + "canRename,"
                    + "canRenameDrive,"
                    + "canChangeDriveBackground,"
                    + "canShare,"
                    + "canChangeCopyRequiresWriterPermissionRestriction,"
                    + "canChangeDomainUsersOnlyRestriction,"
                    + "canChangeDriveMembersOnlyRestriction,"
                    + "canChangeSharingFoldersRequiresOrganizerPermissionRestriction,"
                    + "canResetDriveRestrictions,"
                    + "canDeleteChildren,"
                    + "canTrashChildren,"
                    + "canChangeDownloadRestriction";

    public static final String SHARED_DRIVE_RESTRICTIONS =
            "copyRequiresWriterPermission,"
                    + "domainUsersOnly,"
                    + "driveMembersOnly,"
                    + "adminManagedRestrictions,"
                    + "sharingFoldersRequiresOrganizerPermission,"
                    + "downloadRestriction("
                    + "restrictedForReaders,"
                    + "restrictedForWriters"
                    + ")";

    public static final String SHARED_DRIVE_RESOURCE =
            "id,"
                    + "name,"
                    + "hidden,"
                    + "createdTime,"
                    + "capabilities("
                    + SHARED_DRIVE_CAPABILITIES
                    + "),"
                    + "restrictions("
                    + SHARED_DRIVE_RESTRICTIONS
                    + ")";

    public static final String SHARED_DRIVE_LIST =
            "nextPageToken,"
                    + "drives("
                    + SHARED_DRIVE_RESOURCE
                    + ")";
}
