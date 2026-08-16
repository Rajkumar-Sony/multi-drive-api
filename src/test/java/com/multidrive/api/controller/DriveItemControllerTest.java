package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.service.DriveItemLookupService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriveItemController.class)
class DriveItemControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DriveItemLookupService driveItemLookupService;

	@Test
	void getItemReturnsItemDetailsForAuthenticatedUser() throws Exception {

		when(driveItemLookupService.getItem(GOOGLE_SUBJECT_ID, 10L)).thenReturn(itemResponse());

		mockMvc
			.perform(get("/api/drive/items/{itemId}", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.sourceId").value(30))
			.andExpect(jsonPath("$.name").value("Report.pdf"));

		verify(driveItemLookupService).getItem(GOOGLE_SUBJECT_ID, 10L);
	}

	private DriveItemDetailsResponse itemResponse() {

		return new DriveItemDetailsResponse(10L, 1L, "user@example.com", 30L, "My Drive",
				GoogleDriveItemSourceType.MY_DRIVE, null, "root", "google-file-id", "parent-google-id", "Report.pdf",
				"application/pdf", GoogleDriveItemCategory.OTHER, false, false, null, null, null, 1024L, null, null,
				null);
	}

}
