package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveContentStreamResponse;
import com.multidrive.api.dto.DriveExportFormatResponse;
import com.multidrive.api.dto.DriveExportOptionsResponse;
import com.multidrive.api.service.DriveDownloadService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriveContentController.class)
class DriveContentControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DriveDownloadService driveDownloadService;

	@Test
	void downloadStreamsPreparedContent() throws Exception {

		when(driveDownloadService.prepareDownload(GOOGLE_SUBJECT_ID, 10L)).thenReturn(new DriveContentStreamResponse(
				"Report.pdf", "application/pdf", 5L, outputStream -> outputStream.write("hello".getBytes())));

		MvcResult mvcResult = mockMvc
			.perform(get("/api/drive/items/{itemId}/download", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
			.andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "5"))
			.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("Report.pdf")))
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk()).andExpect(content().string("hello"));

		verify(driveDownloadService).prepareDownload(GOOGLE_SUBJECT_ID, 10L);
	}

	@Test
	void exportStreamsRequestedMimeType() throws Exception {

		when(driveDownloadService.prepareExport(GOOGLE_SUBJECT_ID, 10L, "application/pdf"))
			.thenReturn(new DriveContentStreamResponse("Report.pdf", "application/pdf", null,
					outputStream -> outputStream.write("export".getBytes())));

		MvcResult mvcResult = mockMvc
			.perform(get("/api/drive/items/{itemId}/export", 10L).param("mimeType", "application/pdf")
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
			.andReturn();

		mockMvc.perform(asyncDispatch(mvcResult)).andExpect(status().isOk()).andExpect(content().string("export"));

		verify(driveDownloadService).prepareExport(GOOGLE_SUBJECT_ID, 10L, "application/pdf");
	}

	@Test
	void exportFormatsReturnsAvailableFormats() throws Exception {

		DriveExportOptionsResponse response = new DriveExportOptionsResponse(10L,
				"application/vnd.google-apps.document",
				List.of(new DriveExportFormatResponse("PDF", "application/pdf", "pdf", true)));

		when(driveDownloadService.getExportOptions(GOOGLE_SUBJECT_ID, 10L)).thenReturn(response);

		mockMvc
			.perform(get("/api/drive/items/{itemId}/export-formats", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.itemId").value(10))
			.andExpect(jsonPath("$.formats[0].mimeType").value("application/pdf"));

		verify(driveDownloadService).getExportOptions(GOOGLE_SUBJECT_ID, 10L);
	}

}
