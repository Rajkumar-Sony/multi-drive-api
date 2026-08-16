package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.service.DriveItemLookupService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/drive/items")
public class DriveItemController {

	private final DriveItemLookupService driveItemLookupService;

	public DriveItemController(DriveItemLookupService driveItemLookupService) {

		this.driveItemLookupService = driveItemLookupService;
	}

	@GetMapping("/{itemId}")
	public DriveItemDetailsResponse getItem(

			@PathVariable Long itemId,

			@AuthenticationPrincipal OidcUser oidcUser) {

		if (oidcUser == null) {

			throw new IllegalStateException("Authenticated application user not found");
		}

		return driveItemLookupService.getItem(oidcUser.getSubject(), itemId);
	}

}