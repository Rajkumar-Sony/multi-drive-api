package com.multidrive.api.repository;

import com.multidrive.api.entity.GoogleDriveItem;
import com.multidrive.api.entity.GoogleDriveItemCategory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /*
     * N+1-safe item lookup.
     *
     * connection + application owner + source are loaded
     * in the same SQL query.
     *
     * Capabilities are embedded columns on the item, so
     * they require no extra query.
     */
    @Query("""
            SELECT item
            FROM GoogleDriveItem item
            JOIN FETCH item.connection conn
            JOIN FETCH conn.user owner
            JOIN FETCH item.source source
            WHERE item.id = :itemId
              AND owner.googleSubjectId = :googleSubjectId
            """)
    Optional<GoogleDriveItem>
    findOwnedItemForDetails(
            @Param("itemId")
            Long itemId,

            @Param("googleSubjectId")
            String googleSubjectId
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