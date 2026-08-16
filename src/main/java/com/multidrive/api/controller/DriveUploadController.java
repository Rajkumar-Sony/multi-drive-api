package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveUploadRequest;
import com.multidrive.api.service.DriveUploadService;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/drive/files")
public class DriveUploadController {

	private final DriveUploadService driveUploadService;

	public DriveUploadController(DriveUploadService driveUploadService) {

		this.driveUploadService = driveUploadService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public DriveItemDetailsResponse upload(

			@RequestParam Long sourceId,

			@RequestParam(required = false) Long parentItemId,

			@RequestParam(required = false) String name,

			@RequestPart("file") MultipartFile file,

			@AuthenticationPrincipal OidcUser oidcUser) {

		DriveUploadRequest request = new DriveUploadRequest(sourceId, parentItemId, name, file);

		return driveUploadService.upload(requireGoogleSubjectId(oidcUser), request);
	}

	private String requireGoogleSubjectId(OidcUser oidcUser) {

		if (oidcUser == null || oidcUser.getSubject() == null || oidcUser.getSubject().isBlank()) {

			throw new IllegalStateException("Authenticated application user not found");
		}

		return oidcUser.getSubject();
	}

}
