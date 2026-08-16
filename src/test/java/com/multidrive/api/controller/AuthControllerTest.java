package com.multidrive.api.controller;

import com.multidrive.api.entity.User;
import com.multidrive.api.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	void getCurrentUserReturnsApplicationUser() throws Exception {

		User user = user();

		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);

		mockMvc.perform(get("/api/auth/me").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(42))
			.andExpect(jsonPath("$.email").value("user@example.com"))
			.andExpect(jsonPath("$.name").value("Test User"))
			.andExpect(jsonPath("$.pictureUrl").value("https://example.com/avatar.png"));

		verify(userService).findByGoogleSubjectId(GOOGLE_SUBJECT_ID);
	}

	private User user() {

		User user = mock(User.class);

		when(user.getId()).thenReturn(42L);
		when(user.getEmail()).thenReturn("user@example.com");
		when(user.getName()).thenReturn("Test User");
		when(user.getPictureUrl()).thenReturn("https://example.com/avatar.png");

		return user;
	}

}
