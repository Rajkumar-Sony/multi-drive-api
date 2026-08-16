package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.dto.DriveOperationJobSubmitRequest;
import com.multidrive.api.dto.DriveOperationJobsPageResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.security.AuthenticatedUserResolver;
import com.multidrive.api.service.DriveOperationJobControlService;
import com.multidrive.api.service.DriveOperationJobService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/drive/operation-jobs")
public class DriveOperationJobController {

	private final DriveOperationJobService driveOperationJobService;

	private final DriveOperationJobControlService driveOperationJobControlService;

	private final AuthenticatedUserResolver authenticatedUserResolver;

	public DriveOperationJobController(DriveOperationJobService driveOperationJobService,
			DriveOperationJobControlService driveOperationJobControlService,
			AuthenticatedUserResolver authenticatedUserResolver) {

		this.driveOperationJobService = driveOperationJobService;

		this.driveOperationJobControlService = driveOperationJobControlService;

		this.authenticatedUserResolver = authenticatedUserResolver;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public DriveOperationJobResponse submit(

			@RequestHeader(value = "Idempotency-Key", required = false) @Size(max = 128,
					message = "Idempotency-Key must not exceed 128 characters") String idempotencyKey,

			@RequestBody @Valid @NotNull(message = "request is required") DriveOperationJobSubmitRequest request,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationJobService.submit(authenticatedUserResolver.requireGoogleSubjectId(oidcUser),
				idempotencyKey, request);
	}

	@GetMapping("/{jobId}")
	public DriveOperationJobResponse getJob(

			@PathVariable Long jobId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationJobService.getJob(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), jobId);
	}

	@GetMapping
	public DriveOperationJobsPageResponse getJobs(

			@RequestParam(required = false) DriveOperationJobStatus status,

			@RequestParam(required = false) @Min(value = 0,
					message = "page must be greater than or equal to 0") Integer page,

			@RequestParam(required = false) @Min(value = 1, message = "size must be between 1 and 100") @Max(
					value = 100, message = "size must be between 1 and 100") Integer size,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationJobService.getJobs(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), status,
				page, size);
	}

	@PostMapping("/{jobId}/cancel")
	public DriveOperationJobResponse cancel(

			@PathVariable Long jobId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationJobControlService
			.requestCancellation(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), jobId);
	}

}
