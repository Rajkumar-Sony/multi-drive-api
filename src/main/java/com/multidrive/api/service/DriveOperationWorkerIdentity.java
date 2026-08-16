package com.multidrive.api.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DriveOperationWorkerIdentity {

	private final String workerId = "worker-" + UUID.randomUUID();

	public String getWorkerId() {
		return workerId;
	}

}
