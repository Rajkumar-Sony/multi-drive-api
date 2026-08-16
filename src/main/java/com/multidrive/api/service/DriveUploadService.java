package com.multidrive.api.service;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveUploadRequest;

public interface DriveUploadService {

    DriveItemDetailsResponse upload(
            String googleSubjectId,
            DriveUploadRequest request
    );
}
