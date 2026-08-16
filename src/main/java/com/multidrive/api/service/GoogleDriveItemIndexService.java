package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.entity.GoogleDriveTrackerType;

import java.util.List;

public interface GoogleDriveItemIndexService {

    int upsertItems(
            GoogleDriveConnection connection,
            List<GoogleDriveFileResponse> files,
            GoogleDriveItemSourceType sourceType,
            String driveId,
            String syncRunId
    );

    int applyChanges(
            GoogleDriveConnection connection,
            GoogleDriveTrackerType trackerType,
            String trackerDriveId,
            List<GoogleDriveChangeResponse> changes
    );

    int deleteStaleItems(
            Long connectionId,
            String syncRunId
    );
}