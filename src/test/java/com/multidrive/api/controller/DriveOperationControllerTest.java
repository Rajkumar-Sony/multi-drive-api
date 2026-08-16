package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.entity.GoogleDriveItemCategory;
import com.multidrive.api.entity.GoogleDriveItemSourceType;
import com.multidrive.api.security.AuthenticatedUserResolver;
import com.multidrive.api.service.DriveOperationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DriveOperationController.class)
class DriveOperationControllerTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DriveOperationService driveOperationService;

	@MockitoBean
	private AuthenticatedUserResolver authenticatedUserResolver;

	@BeforeEach
	void setUp() {

		when(authenticatedUserResolver.requireGoogleSubjectId(any(OidcUser.class))).thenReturn(GOOGLE_SUBJECT_ID);
	}

	@Test
	void createFolderDelegatesValidatedRequest() throws Exception {

		when(driveOperationService.createFolder(eq(GOOGLE_SUBJECT_ID), any())).thenReturn(itemResponse());

		mockMvc
			.perform(post("/api/drive/folders").with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "sourceId": 30,
						  "parentItemId": 20,
						  "name": "Project Docs"
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10));

		verify(driveOperationService).createFolder(eq(GOOGLE_SUBJECT_ID), any());
	}

	@Test
	void createFolderRejectsBlankNameBeforeService() throws Exception {

		mockMvc
			.perform(post("/api/drive/folders").with(oidcLogin())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "sourceId": 30,
						  "name": " "
						}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("name"))
			.andExpect(jsonPath("$.errors[0].message").value("name must not be blank"));

		verifyNoInteractions(driveOperationService);
	}

	@Test
	void renameDelegatesValidatedRequest() throws Exception {

		when(driveOperationService.rename(eq(GOOGLE_SUBJECT_ID), eq(10L), any())).thenReturn(itemResponse());

		mockMvc
			.perform(patch("/api/drive/items/{itemId}/rename", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Renamed Report.pdf"
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10));

		verify(driveOperationService).rename(eq(GOOGLE_SUBJECT_ID), eq(10L), any());
	}

	@Test
	void renameRejectsBlankNameBeforeService() throws Exception {

		mockMvc
			.perform(patch("/api/drive/items/{itemId}/rename", 10L).with(oidcLogin())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": ""
						}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("name"))
			.andExpect(jsonPath("$.errors[0].message").value("name must not be blank"));

		verifyNoInteractions(driveOperationService);
	}

	@Test
	void moveRejectsMissingDestinationSourceIdBeforeService() throws Exception {

		mockMvc
			.perform(post("/api/drive/items/{itemId}/move", 10L).with(oidcLogin())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "destinationParentItemId": 20
						}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("destinationSourceId"))
			.andExpect(jsonPath("$.errors[0].message").value("destinationSourceId is required"));

		verifyNoInteractions(driveOperationService);
	}

	@Test
	void copyDelegatesValidatedRequest() throws Exception {

		when(driveOperationService.copy(eq(GOOGLE_SUBJECT_ID), eq(10L), any())).thenReturn(itemResponse());

		mockMvc
			.perform(post("/api/drive/items/{itemId}/copy", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "destinationSourceId": 30,
						  "destinationParentItemId": 20,
						  "name": "Report Copy.pdf"
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10));

		verify(driveOperationService).copy(eq(GOOGLE_SUBJECT_ID), eq(10L), any());
	}

	@Test
	void copyRejectsMissingDestinationSourceIdBeforeService() throws Exception {

		mockMvc
			.perform(post("/api/drive/items/{itemId}/copy", 10L).with(oidcLogin())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Report Copy.pdf"
						}
						"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("destinationSourceId"))
			.andExpect(jsonPath("$.errors[0].message").value("destinationSourceId is required"));

		verifyNoInteractions(driveOperationService);
	}

	@Test
	void trashDelegatesToService() throws Exception {

		when(driveOperationService.trash(GOOGLE_SUBJECT_ID, 10L)).thenReturn(itemResponse());

		mockMvc
			.perform(post("/api/drive/items/{itemId}/trash", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID)))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10));

		verify(driveOperationService).trash(GOOGLE_SUBJECT_ID, 10L);
	}

	@Test
	void restoreDelegatesToService() throws Exception {

		when(driveOperationService.restore(GOOGLE_SUBJECT_ID, 10L)).thenReturn(itemResponse());

		mockMvc
			.perform(post("/api/drive/items/{itemId}/restore", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID)))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10));

		verify(driveOperationService).restore(GOOGLE_SUBJECT_ID, 10L);
	}

	@Test
	void permanentlyDeleteReturnsNoContent() throws Exception {

		mockMvc
			.perform(delete("/api/drive/items/{itemId}/permanent", 10L)
				.with(oidcLogin().idToken(token -> token.subject(GOOGLE_SUBJECT_ID)))
				.with(csrf()))
			.andExpect(status().isNoContent());

		verify(driveOperationService).permanentlyDelete(GOOGLE_SUBJECT_ID, 10L);
	}

	@Test
	void moveDelegatesValidatedRequest() throws Exception {

		when(driveOperationService.move(eq(GOOGLE_SUBJECT_ID), eq(10L), any())).thenReturn(itemResponse());

		mockMvc
			.perform(post("/api/drive/items/{itemId}/move", 10L).with(oidcLogin())
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "destinationSourceId": 30,
						  "destinationParentItemId": 20
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10));

		verify(driveOperationService).move(eq(GOOGLE_SUBJECT_ID), eq(10L), any());
	}

	private DriveItemDetailsResponse itemResponse() {

		return new DriveItemDetailsResponse(10L, 1L, "user@example.com", 30L, "My Drive",
				GoogleDriveItemSourceType.MY_DRIVE, null, "root", "google-file-id", "parent-google-id", "Report.pdf",
				"application/pdf", GoogleDriveItemCategory.OTHER, false, false, null, null, null, 1024L, null, null,
				null);
	}

}
