package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.service.impl.GoogleDriveFileMutationServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleDriveFileMutationServiceImplTest {

	private GoogleTokenService googleTokenService;

	private GoogleDriveFileMutationServiceImpl service;

	@BeforeEach
	void setUp() {

		googleTokenService = Mockito.mock(GoogleTokenService.class);
		service = new GoogleDriveFileMutationServiceImpl(googleTokenService);
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
	}

	@Test
	void renameTrimsNameAndReturnsRequiredFileMetadata() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files/file-1?")))
			.andExpect(method(HttpMethod.PATCH))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(content().string(containsString("\"name\":\"Renamed\"")))
			.andRespond(withSuccess("{\"id\":\"file-1\",\"name\":\"Renamed\"}", MediaType.APPLICATION_JSON));

		GoogleDriveFileResponse response = service.rename(20L, 42L, "file-1", " Renamed ");

		assertThat(response.id()).isEqualTo("file-1");
		assertThat(response.name()).isEqualTo("Renamed");
		server.verify();
	}

	@Test
	void moveBuildsAddAndRemoveParentsWithoutDuplicateDestination() {

		MockRestServiceServer server = bindMockServer();

		server
			.expect(requestTo(allOf(containsString("addParents=dest-parent"),
					containsString("removeParents=parent-a,parent-b"), not(containsString("dest-parent,")))))
			.andExpect(method(HttpMethod.PATCH))
			.andRespond(withSuccess("{\"id\":\"file-1\"}", MediaType.APPLICATION_JSON));

		GoogleDriveFileResponse response = service.move(20L, 42L, "file-1", "dest-parent",
				List.of("parent-a", "dest-parent", " ", "parent-a", "parent-b"));

		assertThat(response.id()).isEqualTo("file-1");
		server.verify();
	}

	@Test
	void copyTreatsBlankOptionalNameAsOmittedRename() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files/file-1/copy?")))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content()
				.string(allOf(containsString("\"parents\":[\"dest-parent\"]"), not(containsString("\"name\"")))))
			.andRespond(withSuccess("{\"id\":\"copy-1\"}", MediaType.APPLICATION_JSON));

		assertThat(service.copy(20L, 42L, "file-1", "dest-parent", " ").id()).isEqualTo("copy-1");
		server.verify();
	}

	@Test
	void copyWithAppPropertiesSendsPrivateMarkerMetadata() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files/file-1/copy?")))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content()
				.string(allOf(containsString("\"parents\":[\"dest-parent\"]"), containsString("\"name\":\"Copy\""),
						containsString("\"appProperties\":{\"multiDriveOp\":\"marker-1\"}"))))
			.andRespond(withSuccess("{\"id\":\"copy-1\",\"appProperties\":{\"multiDriveOp\":\"marker-1\"}}",
					MediaType.APPLICATION_JSON));

		GoogleDriveFileResponse response = service.copyWithAppProperties(20L, 42L, "file-1", "dest-parent", " Copy ",
				Map.of("multiDriveOp", "marker-1"));

		assertThat(response.id()).isEqualTo("copy-1");
		assertThat(response.appProperties()).containsEntry("multiDriveOp", "marker-1");
		server.verify();
	}

	@Test
	void createFolderWithAppPropertiesSendsPrivateMarkerMetadata() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files?")))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().string(allOf(containsString("\"name\":\"Folder\""),
					containsString("\"mimeType\":\"application/vnd.google-apps.folder\""),
					containsString("\"parents\":[\"dest-parent\"]"),
					containsString("\"appProperties\":{\"multiDriveOp\":\"marker-1\"}"))))
			.andRespond(withSuccess("{\"id\":\"folder-1\",\"appProperties\":{\"multiDriveOp\":\"marker-1\"}}",
					MediaType.APPLICATION_JSON));

		GoogleDriveFileResponse response = service.createFolderWithAppProperties(20L, 42L, "dest-parent", " Folder ",
				Map.of("multiDriveOp", "marker-1"));

		assertThat(response.id()).isEqualTo("folder-1");
		assertThat(response.appProperties()).containsEntry("multiDriveOp", "marker-1");
		server.verify();
	}

	@Test
	void mutationsValidateRequiredArgumentsAndEmptyResponses() {

		assertThatThrownBy(() -> service.rename(20L, 42L, "file-1", " ")).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("name must not be blank");
		assertThatThrownBy(() -> service.copy(20L, 42L, "file-1", " ", "copy"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("destinationParentGoogleFileId is required");

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files/file-1?")))
			.andRespond(withSuccess("{\"id\":\"\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> service.trash(20L, 42L, "file-1")).isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive returned an empty trash response");
		server.verify();
	}

	@Test
	void permanentlyDeleteSendsDeleteRequest() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files/file-1?")))
			.andExpect(method(HttpMethod.DELETE))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess());

		service.permanentlyDelete(20L, 42L, "file-1");

		server.verify();
	}

	private MockRestServiceServer bindMockServer() {

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		return server;
	}

}
