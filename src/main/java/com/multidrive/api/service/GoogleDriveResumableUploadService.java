package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;

public interface GoogleDriveResumableUploadService {

    GoogleDriveFileResponse upload(
            Long connectionId,
            Long userId,
            String parentGoogleFileId,
            String fileName,
            String contentType,
            long contentLength,
            InputStreamProvider inputStreamProvider
    );
}
