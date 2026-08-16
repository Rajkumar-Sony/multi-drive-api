package com.multidrive.api.repository;

import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GoogleDriveItemRepository
        extends JpaRepository<GoogleDriveItem, Long>,
        JpaSpecificationExecutor<GoogleDriveItem> {

    Optional<GoogleDriveItem>
    findByConnection_IdAndGoogleFileId(
            Long connectionId,
            String googleFileId
    );

    List<GoogleDriveItem>
    findAllByConnection_Id(
            Long connectionId
    );

    List<GoogleDriveItem>
    findAllByConnection_IdAndGoogleFileIdIn(
            Long connectionId,
            Collection<String> googleFileIds
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

    @Modifying
    @Query("""
            DELETE FROM GoogleDriveItem item
            WHERE item.connection.id = :connectionId
              AND (
                    item.syncRunId IS NULL
                    OR item.syncRunId <> :syncRunId
              )
            """)
    int deleteStaleItems(
            @Param("connectionId")
            Long connectionId,

            @Param("syncRunId")
            String syncRunId
    );
}