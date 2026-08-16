package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveChangeResponse;

import java.util.List;

public interface GoogleDriveChangeProcessingService {

    List<GoogleDriveChangeResponse> processChanges(
            Long trackerId
    );
}