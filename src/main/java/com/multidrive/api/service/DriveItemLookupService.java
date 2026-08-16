package com.multidrive.api.service;

import com.multidrive.api.dto.DriveItemDetailsResponse;

public interface DriveItemLookupService {

	DriveItemDetailsResponse getItem(String googleSubjectId, Long itemId);

}