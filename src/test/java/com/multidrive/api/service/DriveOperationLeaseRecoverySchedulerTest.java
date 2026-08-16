package com.multidrive.api.service;

import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DriveOperationLeaseRecoverySchedulerTest {

	@Test
	void recoverExpiredLeasesUsesCurrentTimeAndShortCooldown() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationLeaseRecoveryScheduler scheduler = new DriveOperationLeaseRecoveryScheduler(store);

		scheduler.recoverExpiredLeases();

		ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		ArgumentCaptor<LocalDateTime> nextAttemptCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

		verify(store).recoverExpiredLeases(nowCaptor.capture(), nextAttemptCaptor.capture());
		assertThat(nextAttemptCaptor.getValue()).isAfter(nowCaptor.getValue());
		assertThat(java.time.Duration.between(nowCaptor.getValue(), nextAttemptCaptor.getValue()).getSeconds())
			.isEqualTo(5);
	}

}
