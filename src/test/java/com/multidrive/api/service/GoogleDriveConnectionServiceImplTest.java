package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleDriveAccountResponse;
import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.User;
import com.multidrive.api.repository.GoogleDriveConnectionRepository;
import com.multidrive.api.service.impl.GoogleDriveConnectionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleDriveConnectionServiceImplTest {

	@Mock
	private GoogleDriveConnectionRepository googleDriveConnectionRepository;

	@Mock
	private TokenEncryptionService tokenEncryptionService;

	private GoogleDriveConnectionServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveConnectionServiceImpl(googleDriveConnectionRepository, tokenEncryptionService);
	}

	@Test
	void saveOrUpdateConnectionCreatesNewConnectionAndEncryptsBothTokens() {

		User user = user();
		GoogleUserInfoResponse googleUserInfo = new GoogleUserInfoResponse("google-subject-1", "user@example.com",
				"User", null);
		GoogleTokenResponse tokenResponse = new GoogleTokenResponse("access-token", "refresh-token", 3600L,
				"drive.readonly", "Bearer", null);

		when(googleDriveConnectionRepository.findByUserIdAndGoogleSubjectId(42L, "google-subject-1"))
			.thenReturn(Optional.empty());
		when(tokenEncryptionService.encrypt("access-token")).thenReturn("encrypted-access");
		when(tokenEncryptionService.encrypt("refresh-token")).thenReturn("encrypted-refresh");
		when(googleDriveConnectionRepository.save(org.mockito.ArgumentMatchers.any(GoogleDriveConnection.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		GoogleDriveConnection connection = service.saveOrUpdateConnection(user, googleUserInfo, tokenResponse);

		assertThat(connection.getUser()).isSameAs(user);
		assertThat(connection.getGoogleSubjectId()).isEqualTo("google-subject-1");
		assertThat(connection.getGoogleEmail()).isEqualTo("user@example.com");
		assertThat(connection.getEncryptedAccessToken()).isEqualTo("encrypted-access");
		assertThat(connection.getEncryptedRefreshToken()).isEqualTo("encrypted-refresh");
		assertThat(connection.getScopes()).isEqualTo("drive.readonly");
		assertThat(connection.getStatus()).isEqualTo("CONNECTED");
		assertThat(connection.getAccessTokenExpiry()).isAfter(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(55));
	}

	@Test
	void saveOrUpdateConnectionKeepsStoredRefreshTokenWhenGoogleOmitsReplacementForExistingConnection() {

		User user = user();
		GoogleDriveConnection existingConnection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(existingConnection, "id", 20L);
		existingConnection.setEncryptedRefreshToken("stored-refresh");

		GoogleUserInfoResponse googleUserInfo = new GoogleUserInfoResponse("google-subject-1", "new@example.com",
				"User", null);
		GoogleTokenResponse tokenResponse = new GoogleTokenResponse("new-access-token", null, null, "drive.file",
				"Bearer", null);

		when(googleDriveConnectionRepository.findByUserIdAndGoogleSubjectId(42L, "google-subject-1"))
			.thenReturn(Optional.of(existingConnection));
		when(tokenEncryptionService.encrypt("new-access-token")).thenReturn("new-encrypted-access");
		when(googleDriveConnectionRepository.save(existingConnection)).thenReturn(existingConnection);

		GoogleDriveConnection connection = service.saveOrUpdateConnection(user, googleUserInfo, tokenResponse);

		assertThat(connection.getEncryptedAccessToken()).isEqualTo("new-encrypted-access");
		assertThat(connection.getEncryptedRefreshToken()).isEqualTo("stored-refresh");
		assertThat(connection.getGoogleEmail()).isEqualTo("new@example.com");
	}

	@Test
	void saveOrUpdateConnectionRejectsNewConnectionWithoutRefreshToken() {

		User user = user();
		GoogleUserInfoResponse googleUserInfo = new GoogleUserInfoResponse("google-subject-1", "user@example.com",
				"User", null);
		GoogleTokenResponse tokenResponse = new GoogleTokenResponse("access-token", " ", 3600L, "drive.readonly",
				"Bearer", null);

		when(googleDriveConnectionRepository.findByUserIdAndGoogleSubjectId(42L, "google-subject-1"))
			.thenReturn(Optional.empty());
		when(tokenEncryptionService.encrypt("access-token")).thenReturn("encrypted-access");

		assertThatThrownBy(() -> service.saveOrUpdateConnection(user, googleUserInfo, tokenResponse))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("Google did not return a refresh token for the new Drive connection");
	}

	@Test
	void getConnectedAccountsMapsOnlyPublicAccountFields() {

		GoogleDriveConnection connection = new GoogleDriveConnection();
		ReflectionTestUtils.setField(connection, "id", 20L);
		LocalDateTime expiry = LocalDateTime.of(2026, 8, 16, 12, 0);
		connection.setGoogleEmail("user@example.com");
		connection.setStatus("CONNECTED");
		connection.setAccessTokenExpiry(expiry);
		connection.setEncryptedAccessToken("secret-access");
		connection.setEncryptedRefreshToken("secret-refresh");

		when(googleDriveConnectionRepository.findAllByUserId(42L)).thenReturn(List.of(connection));

		List<GoogleDriveAccountResponse> accounts = service.getConnectedAccounts(42L);

		assertThat(accounts)
			.containsExactly(new GoogleDriveAccountResponse(20L, "user@example.com", "CONNECTED", expiry));
		verify(googleDriveConnectionRepository).findAllByUserId(42L);
	}

	private User user() {

		User user = new User();
		ReflectionTestUtils.setField(user, "id", 42L);
		user.setGoogleSubjectId("google-subject-user");
		user.setEmail("user@example.com");

		return user;
	}

}
