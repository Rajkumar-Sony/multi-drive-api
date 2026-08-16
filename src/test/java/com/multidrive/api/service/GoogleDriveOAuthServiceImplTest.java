package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;
import com.multidrive.api.service.impl.GoogleDriveOAuthServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleDriveOAuthServiceImplTest {

	private GoogleDriveOAuthServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new GoogleDriveOAuthServiceImpl("client-id", "client-secret", "https://app.example.com/callback");
	}

	@Test
	void buildAuthorizationUrlIncludesOfflineDriveConsentParameters() {

		String authorizationUrl = service.buildAuthorizationUrl("state-123");

		assertThat(authorizationUrl).startsWith("https://accounts.google.com/o/oauth2/v2/auth?");
		assertThat(authorizationUrl).contains("client_id=client-id", "redirect_uri=https://app.example.com/callback",
				"response_type=code", "access_type=offline", "include_granted_scopes=true", "state=state-123");
		assertThat(authorizationUrl).contains("openid", "email", "profile", "https://www.googleapis.com/auth/drive");
		assertThat(authorizationUrl).contains("prompt=consent%20select_account");
	}

	@Test
	void exchangeAuthorizationCodePostsFormAndReturnsTokenResponse() {

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo("https://oauth2.googleapis.com/token"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
			.andExpect(content().string(allOf(containsString("code=auth-code"), containsString("client_id=client-id"),
					containsString("client_secret=client-secret"),
					containsString("redirect_uri=https%3A%2F%2Fapp.example.com%2Fcallback"),
					containsString("grant_type=authorization_code"))))
			.andRespond(withSuccess(
					"{\"access_token\":\"access-token\",\"refresh_token\":\"refresh-token\",\"expires_in\":3600,\"scope\":\"drive\",\"token_type\":\"Bearer\"}",
					MediaType.APPLICATION_JSON));

		GoogleTokenResponse response = service.exchangeAuthorizationCode("auth-code");

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isEqualTo("refresh-token");
		server.verify();
	}

	@Test
	void getUserInfoValidatesAccessTokenAndSendsBearerToken() {

		assertThatThrownBy(() -> service.getUserInfo(" ")).isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Google access token must not be empty");

		MockRestServiceServer server = bindMockServer();

		server.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
			.andRespond(withSuccess(
					"{\"sub\":\"google-subject-1\",\"email\":\"user@example.com\",\"name\":\"User\",\"picture\":\"avatar\"}",
					MediaType.APPLICATION_JSON));

		GoogleUserInfoResponse response = service.getUserInfo("access-token");

		assertThat(response.sub()).isEqualTo("google-subject-1");
		assertThat(response.email()).isEqualTo("user@example.com");
		server.verify();
	}

	private MockRestServiceServer bindMockServer() {

		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ReflectionTestUtils.setField(service, "restClient", builder.build());

		return server;
	}

}
