package com.multidrive.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DriveOperationWorkerIdentityTest {

	@Test
	void getWorkerIdReturnsStableWorkerPrefixedIdentifier() {

		DriveOperationWorkerIdentity identity = new DriveOperationWorkerIdentity();

		String workerId = identity.getWorkerId();

		assertThat(workerId).startsWith("worker-");
		assertThat(identity.getWorkerId()).isEqualTo(workerId);
	}

}
