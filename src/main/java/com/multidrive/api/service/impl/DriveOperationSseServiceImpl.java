package com.multidrive.api.service.impl;

import com.multidrive.api.dto.DriveOperationEventResponse;
import com.multidrive.api.service.DriveOperationSseService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class DriveOperationSseServiceImpl implements DriveOperationSseService {

	private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

	private static final long RECONNECT_TIME_MS = 3000L;

	private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

	@Override
	public SseEmitter subscribe(String googleSubjectId) {

		if (googleSubjectId == null || googleSubjectId.isBlank()) {

			throw new IllegalArgumentException("googleSubjectId is required");
		}

		SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

		CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.computeIfAbsent(googleSubjectId,
				ignored -> new CopyOnWriteArrayList<>());

		userEmitters.add(emitter);

		Runnable cleanup = () -> removeEmitter(googleSubjectId, emitter);

		emitter.onCompletion(cleanup);

		emitter.onTimeout(cleanup);

		emitter.onError(ignored -> cleanup.run());

		try {

			emitter.send(
					SseEmitter.event().name("operation-connected").data("connected").reconnectTime(RECONNECT_TIME_MS));

		}
		catch (IOException exception) {

			removeEmitter(googleSubjectId, emitter);
		}

		return emitter;
	}

	@Override
	public void publish(String googleSubjectId, DriveOperationEventResponse event) {

		if (googleSubjectId == null || event == null) {

			return;
		}

		List<SseEmitter> userEmitters = emitters.get(googleSubjectId);

		if (userEmitters == null || userEmitters.isEmpty()) {

			return;
		}

		for (SseEmitter emitter : userEmitters) {

			try {

				emitter.send(SseEmitter.event()
					.name("operation-progress")
					.id(event.jobId() + "-" + System.nanoTime())
					.data(event));

			}
			catch (Exception exception) {

				removeEmitter(googleSubjectId, emitter);
			}
		}
	}

	@Scheduled(fixedDelay = 25_000)
	public void heartbeat() {

		emitters.forEach((googleSubjectId, userEmitters) -> {

			for (SseEmitter emitter : userEmitters) {

				try {

					emitter.send(SseEmitter.event().name("heartbeat").data("ping"));

				}
				catch (Exception exception) {

					removeEmitter(googleSubjectId, emitter);
				}
			}
		});
	}

	private void removeEmitter(String googleSubjectId, SseEmitter emitter) {

		CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(googleSubjectId);

		if (userEmitters == null) {
			return;
		}

		userEmitters.remove(emitter);

		if (userEmitters.isEmpty()) {

			emitters.remove(googleSubjectId, userEmitters);
		}
	}

}
