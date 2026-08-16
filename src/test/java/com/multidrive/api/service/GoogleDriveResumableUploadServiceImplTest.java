package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.service.impl.GoogleDriveResumableUploadServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class GoogleDriveResumableUploadServiceImplTest {

	private GoogleTokenService googleTokenService;

	private GoogleDriveResumableUploadServiceImpl service;

	@BeforeEach
	void setUp() {

		googleTokenService = Mockito.mock(GoogleTokenService.class);
		service = new GoogleDriveResumableUploadServiceImpl(googleTokenService,
				new tools.jackson.databind.ObjectMapper());
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
	}

	@Test
	void uploadEmptyFileDefaultsInvalidContentTypeAndUsesSessionLocation() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("uploadType=resumable")))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(header("X-Upload-Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE))
			.andExpect(header("X-Upload-Content-Length", "0"))
			.andExpect(content().string(containsString("\"parents\":[\"parent-1\"]")))
			.andRespond(request -> {
				MockClientHttpResponse response = new MockClientHttpResponse(new byte[0], HttpStatus.OK);
				response.getHeaders().setLocation(URI.create("https://upload.example.com/session-1"));
				return response;
			});
		server.expect(requestTo("https://upload.example.com/session-1"))
			.andExpect(method(HttpMethod.PUT))
			.andExpect(header(HttpHeaders.CONTENT_LENGTH, "0"))
			.andRespond(request -> {
				String json = "{\"id\":\"file-1\",\"name\":\"Empty.txt\"}";
				MockClientHttpResponse response = new MockClientHttpResponse(json.getBytes(StandardCharsets.UTF_8),
						HttpStatus.OK);
				response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
				return response;
			});

		GoogleDriveFileResponse response = service.upload(20L, 42L, "parent-1", "Empty.txt", "bad content type", 0,
				() -> new ByteArrayInputStream(new byte[0]));

		assertThat(response.id()).isEqualTo("file-1");
		assertThat(response.name()).isEqualTo("Empty.txt");
		server.verify();
	}

	@Test
	void uploadRejectsInvalidRequestBeforeFetchingToken() {

		assertThatThrownBy(() -> service.upload(null, 42L, "parent-1", "file.txt", "text/plain", 1,
				() -> new ByteArrayInputStream(new byte[] { 1 })))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("connectionId is required");
		assertThatThrownBy(() -> service.upload(20L, 42L, " ", "file.txt", "text/plain", 1,
				() -> new ByteArrayInputStream(new byte[] { 1 })))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("parentGoogleFileId is required");
		assertThatThrownBy(() -> service.upload(20L, 42L, "parent-1", " ", "text/plain", 1,
				() -> new ByteArrayInputStream(new byte[] { 1 })))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("fileName is required");
		assertThatThrownBy(() -> service.upload(20L, 42L, "parent-1", "file.txt", "text/plain", -1,
				() -> new ByteArrayInputStream(new byte[] { 1 })))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("contentLength must be 0 or greater");
		assertThatThrownBy(() -> service.upload(20L, 42L, "parent-1", "file.txt", "text/plain", 1, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("inputStreamProvider is required");

		verify(googleTokenService, never()).getValidAccessToken(org.mockito.ArgumentMatchers.anyLong(),
				org.mockito.ArgumentMatchers.anyLong());
	}

	private MockRestServiceServer bindMockServer() {

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		return server;
	}

}
