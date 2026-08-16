package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveSourceResponse;
import com.multidrive.api.entity.GoogleDriveSource;

import java.util.List;

public interface GoogleDriveSourceService {

	List<GoogleDriveSource> refreshSources(Long connectionId, Long userId);

	List<GoogleDriveSource> getActiveSources(Long connectionId);

	List<GoogleDriveSource> getActiveSharedDriveSources(Long connectionId);

	List<GoogleDriveSourceResponse> getActiveSourceResponses(Long userId);

	List<GoogleDriveSourceResponse> getActiveSourceResponses(Long userId, Long connectionId);

}