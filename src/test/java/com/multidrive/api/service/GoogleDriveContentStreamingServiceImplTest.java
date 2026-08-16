package com.multidrive.api.service;

import com.multidrive.api.service.impl.GoogleDriveContentStreamingServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleDriveContentStreamingServiceImplTest {

	private GoogleTokenService googleTokenService;

	private GoogleDriveContentStreamingServiceImpl service;

	@BeforeEach
	void setUp() {

		googleTokenService = Mockito.mock(GoogleTokenService.class);
		service = new GoogleDriveContentStreamingServiceImpl(googleTokenService);
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
	}

	@Test
	void streamBlobWritesGoogleResponseToOutputStream() throws Exception {

		MockRestServiceServer server = bindMockServer();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		server.expect(requestTo(containsString("/drive/v3/files/file-1?")))
			.andExpect(requestTo(containsString("alt=media")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andExpect(header(HttpHeaders.ACCEPT, "*/*"))
			.andRespond(withSuccess("hello", MediaType.APPLICATION_OCTET_STREAM));

		service.streamBlob(20L, 42L, "file-1", outputStream);

		assertThat(outputStream.toString(StandardCharsets.UTF_8)).isEqualTo("hello");
		server.verify();
	}

	@Test
	void streamExportRequiresMimeTypeAndFailsOnNonSuccessResponse() {

		assertThatThrownBy(() -> service.streamExport(20L, 42L, "file-1", " ", new ByteArrayOutputStream()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("exportMimeType is required");

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files/file-1/export")))
			.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(
				() -> service.streamExport(20L, 42L, "file-1", "application/pdf", new ByteArrayOutputStream()))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive file export failed with HTTP status 404");
		server.verify();
	}

	@Test
	void streamBlobValidatesRequiredArguments() {

		assertThatThrownBy(() -> service.streamBlob(null, 42L, "file-1", new ByteArrayOutputStream()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("connectionId is required");
		assertThatThrownBy(() -> service.streamBlob(20L, 42L, " ", new ByteArrayOutputStream()))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("googleFileId is required");
		assertThatThrownBy(() -> service.streamBlob(20L, 42L, "file-1", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("outputStream is required");
	}

	private MockRestServiceServer bindMockServer() {

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		return server;
	}

}
