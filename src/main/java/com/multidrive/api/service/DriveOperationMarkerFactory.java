package com.multidrive.api.service;

import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class DriveOperationMarkerFactory {

	public static final String APP_PROPERTY_KEY = "multiDriveOp";

	public String create(DriveOperationJobExecutionSnapshot job) {

		if (job == null) {

			throw new IllegalArgumentException("job is required");
		}

		return create(job, job.sourceGoogleFileId());
	}

	public String create(DriveOperationJobExecutionSnapshot job, String sourceGoogleFileId) {

		if (job == null || job.jobId() == null || job.userId() == null || job.createdAt() == null
				|| job.destinationSourceId() == null || sourceGoogleFileId == null || sourceGoogleFileId.isBlank()) {

			throw new IllegalArgumentException("Operation marker information is incomplete");
		}

		String rawValue = job.userId() + "|" + job.jobId() + "|" + job.createdAt() + "|" + sourceGoogleFileId + "|"
				+ job.destinationSourceId();

		try {

			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			byte[] hash = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(hash);

		}
		catch (NoSuchAlgorithmException exception) {

			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

}
