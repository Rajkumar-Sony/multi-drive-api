package com.multidrive.api.service;

import com.multidrive.api.entity.User;
import com.multidrive.api.repository.UserRepository;
import com.multidrive.api.service.impl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	private UserServiceImpl service;

	@BeforeEach
	void setUp() {

		service = new UserServiceImpl(userRepository);
	}

	@Test
	void saveOrUpdateGoogleUserCreatesNewUserWhenSubjectIsUnknown() {

		when(userRepository.findByGoogleSubjectId("google-subject-123")).thenReturn(Optional.empty());
		when(userRepository.save(org.mockito.ArgumentMatchers.any()))
			.thenAnswer(invocation -> invocation.getArgument(0));

		User savedUser = service.saveOrUpdateGoogleUser("google-subject-123", "user@example.com", "Test User",
				"https://example.com/avatar.png");

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		verify(userRepository).save(userCaptor.capture());

		assertThat(savedUser).isSameAs(userCaptor.getValue());
		assertThat(userCaptor.getValue().getGoogleSubjectId()).isEqualTo("google-subject-123");
		assertThat(userCaptor.getValue().getEmail()).isEqualTo("user@example.com");
		assertThat(userCaptor.getValue().getName()).isEqualTo("Test User");
		assertThat(userCaptor.getValue().getPictureUrl()).isEqualTo("https://example.com/avatar.png");
	}

	@Test
	void saveOrUpdateGoogleUserUpdatesExistingUser() {

		User existingUser = new User();

		existingUser.setGoogleSubjectId("google-subject-123");
		existingUser.setEmail("old@example.com");

		when(userRepository.findByGoogleSubjectId("google-subject-123")).thenReturn(Optional.of(existingUser));
		when(userRepository.save(existingUser)).thenReturn(existingUser);

		User savedUser = service.saveOrUpdateGoogleUser("google-subject-123", "new@example.com", "New Name", null);

		assertThat(savedUser).isSameAs(existingUser);
		assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
		assertThat(savedUser.getName()).isEqualTo("New Name");
		assertThat(savedUser.getPictureUrl()).isNull();
	}

	@Test
	void findByGoogleSubjectIdReturnsExistingUserOrThrows() {

		User existingUser = new User();

		when(userRepository.findByGoogleSubjectId("google-subject-123")).thenReturn(Optional.of(existingUser));
		when(userRepository.findByGoogleSubjectId("missing-subject")).thenReturn(Optional.empty());

		assertThat(service.findByGoogleSubjectId("google-subject-123")).isSameAs(existingUser);

		assertThatThrownBy(() -> service.findByGoogleSubjectId("missing-subject"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("User not found");
	}

}
