package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveChangeTracker;

public interface GoogleDriveChangeService {

    GoogleDriveChangeTracker initializeUserTracker(
            Long connectionId,
            Long userId
    );
}