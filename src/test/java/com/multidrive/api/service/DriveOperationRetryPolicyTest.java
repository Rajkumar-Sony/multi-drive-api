package com.multidrive.api.service;

import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.exception.DriveOperationCleanupRequiredException;
import com.multidrive.api.exception.DriveOperationNotAllowedException;
import com.multidrive.api.exception.DriveOperationReconciliationPendingException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class DriveOperationRetryPolicyTest {

	private final DriveOperationRetryPolicy retryPolicy = new DriveOperationRetryPolicy();

	@Test
	void shouldRetryTransientFailures() {

		assertThat(retryPolicy.shouldRetry(new ResourceAccessException("timeout"))).isTrue();

		assertThat(retryPolicy.shouldRetry(httpException(HttpStatus.TOO_MANY_REQUESTS))).isTrue();

		assertThat(retryPolicy.shouldRetry(httpException(HttpStatus.INTERNAL_SERVER_ERROR))).isTrue();

		assertThat(retryPolicy.shouldRetry(new DriveOperationReconciliationPendingException("pending"))).isTrue();
	}

	@Test
	void shouldNotRetryPermanentFailures() {

		assertThat(retryPolicy.shouldRetry(new IllegalArgumentException("bad job"))).isFalse();

		assertThat(retryPolicy.shouldRetry(new DriveItemNotFoundException(10L))).isFalse();

		assertThat(retryPolicy.shouldRetry(new DriveOperationNotAllowedException("MOVE"))).isFalse();

		assertThat(retryPolicy
			.shouldRetry(new DriveOperationCleanupRequiredException("DUPLICATE_OPERATION_MARKER", "duplicates")))
			.isFalse();
	}

	@Test
	void errorCodeUsesCopySpecificExceptionCodes() {

		assertThat(retryPolicy.errorCode(new DriveOperationCleanupRequiredException("COPY_MARKER_MISMATCH", "bad")))
			.isEqualTo("COPY_MARKER_MISMATCH");

		assertThat(retryPolicy.errorCode(new DriveOperationReconciliationPendingException("pending")))
			.isEqualTo("COPY_RECONCILIATION_PENDING");
	}

	@Test
	void retryDelayUsesBoundedExponentialBackoff() {

		assertThat(retryPolicy.retryDelay(1)).isEqualTo(Duration.ofSeconds(5));

		assertThat(retryPolicy.retryDelay(4)).isEqualTo(Duration.ofSeconds(40));

		assertThat(retryPolicy.retryDelay(20)).isEqualTo(Duration.ofMinutes(5));
	}

	private RestClientResponseException httpException(HttpStatus status) {

		return new RestClientResponseException(status.getReasonPhrase(), status.value(), status.getReasonPhrase(), null,
				new byte[0], StandardCharsets.UTF_8);
	}

}
