package com.multidrive.api.service;

import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriveOperationMarkerFactoryTest {

	private final DriveOperationMarkerFactory factory = new DriveOperationMarkerFactory();

	@Test
	void createReturnsStableSha256HexMarker() {

		DriveOperationJobExecutionSnapshot job = job(10L, "file-1");

		String first = factory.create(job);
		String second = factory.create(job);

		assertThat(first).isEqualTo(second);
		assertThat(first).hasSize(64);
		assertThat(first).matches("[0-9a-f]{64}");
	}

	@Test
	void createChangesWhenOperationIdentityChanges() {

		assertThat(factory.create(job(10L, "file-1"))).isNotEqualTo(factory.create(job(11L, "file-1")));
		assertThat(factory.create(job(10L, "file-1"))).isNotEqualTo(factory.create(job(10L, "file-2")));
		assertThat(factory.create(job(10L, "file-1"), "child-file-1"))
			.isNotEqualTo(factory.create(job(10L, "file-1"), "child-file-2"));
	}

	@Test
	void createRejectsIncompleteMarkerInformation() {

		assertThatThrownBy(() -> factory.create(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("job is required");

		assertThatThrownBy(() -> factory.create(job(10L, "file-1"), " ")).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Operation marker information is incomplete");
	}

	private DriveOperationJobExecutionSnapshot job(Long jobId, String sourceGoogleFileId) {

		return new DriveOperationJobExecutionSnapshot(jobId, 99L, "google-subject-123", DriveOperationType.COPY,
				DriveOperationStrategyType.NATIVE_COPY, DriveOperationJobStatus.VALIDATING, 123L, 20L, 30L,
				sourceGoogleFileId, "Source file", "application/pdf", 30L, 20L, GoogleDriveSourceType.MY_DRIVE, null,
				456L, "dest-parent", "Copied file", 1, 3, false, null, LocalDateTime.parse("2026-08-17T00:00:00"));
	}

}
