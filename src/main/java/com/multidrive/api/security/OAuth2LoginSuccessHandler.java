package com.multidrive.api.security;

import com.multidrive.api.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private final UserService userService;

	public OAuth2LoginSuccessHandler(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unable to read Google user information");
			return;
		}

		String googleSubjectId = oidcUser.getSubject();
		String email = oidcUser.getEmail();
		String name = oidcUser.getFullName();
		String pictureUrl = oidcUser.getPicture();

		userService.saveOrUpdateGoogleUser(googleSubjectId, email, name, pictureUrl);

		response.sendRedirect("/");
	}

}