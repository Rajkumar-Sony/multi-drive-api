package com.multidrive.api.controller;

import com.multidrive.api.dto.UnifiedDriveItemsPageResponse;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.UnifiedDriveViewService;
import com.multidrive.api.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UnifiedDriveViewController.class)
class UnifiedDriveViewControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	private static final Long USER_ID = 42L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UnifiedDriveViewService unifiedDriveViewService;

	@MockitoBean
	private UserService userService;

	@BeforeEach
	void setUp() {

		User user = mock(User.class);

		when(user.getId()).thenReturn(USER_ID);
		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);
	}

	@Test
	void getDashboardReturnsDashboardPage() throws Exception {

		when(unifiedDriveViewService.getDashboard(USER_ID, 10L, "parent-id", "report", 1, 25)).thenReturn(page());

		mockMvc
			.perform(get("/api/dashboard").param("connectionId", "10")
				.param("parentId", "parent-id")
				.param("q", "report")
				.param("page", "1")
				.param("size", "25")
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(25));

		verify(unifiedDriveViewService).getDashboard(USER_ID, 10L, "parent-id", "report", 1, 25);
	}

	@Test
	void getDocsReturnsDocsPage() throws Exception {

		when(unifiedDriveViewService.getDocs(USER_ID, null, null, null, 0, 50)).thenReturn(page());

		mockMvc.perform(get("/api/docs").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isArray());

		verify(unifiedDriveViewService).getDocs(USER_ID, null, null, null, 0, 50);
	}

	@Test
	void getGalleryReturnsGalleryPage() throws Exception {

		when(unifiedDriveViewService.getGallery(USER_ID, null, null, null, 0, 50)).thenReturn(page());

		mockMvc.perform(get("/api/gallery").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isArray());

		verify(unifiedDriveViewService).getGallery(USER_ID, null, null, null, 0, 50);
	}

	@Test
	void getVideosReturnsVideosPage() throws Exception {

		when(unifiedDriveViewService.getVideos(USER_ID, null, null, null, 0, 50)).thenReturn(page());

		mockMvc.perform(get("/api/videos").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isArray());

		verify(unifiedDriveViewService).getVideos(USER_ID, null, null, null, 0, 50);
	}

	private UnifiedDriveItemsPageResponse page() {

		return new UnifiedDriveItemsPageResponse(List.of(), 1, 25, 0, 0, true, true);
	}

}
