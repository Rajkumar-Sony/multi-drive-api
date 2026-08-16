package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveUploadRequest;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.service.DriveUploadService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriveUploadController.class)
class DriveUploadControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DriveUploadService driveUploadService;

	@Test
	void uploadBuildsRequestAndReturnsUploadedItem() throws Exception {

		MockMultipartFile file = new MockMultipartFile("file", "report.txt", "text/plain", "hello".getBytes());

		when(driveUploadService.upload(eq(GOOGLE_SUBJECT_ID), org.mockito.ArgumentMatchers.any()))
			.thenReturn(itemResponse());

		mockMvc
			.perform(multipart("/api/drive/files/upload").file(file)
				.param("sourceId", "30")
				.param("parentItemId", "20")
				.param("name", "renamed.txt")
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID)))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.name").value("Report.pdf"));

		ArgumentCaptor<DriveUploadRequest> requestCaptor = ArgumentCaptor.forClass(DriveUploadRequest.class);

		verify(driveUploadService).upload(eq(GOOGLE_SUBJECT_ID), requestCaptor.capture());

		assertThat(requestCaptor.getValue().sourceId()).isEqualTo(30L);
		assertThat(requestCaptor.getValue().parentItemId()).isEqualTo(20L);
		assertThat(requestCaptor.getValue().name()).isEqualTo("renamed.txt");
		assertThat(requestCaptor.getValue().file().getOriginalFilename()).isEqualTo("report.txt");
	}

	private DriveItemDetailsResponse itemResponse() {

		return new DriveItemDetailsResponse(10L, 1L, "user@example.com", 30L, "My Drive",
				GoogleDriveItemSourceType.MY_DRIVE, null, "root", "google-file-id", "parent-google-id", "Report.pdf",
				"application/pdf", GoogleDriveItemCategory.OTHER, false, false, null, null, null, 1024L, null, null,
				null);
	}

}
