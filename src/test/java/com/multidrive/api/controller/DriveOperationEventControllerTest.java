package com.multidrive.api.controller;

import com.multidrive.api.service.DriveOperationSseService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriveOperationEventController.class)
class DriveOperationEventControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DriveOperationSseService driveOperationSseService;

	@Test
	void subscribeUsesAuthenticatedGoogleSubjectId() throws Exception {

		when(driveOperationSseService.subscribe(GOOGLE_SUBJECT_ID)).thenReturn(new SseEmitter());

		mockMvc
			.perform(get("/api/events/operations").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted());

		verify(driveOperationSseService).subscribe(GOOGLE_SUBJECT_ID);
	}

}
