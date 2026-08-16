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

    private final UnifiedDriveViewService
            unifiedDriveViewService;

    private final UserService
            userService;

    public UnifiedDriveViewController(
            UnifiedDriveViewService
                    unifiedDriveViewService,

            UserService
                    userService
    ) {

        this.unifiedDriveViewService =
                unifiedDriveViewService;

        this.userService =
                userService;
    }

    /**
     * All folders and files from all connected
     * Google Drive accounts.
     *
     * GET /api/dashboard
     */
    @GetMapping("/api/dashboard")
    public UnifiedDriveItemsPageResponse getDashboard(

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "50"
            )
            Integer size,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        User user =
                getApplicationUser(
                        oidcUser
                );

        return unifiedDriveViewService
                .getDashboard(
                        user.getId(),
                        page,
                        size
                );
    }

    /**
     * All document files from every connected
     * Google Drive account.
     *
     * GET /api/docs
     */
    @GetMapping("/api/docs")
    public UnifiedDriveItemsPageResponse getDocs(

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "50"
            )
            Integer size,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        User user =
                getApplicationUser(
                        oidcUser
                );

        return unifiedDriveViewService
                .getDocs(
                        user.getId(),
                        page,
                        size
                );
    }

    /**
     * All image files from every connected
     * Google Drive account.
     *
     * GET /api/gallery
     */
    @GetMapping("/api/gallery")
    public UnifiedDriveItemsPageResponse getGallery(

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "50"
            )
            Integer size,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        User user =
                getApplicationUser(
                        oidcUser
                );

        return unifiedDriveViewService
                .getGallery(
                        user.getId(),
                        page,
                        size
                );
    }

    /**
     * All video files from every connected
     * Google Drive account.
     *
     * GET /api/videos
     */
    @GetMapping("/api/videos")
    public UnifiedDriveItemsPageResponse getVideos(

            @RequestParam(
                    defaultValue = "0"
            )
            Integer page,

            @RequestParam(
                    defaultValue = "50"
            )
            Integer size,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        User user =
                getApplicationUser(
                        oidcUser
                );

        return unifiedDriveViewService
                .getVideos(
                        user.getId(),
                        page,
                        size
                );
    }

    private User getApplicationUser(
            OidcUser oidcUser
    ) {

        if (oidcUser == null) {

            throw new IllegalStateException(
                    "Authenticated application user not found"
            );
        }

        return userService
                .findByGoogleSubjectId(
                        oidcUser.getSubject()
                );
    }
}