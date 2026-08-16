package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.impl.GoogleTokenServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class GoogleTokenServiceImplTest {

	@Mock
	private GoogleDriveConnectionRepository googleDriveConnectionRepository;

	@Mock
	private TokenEncryptionService tokenEncryptionService;

	private GoogleTokenServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleTokenServiceImpl(googleDriveConnectionRepository, tokenEncryptionService, "client-id",
				"client-secret");
	}

	@Test
	void getValidAccessTokenReturnsStoredTokenWhenItIsStillOutsideExpiryBuffer() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		connection.setEncryptedAccessToken("encrypted-access");
		connection.setAccessTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(tokenEncryptionService.decrypt("encrypted-access")).thenReturn("plain-access");

		assertThat(service.getValidAccessToken(20L, 42L)).isEqualTo("plain-access");

		verify(googleDriveConnectionRepository, never()).save(connection);
	}

	@Test
	void getValidAccessTokenRefreshesExpiredTokenAndPersistsNewTokenMetadata() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		connection.setEncryptedAccessToken("old-encrypted-access");
		connection.setAccessTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
		connection.setEncryptedRefreshToken("encrypted-refresh");

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		server.expect(requestTo("https://oauth2.googleapis.com/token"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content()
				.string(allOf(containsString("client_id=client-id"), containsString("client_secret=client-secret"),
						containsString("refresh_token=plain-refresh"), containsString("grant_type=refresh_token"))))
			.andRespond(withSuccess(
					"{\"access_token\":\"new-access\",\"expires_in\":3600,\"scope\":\"drive.readonly\",\"token_type\":\"Bearer\"}",
					MediaType.APPLICATION_JSON));

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(tokenEncryptionService.decrypt("encrypted-refresh")).thenReturn("plain-refresh");
		when(tokenEncryptionService.encrypt("new-access")).thenReturn("new-encrypted-access");

		assertThat(service.getValidAccessToken(20L, 42L)).isEqualTo("new-access");

		assertThat(connection.getEncryptedAccessToken()).isEqualTo("new-encrypted-access");
		assertThat(connection.getScopes()).isEqualTo("drive.readonly");
		assertThat(connection.getStatus()).isEqualTo("CONNECTED");
		assertThat(connection.getAccessTokenExpiry()).isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(55));
		verify(googleDriveConnectionRepository).save(connection);
		server.verify();
	}

	@Test
	void getValidAccessTokenRejectsMissingRefreshTokenBeforeCallingGoogle() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		connection.setEncryptedAccessToken("old-encrypted-access");
		connection.setAccessTokenExpiry(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
		connection.setEncryptedRefreshToken("encrypted-refresh");

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.of(connection));
		when(tokenEncryptionService.decrypt("encrypted-refresh")).thenReturn(" ");

		assertThatThrownBy(() -> service.getValidAccessToken(20L, 42L)).isInstanceOf(IllegalStateException.class)
			.hasMessage("Refresh token is not available. Reconnect the Google Drive account.");

		verify(googleDriveConnectionRepository, never()).save(connection);
	}

	@Test
	void getValidAccessTokenRejectsMissingConnection() {

		when(googleDriveConnectionRepository.findByIdAndUserId(20L, 42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getValidAccessToken(20L, 42L)).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google Drive connection not found");
	}

}
