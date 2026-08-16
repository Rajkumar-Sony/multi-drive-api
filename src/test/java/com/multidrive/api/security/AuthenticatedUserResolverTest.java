package com.multidrive.api.security;

import com.multidrive.api.entity.User;
import com.multidrive.api.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticatedUserResolverTest {

	private static final String GOOGLE_SUBJECT_ID = "google-subject-123";

	private final UserService userService = mock(UserService.class);

	private final AuthenticatedUserResolver resolver = new AuthenticatedUserResolver(userService);

	@Test
	void requireGoogleSubjectIdReturnsAuthenticatedSubject() {

		assertThat(resolver.requireGoogleSubjectId(oidcUser(GOOGLE_SUBJECT_ID))).isEqualTo(GOOGLE_SUBJECT_ID);
	}

	@Test
	void requireGoogleSubjectIdRejectsMissingSubject() {

		assertThatThrownBy(() -> resolver.requireGoogleSubjectId(oidcUser(" ")))
			.isInstanceOf(ResponseStatusException.class)
			.extracting("statusCode")
			.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void requireApplicationUserReturnsLocalUser() {

		User user = new User();

		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID)).thenReturn(user);

		assertThat(resolver.requireApplicationUser(oidcUser(GOOGLE_SUBJECT_ID))).isSameAs(user);
	}

	@Test
	void requireApplicationUserRejectsMissingLocalUser() {

		when(userService.findByGoogleSubjectId(GOOGLE_SUBJECT_ID))
			.thenThrow(new IllegalStateException("User not found"));

		assertThatThrownBy(() -> resolver.requireApplicationUser(oidcUser(GOOGLE_SUBJECT_ID)))
			.isInstanceOf(ResponseStatusException.class)
			.extracting("statusCode")
			.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	private OidcUser oidcUser(String subject) {

		OidcUser oidcUser = mock(OidcUser.class);

		when(oidcUser.getSubject()).thenReturn(subject);

		return oidcUser;
	}

}
