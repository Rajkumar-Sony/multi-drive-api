package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;

public interface GoogleDriveFileMutationService {

    GoogleDriveFileResponse getFile(
            Long connectionId,
            Long userId,
            String googleFileId
    );

    GoogleDriveFileResponse createFolder(
            Long connectionId,
            Long userId,
            String parentGoogleFileId,
            String name
    );

    GoogleDriveFileResponse rename(
            Long connectionId,
            Long userId,
            String googleFileId,
            String name
    );
}