package com.multidrive.api.service;

import com.multidrive.api.exception.DriveOperationNotAllowedException;

import org.springframework.stereotype.Component;

@Component
public class DriveOperationCapabilityGuard {

	public void requireAllowed(Boolean capability, String operation) {

		if (!Boolean.TRUE.equals(capability)) {

			throw new DriveOperationNotAllowedException(operation);
		}
	}

	public void requireAllowed(Boolean capability, String operation, String message) {

		if (!Boolean.TRUE.equals(capability)) {

			throw new DriveOperationNotAllowedException(operation, message);
		}
	}

}