package com.multidrive.api.mapper;

import com.multidrive.api.dto.DriveItemCapabilitiesResponse;
import com.multidrive.api.entity.GoogleDriveItemCapabilities;

import org.springframework.stereotype.Component;

@Component
public class DriveItemCapabilityResponseMapper {

	public DriveItemCapabilitiesResponse toResponse(GoogleDriveItemCapabilities capabilities) {

		if (capabilities == null) {

			return new DriveItemCapabilitiesResponse(null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null);
		}

		return new DriveItemCapabilitiesResponse(

				capabilities.getCanReadDrive(),

				capabilities.getCanEdit(),

				capabilities.getCanModifyContent(),

				capabilities.getCanRename(),

				capabilities.getCanCopy(),

				capabilities.getCanDownload(),

				capabilities.getCanComment(),

				capabilities.getCanShare(),

				capabilities.getCanTrash(),

				capabilities.getCanUntrash(),

				capabilities.getCanDelete(),

				capabilities.getCanAddChildren(),

				capabilities.getCanDeleteChildren(),

				capabilities.getCanTrashChildren(),

				capabilities.getCanListChildren(),

				capabilities.getCanRemoveChildren(),

				capabilities.getCanReadRevisions(),

				capabilities.getCanReadLabels(),

				capabilities.getCanModifyLabels(),

				capabilities.getCanMoveItemWithinDrive(),

				capabilities.getCanMoveItemOutOfDrive(),

				capabilities.getCanMoveChildrenWithinDrive(),

				capabilities.getCanMoveChildrenOutOfDrive(),

				capabilities.getCanAddFolderFromAnotherDrive(),

				capabilities.getCanAddMyDriveParent(),

				capabilities.getCanRemoveMyDriveParent(),

				capabilities.getCanChangeSecurityUpdateEnabled(),

				capabilities.getCanChangeItemDownloadRestriction(),

				capabilities.getCanAcceptOwnership(),

				capabilities.getCanDisableInheritedPermissions(),

				capabilities.getCanEnableInheritedPermissions(),

				capabilities.getCanStartApproval());
	}

}