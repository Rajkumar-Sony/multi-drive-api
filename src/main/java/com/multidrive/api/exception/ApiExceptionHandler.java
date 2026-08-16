package com.multidrive.api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final URI VALIDATION_TYPE = URI.create("urn:multidrive:problem:validation-failed");

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {

		ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed",
				"Request validation failed");

		problemDetail.setType(VALIDATION_TYPE);

		problemDetail.setProperty("errors",
				exception.getBindingResult().getFieldErrors().stream().map(this::toValidationError).toList());

		return problemDetail;
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException exception) {

		ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed",
				"Request validation failed");

		problemDetail.setType(VALIDATION_TYPE);

		problemDetail.setProperty("errors",
				exception.getParameterValidationResults()
					.stream()
					.flatMap(result -> result.getResolvableErrors()
						.stream()
						.map(error -> new ValidationError(result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
					.toList());

		return problemDetail;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {

		ProblemDetail problemDetail = createProblemDetail(HttpStatus.BAD_REQUEST, "Validation failed",
				"Request validation failed");

		problemDetail.setType(VALIDATION_TYPE);

		List<ValidationError> errors = exception.getConstraintViolations()
			.stream()
			.map(this::toValidationError)
			.toList();

		problemDetail.setProperty("errors", errors);

		return problemDetail;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {

		return createProblemDetail(HttpStatus.BAD_REQUEST, "Invalid request", exception.getMessage());
	}

	@ExceptionHandler(DriveItemNotFoundException.class)
	public ProblemDetail handleDriveItemNotFound(DriveItemNotFoundException exception) {

		return createProblemDetail(HttpStatus.NOT_FOUND, "Drive item not found", exception.getMessage());
	}

	@ExceptionHandler(DriveOperationJobNotFoundException.class)
	public ProblemDetail handleDriveOperationJobNotFound(DriveOperationJobNotFoundException exception) {

		return createProblemDetail(HttpStatus.NOT_FOUND, "Drive operation job not found", exception.getMessage());
	}

	@ExceptionHandler(DriveOperationNotAllowedException.class)
	public ProblemDetail handleDriveOperationNotAllowed(DriveOperationNotAllowedException exception) {

		return createProblemDetail(HttpStatus.FORBIDDEN, "Drive operation not allowed", exception.getMessage());
	}

	@ExceptionHandler(DriveContentNotSupportedException.class)
	public ProblemDetail handleDriveContentNotSupported(DriveContentNotSupportedException exception) {

		return createProblemDetail(HttpStatus.BAD_REQUEST, "Drive content not supported", exception.getMessage());
	}

	@ExceptionHandler(DriveOperationPlanningException.class)
	public ProblemDetail handleDriveOperationPlanning(DriveOperationPlanningException exception) {

		return createProblemDetail(HttpStatus.CONFLICT, "Drive operation planning failed", exception.getMessage());
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ProblemDetail handleResponseStatus(ResponseStatusException exception) {

		HttpStatusCode statusCode = exception.getStatusCode();

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(statusCode, exception.getReason());

		if (problemDetail.getDetail() == null || problemDetail.getDetail().isBlank()) {

			problemDetail.setDetail(exception.getMessage());
		}

		HttpStatus status = HttpStatus.resolve(statusCode.value());

		if (status != null) {

			problemDetail.setTitle(status.getReasonPhrase());
		}

		return problemDetail;
	}

	@ExceptionHandler(ErrorResponseException.class)
	public ProblemDetail handleErrorResponse(ErrorResponseException exception) {

		return exception.getBody();
	}

	private ProblemDetail createProblemDetail(HttpStatus status, String title, String detail) {

		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);

		problemDetail.setTitle(title);

		return problemDetail;
	}

	private ValidationError toValidationError(FieldError fieldError) {

		return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
	}

	private ValidationError toValidationError(ConstraintViolation<?> violation) {

		return new ValidationError(violation.getPropertyPath().toString(), violation.getMessage());
	}

	private record ValidationError(

			String field,

			String message) {
	}

}
