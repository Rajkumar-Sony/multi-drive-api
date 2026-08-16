package com.multidrive.api.controller;

import com.multidrive.api.dto.GoogleDriveInitialSyncResponse;
import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveWatchChannel;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveChangeService;
import com.multidrive.api.service.GoogleDriveConnectionService;
import com.multidrive.api.service.GoogleDriveInitialSyncService;
import com.multidrive.api.service.GoogleDriveOAuthService;
import com.multidrive.api.service.GoogleDriveWatchService;
import com.multidrive.api.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/google/oauth")
public class GoogleOAuthCallbackController {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthCallbackController.class);

	private static final String OAUTH_STATE_SESSION_KEY = "GOOGLE_DRIVE_OAUTH_STATE";

	private final GoogleDriveOAuthService googleDriveOAuthService;

	private final GoogleDriveConnectionService googleDriveConnectionService;

	private final GoogleDriveChangeService googleDriveChangeService;

	private final GoogleDriveInitialSyncService googleDriveInitialSyncService;

	private final GoogleDriveWatchService googleDriveWatchService;

	private final UserService userService;

	public GoogleOAuthCallbackController(GoogleDriveOAuthService googleDriveOAuthService,

			GoogleDriveConnectionService googleDriveConnectionService,

			GoogleDriveChangeService googleDriveChangeService,

			GoogleDriveInitialSyncService googleDriveInitialSyncService,

			GoogleDriveWatchService googleDriveWatchService,

			UserService userService) {

		this.googleDriveOAuthService = googleDriveOAuthService;

		this.googleDriveConnectionService = googleDriveConnectionService;

		this.googleDriveChangeService = googleDriveChangeService;

		this.googleDriveInitialSyncService = googleDriveInitialSyncService;

		this.googleDriveWatchService = googleDriveWatchService;

		this.userService = userService;
	}

	@GetMapping("/callback")
	public ResponseEntity<Map<String, Object>> callback(

			@RequestParam(required = false) String code,

			@RequestParam(required = false) String state,

			@RequestParam(required = false) String error,

			HttpSession session,

			@AuthenticationPrincipal OidcUser oidcUser) {

		if (error != null && !error.isBlank()) {

			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Google authorization was cancelled or denied");
		}

		if (oidcUser == null) {

			return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authenticated application user not found");
		}

		Object storedStateObject = session.getAttribute(OAUTH_STATE_SESSION_KEY);

		session.removeAttribute(OAUTH_STATE_SESSION_KEY);

		if (!(storedStateObject instanceof String storedState) || state == null || state.isBlank()
				|| !storedState.equals(state)) {

			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid OAuth state");
		}

		if (code == null || code.isBlank()) {

			return buildErrorResponse(HttpStatus.BAD_REQUEST, "Google authorization code is missing");
		}

		try {

			User applicationUser = userService.findByGoogleSubjectId(oidcUser.getSubject());

			GoogleTokenResponse tokenResponse = googleDriveOAuthService.exchangeAuthorizationCode(code);

			if (tokenResponse == null || tokenResponse.accessToken() == null || tokenResponse.accessToken().isBlank()) {

				throw new IllegalStateException("Google access token is missing");
			}

			GoogleUserInfoResponse googleUserInfo = googleDriveOAuthService.getUserInfo(tokenResponse.accessToken());

			if (googleUserInfo == null || googleUserInfo.sub() == null || googleUserInfo.sub().isBlank()) {

				throw new IllegalStateException("Google account information is missing");
			}

			GoogleDriveConnection connection = googleDriveConnectionService.saveOrUpdateConnection(applicationUser,
					googleUserInfo, tokenResponse);

			GoogleDriveChangeTracker userTracker = googleDriveChangeService.initializeUserTracker(connection.getId(),
					applicationUser.getId());

			List<GoogleDriveChangeTracker> sharedDriveTrackers = googleDriveChangeService
				.initializeSharedDriveTrackers(connection.getId(), applicationUser.getId());

			String initialSyncStatus = "COMPLETED";

			GoogleDriveInitialSyncResponse initialSyncResponse = null;

			try {

				initialSyncResponse = googleDriveInitialSyncService.syncConnection(connection.getId(),
						applicationUser.getId());

			}
			catch (Exception syncException) {

				initialSyncStatus = "FAILED";

				LOGGER.error(
						"Google Drive connected, but initial " + "file synchronization failed. " + "connectionId={}",
						connection.getId(), syncException);
			}

			String realTimeSyncStatus = "ACTIVE";

			int watchChannelCount = 0;

			try {

				List<GoogleDriveWatchChannel> watchChannels = googleDriveWatchService
					.registerWatchChannels(connection.getId(), applicationUser.getId());

				watchChannelCount = watchChannels.size();

			}
			catch (Exception watchException) {

				realTimeSyncStatus = "SETUP_FAILED";

				LOGGER.error(
						"Google Drive connected, but real-time " + "watch registration failed. " + "connectionId={}",
						connection.getId(), watchException);
			}

			Map<String, Object> response = new LinkedHashMap<>();

			response.put("status", "SUCCESS");

			response.put("message", "Google Drive connected successfully");

			response.put("connectionId", connection.getId());

			response.put("email", connection.getGoogleEmail());

			response.put("connectionStatus", connection.getStatus());

			response.put("userTrackerId", userTracker.getId());

			response.put("userTrackerStatus", userTracker.getStatus());

			response.put("sharedDriveTrackerCount", sharedDriveTrackers.size());

			response.put("initialSyncStatus", initialSyncStatus);

			if (initialSyncResponse != null) {

				response.put("myDriveItemCount", initialSyncResponse.myDriveItemCount());

				response.put("sharedDriveCount", initialSyncResponse.sharedDriveCount());

				response.put("sharedDriveItemCount", initialSyncResponse.sharedDriveItemCount());

				response.put("totalItemCount", initialSyncResponse.totalItemCount());

				response.put("staleItemCount", initialSyncResponse.staleItemCount());
			}

			response.put("watchChannelCount", watchChannelCount);

			response.put("realTimeSyncStatus", realTimeSyncStatus);

			return ResponseEntity.ok(response);

		}
		catch (Exception exception) {

			LOGGER.error("Failed to complete Google Drive OAuth callback", exception);

			return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to connect Google Drive");
		}
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {

		Map<String, Object> response = new LinkedHashMap<>();

		response.put("status", "ERROR");

		response.put("message", message);

		return ResponseEntity.status(status).body(response);
	}

}