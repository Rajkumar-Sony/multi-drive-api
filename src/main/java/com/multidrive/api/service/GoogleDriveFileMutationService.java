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

    GoogleDriveFileResponse trash(
            Long connectionId,
            Long userId,
            String googleFileId
    );

    GoogleDriveFileResponse restore(
            Long connectionId,
            Long userId,
            String googleFileId
    );

    void permanentlyDelete(
            Long connectionId,
            Long userId,
            String googleFileId
    );
}
