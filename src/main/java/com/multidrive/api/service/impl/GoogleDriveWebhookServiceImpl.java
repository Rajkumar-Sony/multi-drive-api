package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.entity.GoogleDriveWatchChannel;
import com.multidrive.api.repository.GoogleDriveWatchChannelRepository;
import com.multidrive.api.service.GoogleDriveChangeProcessingService;
import com.multidrive.api.service.GoogleDriveWebhookService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleDriveWebhookServiceImpl
        implements GoogleDriveWebhookService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    GoogleDriveWebhookServiceImpl.class
            );

    private static final String CHANNEL_STATUS_ACTIVE =
            "ACTIVE";

    private static final String CHANNEL_STATUS_EXPIRED =
            "EXPIRED";

    private final GoogleDriveWatchChannelRepository
            googleDriveWatchChannelRepository;

    private final GoogleDriveChangeProcessingService
            googleDriveChangeProcessingService;

    public GoogleDriveWebhookServiceImpl(
            GoogleDriveWatchChannelRepository
                    googleDriveWatchChannelRepository,

            GoogleDriveChangeProcessingService
                    googleDriveChangeProcessingService
    ) {

        this.googleDriveWatchChannelRepository =
                googleDriveWatchChannelRepository;

        this.googleDriveChangeProcessingService =
                googleDriveChangeProcessingService;
    }

    @Override
    @Transactional
    public void handleNotification(
            String channelId,
            String channelToken,
            String resourceId,
            String resourceState,
            String messageNumber
    ) {

        validateRequiredHeaders(
                channelId,
                resourceId,
                resourceState,
                messageNumber
        );

        Optional<GoogleDriveWatchChannel> optionalChannel =
                googleDriveWatchChannelRepository
                        .findByChannelIdAndStatus(
                                channelId,
                                CHANNEL_STATUS_ACTIVE
                        );

        /*
         * Google's initial sync notification can reach
         * the webhook before changes.watch returns and
         * before the channel has been saved locally.
         */
        if (optionalChannel.isEmpty()) {

            LOGGER.debug(
                    "Ignoring Google Drive notification "
                            + "for unknown channel {}",
                    channelId
            );

            return;
        }

        GoogleDriveWatchChannel channel =
                optionalChannel.get();

        if (channel.getExpiration() != null
                && !channel
                .getExpiration()
                .isAfter(
                        LocalDateTime.now(
                                ZoneOffset.UTC
                        )
                )) {

            channel.setStatus(
                    CHANNEL_STATUS_EXPIRED
            );

            googleDriveWatchChannelRepository
                    .save(
                            channel
                    );

            return;
        }

        validateChannelToken(
                channel,
                channelToken
        );

        validateResourceId(
                channel,
                resourceId
        );

        long parsedMessageNumber =
                parseMessageNumber(
                        messageNumber
                );

        /*
         * Ignore an already handled notification.
         *
         * Message numbers increase, although they do not
         * have to be consecutive.
         */
        if (channel.getLastMessageNumber() != null
                && parsedMessageNumber
                <= channel.getLastMessageNumber()) {

            LOGGER.debug(
                    "Ignoring duplicate Google Drive notification. "
                            + "channelId={}, messageNumber={}",
                    channelId,
                    parsedMessageNumber
            );

            return;
        }

        /*
         * Google's first message for a new watch channel
         * is normally the sync notification.
         *
         * No changes.list call is needed for that message.
         */
        if ("sync".equalsIgnoreCase(
                resourceState
        )) {

            channel.setLastMessageNumber(
                    parsedMessageNumber
            );

            googleDriveWatchChannelRepository
                    .save(
                            channel
                    );

            LOGGER.info(
                    "Google Drive watch channel synchronized. "
                            + "channelId={}, trackerId={}",
                    channelId,
                    channel
                            .getTracker()
                            .getId()
            );

            return;
        }

        Long trackerId =
                channel
                        .getTracker()
                        .getId();

        /*
         * Important:
         *
         * Process changes BEFORE saving the message number.
         *
         * If changes.list fails, the transaction fails
         * and this notification is not marked as handled.
         */
        List<GoogleDriveChangeResponse> changes =
                googleDriveChangeProcessingService
                        .processChanges(
                                trackerId
                        );

        /*
         * Only mark this notification as processed after
         * changes.list completed successfully and the new
         * page token was saved.
         */
        channel.setLastMessageNumber(
                parsedMessageNumber
        );

        googleDriveWatchChannelRepository
                .save(
                        channel
                );

        LOGGER.info(
                "Google Drive webhook processed successfully. "
                        + "channelId={}, trackerId={}, "
                        + "messageNumber={}, changeCount={}",
                channelId,
                trackerId,
                parsedMessageNumber,
                changes.size()
        );
    }

    private void validateChannelToken(
            GoogleDriveWatchChannel channel,
            String channelToken
    ) {

        if (channelToken == null
                || channelToken.isBlank()) {

            throw new SecurityException(
                    "Google Drive channel token is missing"
            );
        }

        String incomingHash =
                hashChannelToken(
                        channelToken
                );

        if (channel.getChannelToken() == null
                || channel.getChannelToken().isBlank()) {

            throw new SecurityException(
                    "Stored Google Drive channel token is missing"
            );
        }

        byte[] expected =
                channel
                        .getChannelToken()
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        byte[] actual =
                incomingHash
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        if (!MessageDigest.isEqual(
                expected,
                actual
        )) {

            throw new SecurityException(
                    "Google Drive channel token is invalid"
            );
        }
    }

    private void validateResourceId(
            GoogleDriveWatchChannel channel,
            String resourceId
    ) {

        if (channel.getResourceId() == null
                || channel.getResourceId().isBlank()) {

            return;
        }

        if (!channel
                .getResourceId()
                .equals(resourceId)) {

            throw new SecurityException(
                    "Google Drive resource ID does not match"
            );
        }
    }

    private long parseMessageNumber(
            String messageNumber
    ) {

        try {

            long value =
                    Long.parseLong(
                            messageNumber
                    );

            if (value < 1) {

                throw new IllegalArgumentException(
                        "Google Drive message number must be positive"
                );
            }

            return value;

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Google Drive message number is invalid",
                    exception
            );
        }
    }

    private String hashChannelToken(
            String channelToken
    ) {

        try {

            MessageDigest messageDigest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    messageDigest.digest(
                            channelToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64
                    .getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(
                            hash
                    );

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }

    private void validateRequiredHeaders(
            String channelId,
            String resourceId,
            String resourceState,
            String messageNumber
    ) {

        if (channelId == null
                || channelId.isBlank()) {

            throw new IllegalArgumentException(
                    "X-Goog-Channel-ID is required"
            );
        }

        if (resourceId == null
                || resourceId.isBlank()) {

            throw new IllegalArgumentException(
                    "X-Goog-Resource-ID is required"
            );
        }

        if (resourceState == null
                || resourceState.isBlank()) {

            throw new IllegalArgumentException(
                    "X-Goog-Resource-State is required"
            );
        }

        if (messageNumber == null
                || messageNumber.isBlank()) {

            throw new IllegalArgumentException(
                    "X-Goog-Message-Number is required"
            );
        }
    }
}