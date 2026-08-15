package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFilesResponse;

public interface GoogleDriveService {

    GoogleDriveFilesResponse getFiles(
            Long connectionId,
            Long userId,
            Integer pageSize,
            String pageToken
    );
}