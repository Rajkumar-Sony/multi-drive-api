package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;
import com.multidrive.api.service.GoogleDriveOAuthService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleDriveOAuthServiceImpl implements GoogleDriveOAuthService {

	private static final String GOOGLE_AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";

	private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

	private static final String GOOGLE_USER_INFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

	private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";

	private final String clientId;

	private final String clientSecret;

	private final String redirectUri;

	private final RestClient restClient;

	public GoogleDriveOAuthServiceImpl(
			@Value("${spring.security.oauth2.client.registration.google.client-id}") String clientId,

			@Value("${spring.security.oauth2.client.registration.google.client-secret}") String clientSecret,

			@Value("${google.oauth2.drive-redirect-uri}") String redirectUri) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;

		this.restClient = RestClient.create();
	}

	@Override
	public String buildAuthorizationUrl(String state) {

		String scope = String.join(" ", "openid", "email", "profile", DRIVE_SCOPE);

		return UriComponentsBuilder.fromUriString(GOOGLE_AUTHORIZATION_URL)
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("response_type", "code")
			.queryParam("scope", scope)
			.queryParam("access_type", "offline")
			.queryParam("prompt", "consent select_account")
			.queryParam("include_granted_scopes", "true")
			.queryParam("state", state)
			.build()
			.encode()
			.toUriString();
	}

	@Override
	public GoogleTokenResponse exchangeAuthorizationCode(String code) {

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

		formData.add("code", code);
		formData.add("client_id", clientId);
		formData.add("client_secret", clientSecret);
		formData.add("redirect_uri", redirectUri);
		formData.add("grant_type", "authorization_code");

		GoogleTokenResponse tokenResponse = restClient.post()
			.uri(GOOGLE_TOKEN_URL)
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(formData)
			.retrieve()
			.body(GoogleTokenResponse.class);

		if (tokenResponse == null) {
			throw new IllegalStateException("Google returned an empty token response");
		}

		return tokenResponse;
	}

	@Override
	public GoogleUserInfoResponse getUserInfo(String accessToken) {

		if (accessToken == null || accessToken.isBlank()) {
			throw new IllegalArgumentException("Google access token must not be empty");
		}

		GoogleUserInfoResponse userInfo = restClient.get()
			.uri(GOOGLE_USER_INFO_URL)
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.retrieve()
			.body(GoogleUserInfoResponse.class);

		if (userInfo == null) {
			throw new IllegalStateException("Google returned an empty user info response");
		}

		return userInfo;
	}

}