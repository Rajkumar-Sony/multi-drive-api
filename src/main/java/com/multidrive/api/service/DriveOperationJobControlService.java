package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationJobResponse;

public interface DriveOperationJobControlService {

	DriveOperationJobResponse requestCancellation(String googleSubjectId, Long jobId);

}
