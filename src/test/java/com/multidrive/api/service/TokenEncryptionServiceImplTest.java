package com.multidrive.api.service;

import com.multidrive.api.service.impl.TokenEncryptionServiceImpl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceImplTest {

	private static final String ENCRYPTION_KEY = Base64.getEncoder()
		.encodeToString("12345678901234567890123456789012".getBytes(StandardCharsets.UTF_8));

	private final TokenEncryptionServiceImpl service = new TokenEncryptionServiceImpl(ENCRYPTION_KEY);

	@Test
	void encryptsAndDecryptsTokenWithoutReturningPlainText() {

		String firstCipherText = service.encrypt("refresh-token");
		String secondCipherText = service.encrypt("refresh-token");

		assertThat(firstCipherText).isNotEqualTo("refresh-token");
		assertThat(secondCipherText).isNotEqualTo("refresh-token");
		assertThat(secondCipherText).isNotEqualTo(firstCipherText);
		assertThat(service.decrypt(firstCipherText)).isEqualTo("refresh-token");
		assertThat(service.decrypt(secondCipherText)).isEqualTo("refresh-token");
	}

	@Test
	void treatsBlankInputAsAbsentToken() {

		assertThat(service.encrypt(null)).isNull();
		assertThat(service.encrypt(" ")).isNull();
		assertThat(service.decrypt(null)).isNull();
		assertThat(service.decrypt(" ")).isNull();
	}

	@Test
	void rejectsInvalidKeyAndCipherText() {

		assertThatThrownBy(() -> new TokenEncryptionServiceImpl(Base64.getEncoder().encodeToString("short".getBytes())))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("TOKEN_ENCRYPTION_KEY must be a 256-bit Base64 encoded key");

		assertThatThrownBy(() -> service.decrypt("not-base64")).isInstanceOf(IllegalStateException.class)
			.hasMessage("Unable to decrypt token");
	}

}
