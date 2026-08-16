package com.multidrive.api.service;

public interface GoogleDriveWebhookService {

    void handleNotification(
            String channelId,
            String channelToken,
            String resourceId,
            String resourceState,
            String messageNumber
    );
}