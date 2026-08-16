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

		if (job == null || job.jobId() == null || job.createdAt() == null || job.sourceGoogleFileId() == null) {

			throw new IllegalArgumentException("Operation job marker information is incomplete");
		}

		String rawValue = job.userId() + "|" + job.jobId() + "|" + job.createdAt() + "|" + job.sourceGoogleFileId()
				+ "|" + job.destinationSourceId();

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
