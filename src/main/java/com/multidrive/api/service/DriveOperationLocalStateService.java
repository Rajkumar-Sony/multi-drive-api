package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveItemSourceType;

public interface DriveOperationLocalStateService {

	Long createFolder(Long connectionId, Long sourceId, GoogleDriveItemSourceType sourceType, String driveId,
			GoogleDriveFileResponse remoteFile);

	Long createUploadedItem(Long connectionId, Long sourceId, GoogleDriveItemSourceType sourceType, String driveId,
			GoogleDriveFileResponse remoteFile);

	Long createCopiedItem(Long connectionId, Long sourceId, GoogleDriveItemSourceType sourceType, String driveId,
			GoogleDriveFileResponse remoteFile);

	void updateItem(Long itemId, GoogleDriveFileResponse remoteFile);

	void markTrashed(Long itemId, GoogleDriveFileResponse remoteFile);

	void markRestored(Long itemId, GoogleDriveFileResponse remoteFile);

	void deleteSubtree(Long itemId);

}
