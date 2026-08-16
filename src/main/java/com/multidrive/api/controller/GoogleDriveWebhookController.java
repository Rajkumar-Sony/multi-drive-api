package com.multidrive.api.controller;

import com.multidrive.api.service.GoogleDriveWebhookService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/google/webhooks")
public class GoogleDriveWebhookController {

    private final GoogleDriveWebhookService
            googleDriveWebhookService;

    public GoogleDriveWebhookController(
            GoogleDriveWebhookService
                    googleDriveWebhookService
    ) {

        this.googleDriveWebhookService =
                googleDriveWebhookService;
    }

    @PostMapping("/drive")
    public ResponseEntity<Void> receiveDriveNotification(

            @RequestHeader(
                    name = "X-Goog-Channel-ID",
                    required = false
            )
            String channelId,

            @RequestHeader(
                    name = "X-Goog-Channel-Token",
                    required = false
            )
            String channelToken,

            @RequestHeader(
                    name = "X-Goog-Resource-ID",
                    required = false
            )
            String resourceId,

            @RequestHeader(
                    name = "X-Goog-Resource-State",
                    required = false
            )
            String resourceState,

            @RequestHeader(
                    name = "X-Goog-Message-Number",
                    required = false
            )
            String messageNumber
    ) {

        try {

            googleDriveWebhookService
                    .handleNotification(
                            channelId,
                            channelToken,
                            resourceId,
                            resourceState,
                            messageNumber
                    );

            return ResponseEntity
                    .noContent()
                    .build();

        } catch (SecurityException exception) {

            return ResponseEntity
                    .status(
                            HttpStatus.FORBIDDEN
                    )
                    .build();

        } catch (IllegalArgumentException exception) {

            return ResponseEntity
                    .badRequest()
                    .build();
        }
    }
}