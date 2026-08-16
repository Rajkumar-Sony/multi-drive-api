package com.multidrive.api.exception;

public class DriveOperationCleanupRequiredException extends RuntimeException {

	private final String errorCode;

	public DriveOperationCleanupRequiredException(String errorCode, String message) {

		super(message);

		this.errorCode = errorCode;
	}

	public String getErrorCode() {

		return errorCode;
	}

}
