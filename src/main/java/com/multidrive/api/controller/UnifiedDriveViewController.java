package com.multidrive.api.controller;

import com.multidrive.api.dto.UnifiedDriveItemsPageResponse;
import com.multidrive.api.entity.User;
import com.multidrive.api.service.UnifiedDriveViewService;
import com.multidrive.api.service.UserService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnifiedDriveViewController {

	private final UnifiedDriveViewService unifiedDriveViewService;

	private final UserService userService;

	public UnifiedDriveViewController(UnifiedDriveViewService unifiedDriveViewService, UserService userService) {

		this.unifiedDriveViewService = unifiedDriveViewService;

		this.userService = userService;
	}

	/**
	 * Unified Dashboard.
	 *
	 * All non-trashed folders and files from all connected Google Drive accounts.
	 *
	 * Optional filters:
	 *
	 * connectionId = one connected Google account parentId = children of one folder q =
	 * search file/folder name
	 */
	@GetMapping("/api/dashboard")
	public UnifiedDriveItemsPageResponse getDashboard(

			@RequestParam(required = false) Long connectionId,

			@RequestParam(required = false) String parentId,

			@RequestParam(name = "q", required = false) String searchQuery,

			@RequestParam(defaultValue = "0") Integer page,

			@RequestParam(defaultValue = "50") Integer size,

			@AuthenticationPrincipal OidcUser oidcUser) {

		User user = getApplicationUser(oidcUser);

		return unifiedDriveViewService.getDashboard(user.getId(), connectionId, parentId, searchQuery, page, size);
	}

	/**
	 * Unified Docs.
	 */
	@GetMapping("/api/docs")
	public UnifiedDriveItemsPageResponse getDocs(

			@RequestParam(required = false) Long connectionId,

			@RequestParam(required = false) String parentId,

			@RequestParam(name = "q", required = false) String searchQuery,

			@RequestParam(defaultValue = "0") Integer page,

			@RequestParam(defaultValue = "50") Integer size,

			@AuthenticationPrincipal OidcUser oidcUser) {

		User user = getApplicationUser(oidcUser);

		return unifiedDriveViewService.getDocs(user.getId(), connectionId, parentId, searchQuery, page, size);
	}

	/**
	 * Unified Gallery.
	 */
	@GetMapping("/api/gallery")
	public UnifiedDriveItemsPageResponse getGallery(

			@RequestParam(required = false) Long connectionId,

			@RequestParam(required = false) String parentId,

			@RequestParam(name = "q", required = false) String searchQuery,

			@RequestParam(defaultValue = "0") Integer page,

			@RequestParam(defaultValue = "50") Integer size,

			@AuthenticationPrincipal OidcUser oidcUser) {

		User user = getApplicationUser(oidcUser);

		return unifiedDriveViewService.getGallery(user.getId(), connectionId, parentId, searchQuery, page, size);
	}

	/**
	 * Unified Videos.
	 */
	@GetMapping("/api/videos")
	public UnifiedDriveItemsPageResponse getVideos(

			@RequestParam(required = false) Long connectionId,

			@RequestParam(required = false) String parentId,

			@RequestParam(name = "q", required = false) String searchQuery,

			@RequestParam(defaultValue = "0") Integer page,

			@RequestParam(defaultValue = "50") Integer size,

			@AuthenticationPrincipal OidcUser oidcUser) {

		User user = getApplicationUser(oidcUser);

		return unifiedDriveViewService.getVideos(user.getId(), connectionId, parentId, searchQuery, page, size);
	}

	private User getApplicationUser(OidcUser oidcUser) {

		if (oidcUser == null) {

			throw new IllegalStateException("Authenticated application user not found");
		}

		return userService.findByGoogleSubjectId(oidcUser.getSubject());
	}

}