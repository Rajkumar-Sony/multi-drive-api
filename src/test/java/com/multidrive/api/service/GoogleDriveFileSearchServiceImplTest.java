package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFileResponse;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.service.impl.GoogleDriveFileSearchServiceImpl;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleDriveFileSearchServiceImplTest {

	private GoogleTokenService googleTokenService;

	private GoogleDriveFileSearchServiceImpl service;

	@BeforeEach
	void setUp() {

		googleTokenService = Mockito.mock(GoogleTokenService.class);
		service = new GoogleDriveFileSearchServiceImpl(googleTokenService);
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
	}

	@Test
	void findByAppPropertySearchesUserCorpusAndReturnsMatches() {

		MockRestServiceServer server = bindMockServer();

		server
			.expect(requestTo(allOf(containsString("/drive/v3/files?"), containsString("appProperties"),
					containsString("multiDriveOp"), containsString("marker-1"), containsString("corpora=user"),
					containsString("includeItemsFromAllDrives=true"))))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(
					withSuccess("{\"files\":[{\"id\":\"copy-1\",\"appProperties\":{\"multiDriveOp\":\"marker-1\"}}]}",
							MediaType.APPLICATION_JSON));

		List<GoogleDriveFileResponse> matches = service.findByAppProperty(20L, 42L, GoogleDriveSourceType.MY_DRIVE,
				null, "multiDriveOp", "marker-1");

		assertThat(matches).hasSize(1);
		assertThat(matches.getFirst().id()).isEqualTo("copy-1");
		assertThat(matches.getFirst().appProperties()).containsEntry("multiDriveOp", "marker-1");
		server.verify();
	}

	@Test
	void findByAppPropertySearchesSharedDriveCorpus() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(allOf(containsString("corpora=drive"), containsString("driveId=shared-drive-1"))))
			.andRespond(withSuccess("{\"files\":[]}", MediaType.APPLICATION_JSON));

		assertThat(service.findByAppProperty(20L, 42L, GoogleDriveSourceType.SHARED_DRIVE, "shared-drive-1",
				"multiDriveOp", "marker-1"))
			.isEmpty();
		server.verify();
	}

	@Test
	void findByAppPropertyRejectsIncompleteSearch() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("/drive/v3/files?")))
			.andRespond(withSuccess("{\"files\":[],\"incompleteSearch\":true}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> service.findByAppProperty(20L, 42L, GoogleDriveSourceType.MY_DRIVE, null,
				"multiDriveOp", "marker-1"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive operation-marker search returned incomplete results");
		server.verify();
	}

	private MockRestServiceServer bindMockServer() {

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		return server;
	}

}
