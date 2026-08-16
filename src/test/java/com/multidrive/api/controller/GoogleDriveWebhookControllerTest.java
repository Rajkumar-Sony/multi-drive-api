package com.multidrive.api.controller;

import com.multidrive.api.service.GoogleDriveWebhookService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoogleDriveWebhookController.class)
class GoogleDriveWebhookControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleDriveWebhookService googleDriveWebhookService;

	@Test
	void receiveDriveNotificationDelegatesHeadersAndReturnsNoContent() throws Exception {

		mockMvc
			.perform(post("/api/google/webhooks/drive").with(oidcLogin())
				.with(csrf())
				.header("X-Goog-Channel-ID", "channel-id")
				.header("X-Goog-Channel-Token", "channel-token")
				.header("X-Goog-Resource-ID", "resource-id")
				.header("X-Goog-Resource-State", "change")
				.header("X-Goog-Message-Number", "7"))
			.andExpect(status().isNoContent());

		verify(googleDriveWebhookService).handleNotification("channel-id", "channel-token", "resource-id", "change",
				"7");
	}

	@Test
	void receiveDriveNotificationReturnsForbiddenWhenTokenValidationFails() throws Exception {

		doThrow(new SecurityException("invalid channel token")).when(googleDriveWebhookService)
			.handleNotification(null, null, null, null, null);

		mockMvc.perform(post("/api/google/webhooks/drive").with(oidcLogin()).with(csrf()))
			.andExpect(status().isForbidden());
	}

	@Test
	void receiveDriveNotificationReturnsBadRequestWhenNotificationIsMalformed() throws Exception {

		doThrow(new IllegalArgumentException("channel id is required")).when(googleDriveWebhookService)
			.handleNotification(null, null, null, null, null);

		mockMvc.perform(post("/api/google/webhooks/drive").with(oidcLogin()).with(csrf()))
			.andExpect(status().isBadRequest());
	}

}
