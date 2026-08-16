package com.multidrive.api.controller;

import com.multidrive.api.dto.GoogleDriveAccountResponse;
import com.multidrive.api.dto.GoogleDriveFilesResponse;
import com.multidrive.api.dto.GoogleSharedDrivesResponse;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.GoogleDriveConnectionService;
import com.multidrive.api.service.GoogleDriveOAuthService;
import com.multidrive.api.service.GoogleDriveService;
import com.multidrive.api.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/google/accounts")
public class GoogleAccountController {

	private static final String OAUTH_STATE_SESSION_KEY = "GOOGLE_DRIVE_OAUTH_STATE";

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final GoogleDriveOAuthService googleDriveOAuthService;

	private final GoogleDriveConnectionService googleDriveConnectionService;

	private final GoogleDriveService googleDriveService;

	private final UserService userService;

	public GoogleAccountController(GoogleDriveOAuthService googleDriveOAuthService,
			GoogleDriveConnectionService googleDriveConnectionService, GoogleDriveService googleDriveService,
			UserService userService) {
		this.googleDriveOAuthService = googleDriveOAuthService;

		this.googleDriveConnectionService = googleDriveConnectionService;

		this.googleDriveService = googleDriveService;

		this.userService = userService;
	}

	@GetMapping("/connect")
	public void connectGoogleDrive(HttpSession session, HttpServletResponse response) throws IOException {

		String state = generateState();

		session.setAttribute(OAUTH_STATE_SESSION_KEY, state);

		String authorizationUrl = googleDriveOAuthService.buildAuthorizationUrl(state);

		response.sendRedirect(authorizationUrl);
	}

	@GetMapping
	public List<GoogleDriveAccountResponse> getConnectedAccounts(@AuthenticationPrincipal OidcUser oidcUser) {

		User user = getApplicationUser(oidcUser);

		return googleDriveConnectionService.getConnectedAccounts(user.getId());
	}

	@GetMapping("/{connectionId}/files")
	public GoogleDriveFilesResponse getFiles(@PathVariable Long connectionId,

			@RequestParam(defaultValue = "50") Integer pageSize,

			@RequestParam(required = false) String pageToken,

			@AuthenticationPrincipal OidcUser oidcUser) {

		User user = getApplicationUser(oidcUser);

		return googleDriveService.getFiles(connectionId, user.getId(), pageSize, pageToken);
	}

	@GetMapping("/{connectionId}/drives")
	public GoogleSharedDrivesResponse getSharedDrives(@PathVariable Long connectionId,

			@RequestParam(defaultValue = "50") Integer pageSize,

			@RequestParam(required = false) String pageToken,

			@AuthenticationPrincipal OidcUser oidcUser) {

		User user = getApplicationUser(oidcUser);

		return googleDriveService.getSharedDrives(connectionId, user.getId(), pageSize, pageToken);
	}

	private User getApplicationUser(OidcUser oidcUser) {

		if (oidcUser == null) {
			throw new IllegalStateException("Authenticated application user not found");
		}

		return userService.findByGoogleSubjectId(oidcUser.getSubject());
	}

	private String generateState() {

		byte[] randomBytes = new byte[32];

		SECURE_RANDOM.nextBytes(randomBytes);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

}