package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveRealtimeEventResponse;
import com.multidrive.api.service.GoogleDriveSseService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class GoogleDriveSseServiceImpl implements GoogleDriveSseService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleDriveSseServiceImpl.class);

	/*
	 * Keep each SSE connection open for 30 minutes.
	 *
	 * Browser EventSource will reconnect automatically when we build the frontend.
	 */
	private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

	/*
	 * Tell EventSource to retry after 3 seconds if the connection is interrupted.
	 */
	private static final long SSE_RECONNECT_TIME_MILLIS = 3000L;

	/*
	 * One application user can have multiple browser tabs/windows open, so each user can
	 * have multiple SseEmitter connections.
	 */
	private final ConcurrentMap<Long, Set<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

	@Override
	public SseEmitter subscribe(Long userId) {

		if (userId == null) {

			throw new IllegalArgumentException("userId is required");
		}

		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

		Set<SseEmitter> userEmitters = emittersByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());

		userEmitters.add(emitter);

		emitter.onCompletion(() -> removeEmitter(userId, emitter));

		emitter.onTimeout(() -> removeEmitter(userId, emitter));

		emitter.onError(error -> removeEmitter(userId, emitter));

		sendConnectedEvent(userId, emitter);

		LOGGER.info("SSE client subscribed. " + "userId={}, activeConnections={}", userId, userEmitters.size());

		return emitter;
	}

	@Override
	public void publishDriveChanges(Long userId, GoogleDriveRealtimeEventResponse event) {

		if (userId == null || event == null) {

			return;
		}

		Set<SseEmitter> emitters = emittersByUser.get(userId);

		if (emitters == null || emitters.isEmpty()) {

			LOGGER.debug("No active SSE subscribers for userId={}", userId);

			return;
		}

		for (SseEmitter emitter : Set.copyOf(emitters)) {

			try {

				emitter.send(SseEmitter.event()
					.id(UUID.randomUUID().toString())
					.name("drive-change")
					.reconnectTime(SSE_RECONNECT_TIME_MILLIS)
					.data(event));

			}
			catch (IOException | IllegalStateException exception) {

				removeEmitter(userId, emitter);

				LOGGER.debug("Removed disconnected SSE client. " + "userId={}", userId);
			}
		}
	}

	/*
	 * Spring MVC recommends periodic writes for long-running streaming responses.
	 *
	 * This heartbeat also helps detect browser clients that have disconnected.
	 */
	@Scheduled(fixedDelay = 25_000L)
	public void sendHeartbeat() {

		for (Map.Entry<Long, Set<SseEmitter>> entry : emittersByUser.entrySet()) {

			Long userId = entry.getKey();

			Set<SseEmitter> emitters = entry.getValue();

			for (SseEmitter emitter : Set.copyOf(emitters)) {

				try {

					emitter.send(SseEmitter.event().name("heartbeat").data("keep-alive"));

				}
				catch (IOException | IllegalStateException exception) {

					removeEmitter(userId, emitter);
				}
			}
		}
	}

	private void sendConnectedEvent(Long userId, SseEmitter emitter) {

		try {

			emitter.send(SseEmitter.event()
				.name("connected")
				.reconnectTime(SSE_RECONNECT_TIME_MILLIS)
				.data(Map.of("status", "CONNECTED")));

		}
		catch (IOException | IllegalStateException exception) {

			removeEmitter(userId, emitter);
		}
	}

	private void removeEmitter(Long userId, SseEmitter emitter) {

		Set<SseEmitter> emitters = emittersByUser.get(userId);

		if (emitters == null) {
			return;
		}

		emitters.remove(emitter);

		if (emitters.isEmpty()) {

			emittersByUser.remove(userId, emitters);
		}
	}

}