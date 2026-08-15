package com.multidrive.api.repository;

import com.multidrive.api.entity.GoogleDriveWatchChannel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GoogleDriveWatchChannelRepository
        extends JpaRepository<
                GoogleDriveWatchChannel,
                Long
        > {

    Optional<GoogleDriveWatchChannel>
    findByChannelId(
            String channelId
    );

    Optional<GoogleDriveWatchChannel>
    findByChannelIdAndStatus(
            String channelId,
            String status
    );

    List<GoogleDriveWatchChannel>
    findAllByTracker_Id(
            Long trackerId
    );

    List<GoogleDriveWatchChannel>
    findAllByTracker_IdAndStatus(
            Long trackerId,
            String status
    );

    List<GoogleDriveWatchChannel>
    findAllByStatusAndExpirationBefore(
            String status,
            LocalDateTime expiration
    );
}