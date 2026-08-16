package com.multidrive.api.service;

import com.multidrive.api.dto.GoogleTokenResponse;
import com.multidrive.api.dto.GoogleUserInfoResponse;

public interface GoogleDriveOAuthService {

	String buildAuthorizationUrl(String state);

	GoogleTokenResponse exchangeAuthorizationCode(String code);

	GoogleUserInfoResponse getUserInfo(String accessToken);

}