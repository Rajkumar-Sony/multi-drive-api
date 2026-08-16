package com.multidrive.api.service;

import com.multidrive.api.dto.DriveOperationEventResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.service.impl.DriveOperationSseServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriveOperationSseServiceImplTest {

	@Test
	void subscribeValidatesSubjectAndRegistersEmitter() {

		DriveOperationSseServiceImpl service = new DriveOperationSseServiceImpl();

		assertThatThrownBy(() -> service.subscribe(" ")).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("googleSubjectId is required");

		SseEmitter emitter = service.subscribe("google-subject-1");

		assertThat(emitters(service).get("google-subject-1")).containsExactly(emitter);
	}

	@Test
	void publishRemovesEmitterThatFailsDuringSend() {

		DriveOperationSseServiceImpl service = new DriveOperationSseServiceImpl();
		FailingSseEmitter emitter = new FailingSseEmitter();
		emitters(service).put("google-subject-1", new CopyOnWriteArrayList<>(java.util.List.of(emitter)));

		service.publish("google-subject-1", event());

		assertThat(emitters(service)).doesNotContainKey("google-subject-1");
		assertThat(emitter.sendCount).isEqualTo(1);
	}

	@Test
	void heartbeatRemovesEmitterThatFailsDuringSend() {

		DriveOperationSseServiceImpl service = new DriveOperationSseServiceImpl();
		FailingSseEmitter emitter = new FailingSseEmitter();
		emitters(service).put("google-subject-1", new CopyOnWriteArrayList<>(java.util.List.of(emitter)));

		service.heartbeat();

		assertThat(emitters(service)).doesNotContainKey("google-subject-1");
	}

	private DriveOperationEventResponse event() {

		return new DriveOperationEventResponse("JOB_PROGRESS", 10L, DriveOperationJobStatus.RUNNING, 2L, 1L, 0L, 100L,
				50L, 1, 3, false, null, null, "running", Instant.now());
	}

	@SuppressWarnings("unchecked")
	private ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters(DriveOperationSseServiceImpl service) {

		return (ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>>) ReflectionTestUtils.getField(service,
				"emitters");
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
