package com.multidrive.api.controller;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.dto.DriveOperationJobSubmitRequest;
import com.multidrive.api.dto.DriveOperationJobsPageResponse;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.service.DriveOperationJobControlService;
import com.multidrive.api.service.DriveOperationJobService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
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
@RequestMapping(
        "/api/drive/operation-jobs"
)
public class DriveOperationJobController {

    private final DriveOperationJobService
            driveOperationJobService;

    private final DriveOperationJobControlService
            driveOperationJobControlService;

    public DriveOperationJobController(
            DriveOperationJobService driveOperationJobService,
            DriveOperationJobControlService driveOperationJobControlService
    ) {

        this.driveOperationJobService =
                driveOperationJobService;

        this.driveOperationJobControlService =
                driveOperationJobControlService;
    }

    @PostMapping
    @ResponseStatus(
            HttpStatus.ACCEPTED
    )
    public DriveOperationJobResponse submit(

            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            )
            String idempotencyKey,

            @RequestBody
            DriveOperationJobSubmitRequest request,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationJobService
                .submit(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        idempotencyKey,
                        request
                );
    }

    @GetMapping(
            "/{jobId}"
    )
    public DriveOperationJobResponse getJob(

            @PathVariable
            Long jobId,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationJobService
                .getJob(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        jobId
                );
    }

    @GetMapping
    public DriveOperationJobsPageResponse getJobs(

            @RequestParam(
                    required = false
            )
            DriveOperationJobStatus status,

            @RequestParam(
                    required = false
            )
            Integer page,

            @RequestParam(
                    required = false
            )
            Integer size,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationJobService
                .getJobs(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        status,
                        page,
                        size
                );
    }

    @PostMapping(
            "/{jobId}/cancel"
    )
    public DriveOperationJobResponse cancel(

            @PathVariable
            Long jobId,

            @AuthenticationPrincipal
            OidcUser oidcUser
    ) {

        return driveOperationJobControlService
                .requestCancellation(
                        requireGoogleSubjectId(
                                oidcUser
                        ),
                        jobId
                );
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
