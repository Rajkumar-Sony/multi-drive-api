package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveSourceType;

import java.util.List;

public interface GoogleDriveFileSearchService {

	List<GoogleDriveFileResponse> findByAppProperty(Long connectionId, Long userId, GoogleDriveSourceType sourceType,
			String googleDriveId, String propertyKey, String propertyValue);

}
