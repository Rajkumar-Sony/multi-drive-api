package com.multidrive.api.exception;

public class DriveOperationLeaseLostException extends RuntimeException {

	public DriveOperationLeaseLostException(Long jobId) {

		super("Drive operation worker lease was lost for job " + jobId);
	}

}
