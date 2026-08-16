package com.multidrive.api.service;

import com.multidrive.api.exception.DriveItemNotFoundException;
import com.multidrive.api.exception.DriveOperationCleanupRequiredException;
import com.multidrive.api.exception.DriveOperationNotAllowedException;
import com.multidrive.api.exception.DriveOperationReconciliationPendingException;

import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Component
public class DriveOperationRetryPolicy {

	private static final Duration INITIAL_DELAY = Duration.ofSeconds(5);

	private static final Duration MAX_DELAY = Duration.ofMinutes(5);

	public boolean shouldRetry(Throwable throwable) {

		Throwable cause = unwrap(throwable);

		if (cause instanceof DriveOperationCleanupRequiredException) {

			return false;
		}

		if (cause instanceof DriveOperationReconciliationPendingException) {

			return true;
		}

		if (cause instanceof DriveOperationNotAllowedException) {

			return false;
		}

		if (cause instanceof DriveItemNotFoundException) {

			return false;
		}

		if (cause instanceof IllegalArgumentException) {

			return false;
		}

		if (cause instanceof ResourceAccessException) {

			return true;
		}

		if (cause instanceof RestClientResponseException responseException) {

			int status = responseException.getStatusCode().value();

			return status == 408 || status == 429 || status >= 500;
		}

		return true;
	}

	public Duration retryDelay(int attemptCount) {

		int exponent = Math.max(0, attemptCount - 1);

		long multiplier = 1L << Math.min(exponent, 10);

		Duration calculated = INITIAL_DELAY.multipliedBy(multiplier);

		if (calculated.compareTo(MAX_DELAY) > 0) {

			return MAX_DELAY;
		}

		return calculated;
	}

	public String errorCode(Throwable throwable) {

		Throwable cause = unwrap(throwable);

		if (cause instanceof DriveOperationCleanupRequiredException cleanupRequiredException) {

			return cleanupRequiredException.getErrorCode();
		}

		if (cause instanceof DriveOperationReconciliationPendingException) {

			return "COPY_RECONCILIATION_PENDING";
		}

		if (cause instanceof DriveOperationNotAllowedException) {

			return "OPERATION_NOT_ALLOWED";
		}

		if (cause instanceof DriveItemNotFoundException) {

			return "ITEM_NOT_FOUND";
		}

		if (cause instanceof ResourceAccessException) {

			return "GOOGLE_NETWORK_ERROR";
		}

		if (cause instanceof RestClientResponseException responseException) {

			return "GOOGLE_HTTP_" + responseException.getStatusCode().value();
		}

		if (cause instanceof IllegalArgumentException) {

			return "INVALID_OPERATION_STATE";
		}

		return "WORKER_ERROR";
	}

	public String errorMessage(Throwable throwable) {

		Throwable cause = unwrap(throwable);

		String message = cause.getMessage();

		if (message == null || message.isBlank()) {

			message = cause.getClass().getSimpleName();
		}

		if (message.length() > 4000) {

			return message.substring(0, 4000);
		}

		return message;
	}

	public Throwable unwrap(Throwable throwable) {

		Throwable current = throwable;

		while (current.getCause() != null && current.getCause() != current) {

			current = current.getCause();
		}

		return current;
	}

}
