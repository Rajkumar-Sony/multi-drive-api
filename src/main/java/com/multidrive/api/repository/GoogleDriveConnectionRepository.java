package com.multidrive.api.repository;

import com.multidrive.api.entity.GoogleDriveConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoogleDriveConnectionRepository
        extends JpaRepository<GoogleDriveConnection, Long> {

    List<GoogleDriveConnection> findAllByUserId(Long userId);

    Optional<GoogleDriveConnection> findByIdAndUserId(
            Long connectionId,
            Long userId
    );

    Optional<GoogleDriveConnection> findByUserIdAndGoogleSubjectId(
            Long userId,
            String googleSubjectId
    );

    boolean existsByUserIdAndGoogleSubjectId(
            Long userId,
            String googleSubjectId
    );
}