package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItemSourceType;

import java.util.List;

public interface GoogleDriveItemIndexService {

    int upsertItems(
            GoogleDriveConnection connection,
            List<GoogleDriveFileResponse> files,
            GoogleDriveItemSourceType sourceType,
            String driveId,
            String syncRunId
    );

    int deleteStaleItems(
            Long connectionId,
            String syncRunId
    );
}