package com.multidrive.api.controller;

import com.multidrive.api.dto.GoogleDriveSourceResponse;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.entity.User;
import com.multidrive.api.security.AuthenticatedUserResolver;
import com.multidrive.api.service.GoogleDriveSourceService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleDriveSourceController.class)
class GoogleDriveSourceControllerTest {

	private static final Long USER_ID = 42L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleDriveSourceService googleDriveSourceService;

	@MockitoBean
	private AuthenticatedUserResolver authenticatedUserResolver;

	@Test
	void getSourcesReturnsAllActiveSourcesForUser() throws Exception {

		User user = user();

		GoogleDriveSourceResponse source = source(10L, 20L, "My Drive");

		when(authenticatedUserResolver.requireApplicationUser(org.mockito.ArgumentMatchers.any())).thenReturn(user);

		when(googleDriveSourceService.getActiveSourceResponses(USER_ID)).thenReturn(List.of(source));

		mockMvc.perform(get("/api/google/sources").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(10))
			.andExpect(jsonPath("$[0].name").value("My Drive"));

		verify(googleDriveSourceService).getActiveSourceResponses(USER_ID);
	}

	@Test
	void getSourcesFiltersByConnectionWhenRequested() throws Exception {

		User user = user();

		when(authenticatedUserResolver.requireApplicationUser(org.mockito.ArgumentMatchers.any())).thenReturn(user);

		when(googleDriveSourceService.getActiveSourceResponses(USER_ID, 20L))
			.thenReturn(List.of(source(10L, 20L, "Shared Drive")));

		mockMvc.perform(get("/api/google/sources").param("connectionId", "20").with(oidcLogin()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].connectionId").value(20));

		verify(googleDriveSourceService).getActiveSourceResponses(USER_ID, 20L);
	}

	private User user() {

		User user = mock(User.class);

		when(user.getId()).thenReturn(USER_ID);

		return user;
	}

	private GoogleDriveSourceResponse source(Long id, Long connectionId, String name) {

		return new GoogleDriveSourceResponse(id, connectionId, "user@example.com", GoogleDriveSourceType.MY_DRIVE, null,
				"root", name, GoogleDriveSourceStatus.ACTIVE, LocalDateTime.of(2026, 8, 16, 12, 0));
	}

}
