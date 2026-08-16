package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveRealtimeEventResponse;
import com.multidrive.api.entity.GoogleDriveTrackerType;
import com.multidrive.api.service.impl.GoogleDriveSseServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoogleDriveSseServiceImplTest {

	@Test
	void subscribeValidatesUserIdAndRegistersEmitter() {

		GoogleDriveSseServiceImpl service = new GoogleDriveSseServiceImpl();

		assertThatThrownBy(() -> service.subscribe(null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("userId is required");

		SseEmitter emitter = service.subscribe(42L);

		assertThat(emitters(service).get(42L)).containsExactly(emitter);
	}

	@Test
	void publishDriveChangesRemovesEmitterThatFailsDuringSend() {

		GoogleDriveSseServiceImpl service = new GoogleDriveSseServiceImpl();
		FailingSseEmitter emitter = new FailingSseEmitter();
		emitters(service).put(42L, java.util.concurrent.ConcurrentHashMap.newKeySet());
		emitters(service).get(42L).add(emitter);

		service.publishDriveChanges(42L, event());

		assertThat(emitters(service)).doesNotContainKey(42L);
		assertThat(emitter.sendCount).isEqualTo(1);
	}

	@Test
	void sendHeartbeatRemovesEmitterThatFailsDuringSend() {

		GoogleDriveSseServiceImpl service = new GoogleDriveSseServiceImpl();
		FailingSseEmitter emitter = new FailingSseEmitter();
		emitters(service).put(42L, java.util.concurrent.ConcurrentHashMap.newKeySet());
		emitters(service).get(42L).add(emitter);

		service.sendHeartbeat();

		assertThat(emitters(service)).doesNotContainKey(42L);
	}

	private GoogleDriveRealtimeEventResponse event() {

		return new GoogleDriveRealtimeEventResponse("DRIVE_CHANGES", 20L, 30L, GoogleDriveTrackerType.USER, null, 1,
				Instant.now());
	}

	@SuppressWarnings("unchecked")
	private ConcurrentMap<Long, Set<SseEmitter>> emitters(GoogleDriveSseServiceImpl service) {

		return (ConcurrentMap<Long, Set<SseEmitter>>) ReflectionTestUtils.getField(service, "emittersByUser");
	}

	private static final class FailingSseEmitter extends SseEmitter {

		private int sendCount;

		@Override
		public void send(SseEventBuilder builder) throws IOException {

			sendCount++;

			throw new IOException("client disconnected");
		}

	}

}
