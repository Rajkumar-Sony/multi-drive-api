package com.multidrive.api.controller;

import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveSseService;
import com.multidrive.api.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleDriveEventController.class)
class GoogleDriveEventControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleDriveSseService googleDriveSseService;

	@MockitoBean
	private UserService userService;

	@Test
	void subscribeUsesAuthenticatedApplicationUser() throws Exception {

		User user = mock(User.class);

		when(user.getId()).thenReturn(42L);
		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);
		when(googleDriveSseService.subscribe(42L)).thenReturn(new SseEmitter());

		mockMvc.perform(get("/api/events/drive").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted());

		verify(googleDriveSseService).subscribe(42L);
	}

}
