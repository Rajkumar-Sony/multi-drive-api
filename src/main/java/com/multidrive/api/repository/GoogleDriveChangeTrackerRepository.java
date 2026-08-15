package com.multidrive.api.repository;

import com.multidrive.api.entity.GoogleDriveChangeTracker;
import com.multidrive.api.entity.GoogleDriveTrackerType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoogleDriveChangeTrackerRepository
        extends JpaRepository<
                GoogleDriveChangeTracker,
                Long
        > {

    List<GoogleDriveChangeTracker>
    findAllByConnection_Id(
            Long connectionId
    );

    Optional<GoogleDriveChangeTracker>
    findByConnection_IdAndTrackerTypeAndDriveIdIsNull(
            Long connectionId,
            GoogleDriveTrackerType trackerType
    );

    Optional<GoogleDriveChangeTracker>
    findByConnection_IdAndTrackerTypeAndDriveId(
            Long connectionId,
            GoogleDriveTrackerType trackerType,
            String driveId
    );

    List<GoogleDriveChangeTracker>
    findAllByConnection_IdAndStatus(
            Long connectionId,
            String status
    );
}