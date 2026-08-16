package com.multidrive.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class DriveOperationNotAllowedException extends RuntimeException {

	public DriveOperationNotAllowedException(String operation) {

		super("Google Drive operation is not allowed: " + operation);
	}

	public DriveOperationNotAllowedException(String operation, String message) {

		super("Google Drive operation is not allowed: " + operation + ". " + message);
	}

}