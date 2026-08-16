package com.multidrive.api.controller;

import com.multidrive.api.dto.GoogleDriveAccountResponse;
import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.dto.GoogleSharedDriveResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveConnectionService;
import com.multidrive.api.service.GoogleDriveOAuthService;
import com.multidrive.api.service.GoogleDriveService;
import com.multidrive.api.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleAccountController.class)
class GoogleAccountControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	private static final Long USER_ID = 42L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleDriveOAuthService googleDriveOAuthService;

	@MockitoBean
	private GoogleDriveConnectionService googleDriveConnectionService;

	@MockitoBean
	private GoogleDriveService googleDriveService;

	@MockitoBean
	private UserService userService;

	@Test
	void connectGoogleDriveStoresStateAndRedirectsToGoogleAuthorizationUrl() throws Exception {

		when(googleDriveOAuthService.buildAuthorizationUrl(anyString()))
			.thenReturn("https://accounts.google.com/o/oauth2");

		MvcResult mvcResult = mockMvc.perform(get("/api/google/accounts/connect").with(oidcLogin()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("https://accounts.google.com/o/oauth2"))
			.andReturn();

		HttpSession session = mvcResult.getRequest().getSession(false);

		assertThat(session).isNotNull();
		assertThat(session.getAttribute("GOOGLE_DRIVE_OAUTH_STATE")).isInstanceOf(String.class);

		verify(googleDriveOAuthService).buildAuthorizationUrl(anyString());
	}

	@Test
	void getConnectedAccountsReturnsAccountsForApplicationUser() throws Exception {

		User user = user();

		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);
		when(googleDriveConnectionService.getConnectedAccounts(USER_ID))
			.thenReturn(List.of(new GoogleDriveAccountResponse(10L, "drive@example.com", "CONNECTED",
					LocalDateTime.of(2026, 8, 16, 12, 0))));

		mockMvc
			.perform(get("/api/google/accounts").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].connectionId").value(10))
			.andExpect(jsonPath("$[0].email").value("drive@example.com"));

		verify(googleDriveConnectionService).getConnectedAccounts(USER_ID);
	}

	@Test
	void getFilesReturnsFilesForConnection() throws Exception {

		User user = user();

		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);
		when(googleDriveService.getFiles(10L, USER_ID, 25, "next-token"))
			.thenReturn(new GoogleDriveFilesResponse(List.of(file()), "another-token", false));

		mockMvc
			.perform(get("/api/google/accounts/{connectionId}/files", 10L).param("pageSize", "25")
				.param("pageToken", "next-token")
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.files[0].id").value("google-file-id"))
			.andExpect(jsonPath("$.nextPageToken").value("another-token"));

		verify(googleDriveService).getFiles(10L, USER_ID, 25, "next-token");
	}

	@Test
	void getSharedDrivesReturnsDrivesForConnection() throws Exception {

		User user = user();

		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);
		when(googleDriveService.getSharedDrives(10L, USER_ID, 25, "drive-token"))
			.thenReturn(new GoogleSharedDrivesResponse(List.of(sharedDrive()), "next-drive-token"));

		mockMvc
			.perform(get("/api/google/accounts/{connectionId}/drives", 10L).param("pageSize", "25")
				.param("pageToken", "drive-token")
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.drives[0].id").value("drive-id"))
			.andExpect(jsonPath("$.nextPageToken").value("next-drive-token"));

		verify(googleDriveService).getSharedDrives(10L, USER_ID, 25, "drive-token");
	}

	private User user() {

		User user = mock(User.class);

		when(user.getId()).thenReturn(USER_ID);

		return user;
	}

	private GoogleDriveFileResponse file() {

		return new GoogleDriveFileResponse("google-file-id", "Report.pdf", "application/pdf", null, null,
				List.of("parent-id"), null, null, null, "1024", null, false, false, null);
	}

	private GoogleSharedDriveResponse sharedDrive() {

		return new GoogleSharedDriveResponse("drive-id", "Team Drive", false, null, null, null);
	}

}
