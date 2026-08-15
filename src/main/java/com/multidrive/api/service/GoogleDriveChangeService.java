package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveChangeTracker;

import java.util.List;

public interface GoogleDriveChangeService {

    GoogleDriveChangeTracker initializeUserTracker(
            Long connectionId,
            Long userId
    );

    List<GoogleDriveChangeTracker> initializeSharedDriveTrackers(
            Long connectionId,
            Long userId
    );
}