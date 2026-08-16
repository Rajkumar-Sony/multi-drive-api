package com.multidrive.api.repository;

import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoogleDriveItemRepository
        extends JpaRepository<
                GoogleDriveItem,
                Long
        > {

    Optional<GoogleDriveItem>
    findByConnection_IdAndGoogleFileId(
            Long connectionId,
            String googleFileId
    );

    List<GoogleDriveItem>
    findAllByConnection_Id(
            Long connectionId
    );

    Page<GoogleDriveItem>
    findAllByConnection_User_IdAndTrashedFalse(
            Long userId,
            Pageable pageable
    );

    Page<GoogleDriveItem>
    findAllByConnection_User_IdAndCategoryAndTrashedFalse(
            Long userId,
            GoogleDriveItemCategory category,
            Pageable pageable
    );

    List<GoogleDriveItem>
    findAllByConnection_IdAndDriveId(
            Long connectionId,
            String driveId
    );

    boolean existsByConnection_IdAndGoogleFileId(
            Long connectionId,
            String googleFileId
    );

    void deleteByConnection_IdAndGoogleFileId(
            Long connectionId,
            String googleFileId
    );
}