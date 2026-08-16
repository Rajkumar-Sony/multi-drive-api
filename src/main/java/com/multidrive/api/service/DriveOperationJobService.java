package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.dto.DriveOperationJobSubmitRequest;
import com.multidrive.api.dto.DriveOperationJobsPageResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;

public interface DriveOperationJobService {

	DriveOperationJobResponse submit(String googleSubjectId, String idempotencyKey,
			DriveOperationJobSubmitRequest request);

	DriveOperationJobResponse getJob(String googleSubjectId, Long jobId);

	DriveOperationJobsPageResponse getJobs(String googleSubjectId, DriveOperationJobStatus status, Integer page,
			Integer size);

	DriveOperationJobResponse cancel(String googleSubjectId, Long jobId);

}
