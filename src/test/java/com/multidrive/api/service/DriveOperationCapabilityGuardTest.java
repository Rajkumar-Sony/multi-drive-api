package com.multidrive.api.service;

import com.multidrive.api.exception.DriveOperationNotAllowedException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriveOperationCapabilityGuardTest {

	private final DriveOperationCapabilityGuard guard = new DriveOperationCapabilityGuard();

	@Test
	void allowsOnlyTrueCapability() {

		guard.requireAllowed(true, "MOVE");
	}

	@Test
	void rejectsFalseOrUnknownCapabilities() {

		assertThatThrownBy(() -> guard.requireAllowed(false, "MOVE"))
			.isInstanceOf(DriveOperationNotAllowedException.class)
			.hasMessage("Google Drive operation is not allowed: MOVE");

		assertThatThrownBy(() -> guard.requireAllowed(null, "COPY", "Missing permission"))
			.isInstanceOf(DriveOperationNotAllowedException.class)
			.hasMessage("Google Drive operation is not allowed: COPY. Missing permission");
	}

}
