package com.multidrive.api.security;

import com.multidrive.api.entity.User;
import com.multidrive.api.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AuthenticatedUserResolver {

	private static final String AUTHENTICATED_USER_NOT_FOUND = "Authenticated application user not found";

	private final UserService userService;

	public AuthenticatedUserResolver(UserService userService) {

		this.userService = userService;
	}

	public String requireGoogleSubjectId(OidcUser oidcUser) {

		if (oidcUser == null || oidcUser.getSubject() == null || oidcUser.getSubject().isBlank()) {

			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AUTHENTICATED_USER_NOT_FOUND);
		}

		return oidcUser.getSubject();
	}

	public User requireApplicationUser(OidcUser oidcUser) {

		String googleSubjectId = requireGoogleSubjectId(oidcUser);

		try {
			return userService.findByGoogleSubjectId(googleSubjectId);
		}
		catch (IllegalStateException exception) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, AUTHENTICATED_USER_NOT_FOUND);
		}
	}

}
