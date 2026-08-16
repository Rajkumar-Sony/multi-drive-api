package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveCreateFolderRequest;
import com.multidrive.api.dto.DriveItemDetailsResponse;
import com.multidrive.api.dto.DriveRenameRequest;
import com.multidrive.api.service.DriveOperationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
        "/api/drive"
)
public class DriveOperationController {

    private final DriveOperationService
            driveOperationService;

    public DriveOperationController(
            DriveOperationService driveOperationService
    ) {

        this.driveOperationService =
                driveOperationService;
    }

    @PostMapping("/folders")
    public DriveItemDetailsResponse createFolder(

            @RequestBody
            DriveCreateFolderRequest request,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationService
                .createFolder(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        request
                );
    }

    @PatchMapping(
            "/items/{itemId}/rename"
    )
    public DriveItemDetailsResponse rename(

            @PathVariable
            Long itemId,

            @RequestBody
            DriveRenameRequest request,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationService
                .rename(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        itemId,
                        request
                );
    }

    @PostMapping(
            "/items/{itemId}/trash"
    )
    public DriveItemDetailsResponse trash(

            @PathVariable
            Long itemId,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationService
                .trash(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        itemId
                );
    }

    @PostMapping(
            "/items/{itemId}/restore"
    )
    public DriveItemDetailsResponse restore(

            @PathVariable
            Long itemId,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationService
                .restore(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        itemId
                );
    }

    @DeleteMapping(
            "/items/{itemId}/permanent"
    )
    public ResponseEntity<Void> permanentlyDelete(

            @PathVariable
            Long itemId,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        driveOperationService
                .permanentlyDelete(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        itemId
                );

        return ResponseEntity
                .noContent()
                .build();
    }

    private String requireGoogleSubjectId(
            OidcUser oidcUser
    ) {

        if (oidcUser == null
                || oidcUser.getSubject() == null
                || oidcUser.getSubject().isBlank()) {

            throw new IllegalStateException(
                    "Authenticated application user not found"
            );
        }

        return oidcUser.getSubject();
    }
}
