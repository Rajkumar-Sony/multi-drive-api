package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.dto.GoogleDriveRootResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.service.impl.GoogleDriveServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleDriveServiceImplTest {

	private GoogleTokenService googleTokenService;

	private GoogleDriveServiceImpl service;

	@BeforeEach
	void setUp() {

		googleTokenService = Mockito.mock(GoogleTokenService.class);
		service = new GoogleDriveServiceImpl(googleTokenService);
		when(googleTokenService.getValidAccessToken(20L, 42L)).thenReturn("access-token");
	}

	@Test
	void getFilesUsesDefaultPageSizeAndTrashFilter() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("pageSize=50")))
			.andExpect(requestTo(containsString("q=trashed%3Dfalse")))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess("{\"files\":[],\"nextPageToken\":\"next\"}", MediaType.APPLICATION_JSON));

		GoogleDriveFilesResponse response = service.getFiles(20L, 42L, null, null);

		assertThat(response.files()).isEmpty();
		assertThat(response.nextPageToken()).isEqualTo("next");
		server.verify();
	}

	@Test
	void getSharedDriveFilesRequiresDriveIdAndAddsDriveQueryParameters() {

		assertThatThrownBy(() -> service.getSharedDriveFiles(20L, 42L, " ", 10, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("driveId is required");

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo(containsString("corpora=drive")))
			.andExpect(requestTo(containsString("driveId=drive-a")))
			.andExpect(requestTo(containsString("includeItemsFromAllDrives=true")))
			.andRespond(withSuccess("{\"files\":[]}", MediaType.APPLICATION_JSON));

		assertThat(service.getSharedDriveFiles(20L, 42L, "drive-a", 10, null).files()).isEmpty();
		server.verify();
	}

	@Test
	void sharedDriveAndFilePageSizeBoundsAreValidated() {

		assertThatThrownBy(() -> service.getFiles(20L, 42L, 0, null)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("File pageSize must be between 1 and 1000");
		assertThatThrownBy(() -> service.getSharedDrives(20L, 42L, 101, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Shared Drive pageSize must be between 1 and 100");
	}

	@Test
	void getSharedDrivesAndMyDriveRootRejectEmptyGoogleResponses() {

		MockRestServiceServer sharedDriveServer = bindMockServer();

		sharedDriveServer.expect(requestTo(containsString("https://www.googleapis.com/drive/v3/drives")))
			.andRespond(withSuccess("", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> service.getSharedDrives(20L, 42L, 50, null)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive returned an empty Shared Drives response");

		MockRestServiceServer rootServer = bindMockServer();

		rootServer.expect(requestTo(containsString("https://www.googleapis.com/drive/v3/files/root")))
			.andRespond(withSuccess("{\"id\":\"\"}", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> service.getMyDriveRoot(20L, 42L)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Google Drive root folder could not be resolved");
	}

	private MockRestServiceServer bindMockServer() {

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		return server;
	}

}
