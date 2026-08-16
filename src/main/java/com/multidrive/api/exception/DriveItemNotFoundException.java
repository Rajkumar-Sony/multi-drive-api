package com.multidrive.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DriveItemNotFoundException extends RuntimeException {

	public DriveItemNotFoundException(Long itemId) {

		super("Drive item not found: " + itemId);
	}

}