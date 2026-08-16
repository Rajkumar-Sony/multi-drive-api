package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveCopyRequest;
import com.multidrive.api.dto.DriveCreateFolderRequest;
import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveMoveRequest;
import com.multidrive.api.dto.DriveRenameRequest;
import com.multidrive.api.security.AuthenticatedUserResolver;
import com.multidrive.api.service.DriveOperationService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/drive")
public class DriveOperationController {

	private final DriveOperationService driveOperationService;

	private final AuthenticatedUserResolver authenticatedUserResolver;

	public DriveOperationController(DriveOperationService driveOperationService,
			AuthenticatedUserResolver authenticatedUserResolver) {

		this.driveOperationService = driveOperationService;

		this.authenticatedUserResolver = authenticatedUserResolver;
	}

	@PostMapping("/folders")
	public DriveItemDetailsResponse createFolder(

			@RequestBody @Valid @NotNull(message = "request is required") DriveCreateFolderRequest request,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationService.createFolder(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), request);
	}

	@PatchMapping("/items/{itemId}/rename")
	public DriveItemDetailsResponse rename(

			@PathVariable Long itemId,

			@RequestBody @Valid @NotNull(message = "request is required") DriveRenameRequest request,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationService.rename(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), itemId,
				request);
	}

	@PostMapping("/items/{itemId}/move")
	public DriveItemDetailsResponse move(

			@PathVariable Long itemId,

			@RequestBody @Valid @NotNull(message = "request is required") DriveMoveRequest request,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationService.move(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), itemId, request);
	}

	@PostMapping("/items/{itemId}/copy")
	public DriveItemDetailsResponse copy(

			@PathVariable Long itemId,

			@RequestBody @Valid @NotNull(message = "request is required") DriveCopyRequest request,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationService.copy(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), itemId, request);
	}

	@PostMapping("/items/{itemId}/trash")
	public DriveItemDetailsResponse trash(

			@PathVariable Long itemId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationService.trash(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), itemId);
	}

	@PostMapping("/items/{itemId}/restore")
	public DriveItemDetailsResponse restore(

			@PathVariable Long itemId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		return driveOperationService.restore(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), itemId);
	}

	@DeleteMapping("/items/{itemId}/permanent")
	public ResponseEntity<Void> permanentlyDelete(

			@PathVariable Long itemId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		driveOperationService.permanentlyDelete(authenticatedUserResolver.requireGoogleSubjectId(oidcUser), itemId);

		return ResponseEntity.noContent().build();
	}

}
