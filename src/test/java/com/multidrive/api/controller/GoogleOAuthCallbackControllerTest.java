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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleOAuthCallbackController.class)
class GoogleOAuthCallbackControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	private static final Long USER_ID = 42L;

	private static final Long CONNECTION_ID = 10L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleDriveOAuthService googleDriveOAuthService;

	@MockitoBean
	private GoogleDriveConnectionService googleDriveConnectionService;

	@MockitoBean
	private GoogleDriveChangeService googleDriveChangeService;

	@MockitoBean
	private GoogleDriveInitialSyncService googleDriveInitialSyncService;

	@MockitoBean
	private GoogleDriveWatchService googleDriveWatchService;

	@MockitoBean
	private UserService userService;

	@Test
	void callbackConnectsDriveAndReturnsSyncStatus() throws Exception {

		MockHttpSession session = new MockHttpSession();

		session.setAttribute("GOOGLE_DRIVE_OAUTH_STATE", "state-123");

		User user = user();
		GoogleTokenResponse tokenResponse = new GoogleTokenResponse("access-token", "refresh-token", 3600L,
				"drive.metadata.readonly", "Bearer", "id-token");
		GoogleUserInfoResponse userInfoResponse = new GoogleUserInfoResponse("drive-subject", "drive@example.com",
				"Drive User", null);
		GoogleDriveConnection connection = connection();
		GoogleDriveChangeTracker userTracker = tracker(100L);
		List<GoogleDriveChangeTracker> sharedDriveTrackers = List.of(tracker(101L), tracker(102L));
		List<GoogleDriveWatchChannel> watchChannels = List.of(mock(GoogleDriveWatchChannel.class));

		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);
		when(googleDriveOAuthService.exchangeAuthorizationCode("code-123")).thenReturn(tokenResponse);
		when(googleDriveOAuthService.getUserInfo("access-token")).thenReturn(userInfoResponse);
		when(googleDriveConnectionService.saveOrUpdateConnection(user, userInfoResponse, tokenResponse))
			.thenReturn(connection);
		when(googleDriveChangeService.initializeUserTracker(CONNECTION_ID, USER_ID)).thenReturn(userTracker);
		when(googleDriveChangeService.initializeSharedDriveTrackers(CONNECTION_ID, USER_ID))
			.thenReturn(sharedDriveTrackers);
		when(googleDriveInitialSyncService.syncConnection(CONNECTION_ID, USER_ID))
			.thenReturn(new GoogleDriveInitialSyncResponse(CONNECTION_ID, 3, 1, 2, 5, 0));
		when(googleDriveWatchService.registerWatchChannels(CONNECTION_ID, USER_ID)).thenReturn(watchChannels);

		mockMvc
			.perform(get("/api/google/oauth/callback").param("code", "code-123")
				.param("state", "state-123")
				.session(session)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("SUCCESS"))
			.andExpect(jsonPath("$.connectionId").value(10))
			.andExpect(jsonPath("$.email").value("drive@example.com"))
			.andExpect(jsonPath("$.userTrackerId").value(100))
			.andExpect(jsonPath("$.sharedDriveTrackerCount").value(2))
			.andExpect(jsonPath("$.initialSyncStatus").value("COMPLETED"))
			.andExpect(jsonPath("$.realTimeSyncStatus").value("ACTIVE"));

		verify(googleDriveConnectionService).saveOrUpdateConnection(user, userInfoResponse, tokenResponse);
		verify(googleDriveInitialSyncService).syncConnection(CONNECTION_ID, USER_ID);
		verify(googleDriveWatchService).registerWatchChannels(CONNECTION_ID, USER_ID);
	}

	@Test
	void callbackReturnsBadRequestWhenGoogleAuthorizationFails() throws Exception {

		mockMvc.perform(get("/api/google/oauth/callback").param("error", "access_denied").with(oidcLogin()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("ERROR"))
			.andExpect(jsonPath("$.message").value("Google authorization was cancelled or denied"));

		verifyNoInteractions(userService, googleDriveOAuthService, googleDriveConnectionService);
	}

	@Test
	void callbackReturnsBadRequestWhenStateDoesNotMatch() throws Exception {

		MockHttpSession session = new MockHttpSession();

		session.setAttribute("GOOGLE_DRIVE_OAUTH_STATE", "expected-state");

		mockMvc
			.perform(get("/api/google/oauth/callback").param("code", "code-123")
				.param("state", "actual-state")
				.session(session)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value("ERROR"))
			.andExpect(jsonPath("$.message").value("Invalid OAuth state"));

		verifyNoInteractions(userService, googleDriveOAuthService, googleDriveConnectionService);
	}

	private User user() {

		User user = mock(User.class);

		when(user.getId()).thenReturn(USER_ID);

		return user;
	}

	private GoogleDriveConnection connection() {

		GoogleDriveConnection connection = mock(GoogleDriveConnection.class);

		when(connection.getId()).thenReturn(CONNECTION_ID);
		when(connection.getGoogleEmail()).thenReturn("drive@example.com");
		when(connection.getStatus()).thenReturn("CONNECTED");

		return connection;
	}

	private GoogleDriveChangeTracker tracker(Long trackerId) {

		GoogleDriveChangeTracker tracker = mock(GoogleDriveChangeTracker.class);

		when(tracker.getId()).thenReturn(trackerId);
		when(tracker.getStatus()).thenReturn("ACTIVE");

		return tracker;
	}

}
