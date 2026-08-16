package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;

public interface GoogleDriveService {

    GoogleDriveFilesResponse getFiles(
            Long connectionId,
            Long userId,
            Integer pageSize,
            String pageToken
    );

    GoogleDriveFilesResponse getMyDriveFiles(
            Long connectionId,
            Long userId,
            Integer pageSize,
            String pageToken
    );

    GoogleDriveFilesResponse getSharedDriveFiles(
            Long connectionId,
            Long userId,
            String driveId,
            Integer pageSize,
            String pageToken
    );

    GoogleSharedDrivesResponse getSharedDrives(
            Long connectionId,
            Long userId,
            Integer pageSize,
            String pageToken
    );
}