package com.multidrive.api.service.impl;

import com.multidrive.api.dto.GoogleDriveChangeResponse;
import com.multidrive.api.dto.GoogleDriveRealtimeEventResponse;
import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveConnection;
import com.multidrive.api.entity.GoogleDriveWatchChannel;
import com.multidrive.api.repository.GoogleDriveWatchChannelRepository;
import com.multidrive.api.service.GoogleDriveChangeProcessingService;
import com.multidrive.api.service.GoogleDriveSseService;
import com.multidrive.api.service.GoogleDriveWebhookService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
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

    private final GoogleDriveSseService
            googleDriveSseService;

    public GoogleDriveWebhookServiceImpl(
            GoogleDriveWatchChannelRepository
                    googleDriveWatchChannelRepository,

            GoogleDriveChangeProcessingService
                    googleDriveChangeProcessingService,

            GoogleDriveSseService
                    googleDriveSseService
    ) {

        this.googleDriveWatchChannelRepository =
                googleDriveWatchChannelRepository;

        this.googleDriveChangeProcessingService =
                googleDriveChangeProcessingService;

        this.googleDriveSseService =
                googleDriveSseService;
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
         * Google's initial sync notification can arrive
         * before changes.watch has returned and before
         * the channel row is saved.
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
         * Ignore duplicate or already processed
         * notification messages.
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
         * The initial Google notification is normally
         * resourceState=sync.
         *
         * There is no actual Drive change to process yet.
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

        GoogleDriveChangeTracker tracker =
                channel.getTracker();

        if (tracker == null
                || tracker.getId() == null) {

            throw new IllegalStateException(
                    "Google Drive tracker is missing"
            );
        }

        GoogleDriveConnection connection =
                tracker.getConnection();

        if (connection == null
                || connection.getId() == null) {

            throw new IllegalStateException(
                    "Google Drive connection is missing"
            );
        }

        if (connection.getUser() == null
                || connection.getUser().getId() == null) {

            throw new IllegalStateException(
                    "Google Drive connection user is missing"
            );
        }

        Long trackerId =
                tracker.getId();

        Long connectionId =
                connection.getId();

        Long userId =
                connection
                        .getUser()
                        .getId();

        /*
         * Process Google changes before marking the
         * webhook message as completed.
         */
        List<GoogleDriveChangeResponse> changes =
                googleDriveChangeProcessingService
                        .processChanges(
                                trackerId
                        );

        /*
         * Only update the message number after
         * changes.list completed successfully.
         */
        channel.setLastMessageNumber(
                parsedMessageNumber
        );

        googleDriveWatchChannelRepository
                .save(
                        channel
                );

        GoogleDriveRealtimeEventResponse realtimeEvent =
                new GoogleDriveRealtimeEventResponse(
                        "DRIVE_CHANGES",
                        connectionId,
                        trackerId,
                        tracker.getTrackerType(),
                        tracker.getDriveId(),
                        changes.size(),
                        Instant.now()
                );

        /*
         * Important:
         *
         * Send the SSE message only AFTER the database
         * transaction commits successfully.
         *
         * This prevents the browser from refreshing
         * before our new page token/message number has
         * actually been committed.
         */
        publishAfterCommit(
                userId,
                realtimeEvent
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

    private void publishAfterCommit(
            Long userId,
            GoogleDriveRealtimeEventResponse event
    ) {

        if (TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {

                                @Override
                                public void afterCommit() {

                                    googleDriveSseService
                                            .publishDriveChanges(
                                                    userId,
                                                    event
                                            );
                                }
                            }
                    );

            return;
        }

        googleDriveSseService
                .publishDriveChanges(
                        userId,
                        event
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

        if (channel.getChannelToken() == null
                || channel.getChannelToken().isBlank()) {

            throw new SecurityException(
                    "Stored Google Drive channel token is missing"
            );
        }

        String incomingHash =
                hashChannelToken(
                        channelToken
                );

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