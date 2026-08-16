package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;

import java.util.List;
import java.util.Map;

public interface GoogleDriveFileMutationService {

	GoogleDriveFileResponse getFile(Long connectionId, Long userId, String googleFileId);

	GoogleDriveFileResponse createFolder(Long connectionId, Long userId, String parentGoogleFileId, String name);

	GoogleDriveFileResponse rename(Long connectionId, Long userId, String googleFileId, String name);

	GoogleDriveFileResponse move(Long connectionId, Long userId, String googleFileId,
			String destinationParentGoogleFileId, List<String> currentParentGoogleFileIds);

	GoogleDriveFileResponse copy(Long connectionId, Long userId, String googleFileId,
			String destinationParentGoogleFileId, String name);

	GoogleDriveFileResponse copyWithAppProperties(Long connectionId, Long userId, String googleFileId,
			String destinationParentGoogleFileId, String name, Map<String, String> appProperties);

	GoogleDriveFileResponse trash(Long connectionId, Long userId, String googleFileId);

	GoogleDriveFileResponse restore(Long connectionId, Long userId, String googleFileId);

	void permanentlyDelete(Long connectionId, Long userId, String googleFileId);

}
