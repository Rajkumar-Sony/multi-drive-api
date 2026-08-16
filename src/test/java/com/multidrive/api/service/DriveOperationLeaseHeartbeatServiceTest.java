package com.multidrive.api.service;

import com.multidrive.api.repository.DriveOperationJobExecutionStore;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DriveOperationLeaseHeartbeatServiceTest {

	@Test
	void heartbeatDoesNothingWhenNoJobsAreRegistered() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationLeaseHeartbeatService service = new DriveOperationLeaseHeartbeatService(store);

		service.heartbeat();

		verifyNoInteractions(store);
	}

	@Test
	void heartbeatExtendsRegisteredLeasesAndRemovesLostLeases() {

		DriveOperationJobExecutionStore store = mock(DriveOperationJobExecutionStore.class);
		DriveOperationLeaseHeartbeatService service = new DriveOperationLeaseHeartbeatService(store);

		when(store.extendLease(eq(10L), eq("worker-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(true);
		when(store.extendLease(eq(11L), eq("worker-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
			.thenReturn(false);

		service.register(10L, "worker-1");
		service.register(11L, "worker-1");
		service.heartbeat();
		service.heartbeat();

		verify(store, times(2)).extendLease(eq(10L), eq("worker-1"), any(LocalDateTime.class),
				any(LocalDateTime.class));
		verify(store).extendLease(eq(11L), eq("worker-1"), any(LocalDateTime.class), any(LocalDateTime.class));
	}

}
