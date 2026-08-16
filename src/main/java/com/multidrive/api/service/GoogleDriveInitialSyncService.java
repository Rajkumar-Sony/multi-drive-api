package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveInitialSyncResponse;

public interface GoogleDriveInitialSyncService {

	GoogleDriveInitialSyncResponse syncConnection(Long connectionId, Long userId);

}