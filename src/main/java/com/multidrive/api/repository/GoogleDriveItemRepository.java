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
		extends JpaRepository<GoogleDriveItem, Long>, JpaSpecificationExecutor<GoogleDriveItem> {

	Optional<GoogleDriveItem> findByConnection_IdAndGoogleFileId(Long connectionId, String googleFileId);

	List<GoogleDriveItem> findAllByConnection_Id(Long connectionId);

	List<GoogleDriveItem> findAllByConnection_IdAndGoogleFileIdIn(Long connectionId, Collection<String> googleFileIds);

	Page<GoogleDriveItem> findAllByConnection_User_IdAndTrashedFalse(Long userId, Pageable pageable);

	Page<GoogleDriveItem> findAllByConnection_User_IdAndCategoryAndTrashedFalse(Long userId,
			GoogleDriveItemCategory category, Pageable pageable);

	List<GoogleDriveItem> findAllByConnection_IdAndDriveId(Long connectionId, String driveId);

	boolean existsByConnection_IdAndGoogleFileId(Long connectionId, String googleFileId);

	void deleteByConnection_IdAndGoogleFileId(Long connectionId, String googleFileId);

	/*
	 * N+1-safe item lookup.
	 *
	 * connection + application owner + source are loaded in the same SQL query.
	 *
	 * Capabilities are embedded columns on the item, so they require no extra query.
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
	Optional<GoogleDriveItem> findOwnedItemForDetails(@Param("itemId") Long itemId,

			@Param("googleSubjectId") String googleSubjectId);

	/*
	 * Used before moving a folder.
	 *
	 * We must not move a folder into one of its own descendants. PostgreSQL resolves the
	 * hierarchy in one recursive query.
	 */
	@Query(value = """
			WITH RECURSIVE descendants AS (

			    SELECT
			        child.google_file_id
			    FROM google_drive_items child
			    WHERE child.connection_id = :connectionId
			      AND child.source_id = :sourceId
			      AND child.parent_id = :rootGoogleFileId

			    UNION ALL

			    SELECT
			        child.google_file_id
			    FROM google_drive_items child
			    JOIN descendants parent
			      ON child.parent_id = parent.google_file_id
			    WHERE child.connection_id = :connectionId
			      AND child.source_id = :sourceId
			)

			SELECT EXISTS (
			    SELECT 1
			    FROM descendants
			    WHERE google_file_id = :candidateGoogleFileId
			)
			""", nativeQuery = true)
	boolean isDescendant(@Param("connectionId") Long connectionId,

			@Param("sourceId") Long sourceId,

			@Param("rootGoogleFileId") String rootGoogleFileId,

			@Param("candidateGoogleFileId") String candidateGoogleFileId);

	/*
	 * Folder trash:
	 *
	 * Mark the complete local descendant tree as trashed without changing
	 * explicitly_trashed.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			WITH RECURSIVE descendants AS (

			    SELECT
			        child.id,
			        child.google_file_id
			    FROM google_drive_items child
			    WHERE child.connection_id = :connectionId
			      AND child.source_id = :sourceId
			      AND child.parent_id = :rootGoogleFileId

			    UNION ALL

			    SELECT
			        child.id,
			        child.google_file_id
			    FROM google_drive_items child
			    JOIN descendants parent
			      ON child.parent_id = parent.google_file_id
			    WHERE child.connection_id = :connectionId
			      AND child.source_id = :sourceId
			)

			UPDATE google_drive_items item
			SET
			    trashed = TRUE,
			    updated_at = CURRENT_TIMESTAMP
			FROM descendants
			WHERE item.id = descendants.id
			""", nativeQuery = true)
	int markDescendantsTrashed(@Param("connectionId") Long connectionId,

			@Param("sourceId") Long sourceId,

			@Param("rootGoogleFileId") String rootGoogleFileId);

	/*
	 * Folder restore:
	 *
	 * Restore descendants that were trashed only because of the restored parent.
	 * Explicitly trashed nested folders keep their subtree trashed.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			WITH RECURSIVE subtree AS (

			    SELECT
			        child.id,
			        child.google_file_id,
			        COALESCE(
			            child.explicitly_trashed,
			            child.trashed,
			            FALSE
			        ) AS keep_trashed
			    FROM google_drive_items child
			    WHERE child.connection_id = :connectionId
			      AND child.source_id = :sourceId
			      AND child.parent_id = :rootGoogleFileId

			    UNION ALL

			    SELECT
			        child.id,
			        child.google_file_id,
			        (
			            parent.keep_trashed
			            OR
			            COALESCE(
			                child.explicitly_trashed,
			                child.trashed,
			                FALSE
			            )
			        ) AS keep_trashed
			    FROM google_drive_items child
			    JOIN subtree parent
			      ON child.parent_id = parent.google_file_id
			    WHERE child.connection_id = :connectionId
			      AND child.source_id = :sourceId
			)

			UPDATE google_drive_items item
			SET
			    trashed = subtree.keep_trashed,
			    updated_at = CURRENT_TIMESTAMP
			FROM subtree
			WHERE item.id = subtree.id
			""", nativeQuery = true)
	int restoreDescendantsAfterParentRestore(@Param("connectionId") Long connectionId,

			@Param("sourceId") Long sourceId,

			@Param("rootGoogleFileId") String rootGoogleFileId);

	/*
	 * Delete the complete local subtree using one PostgreSQL recursive statement.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			WITH RECURSIVE subtree AS (

			    SELECT
			        item.id,
			        item.google_file_id
			    FROM google_drive_items item
			    WHERE item.id = :rootItemId
			      AND item.connection_id = :connectionId
			      AND item.source_id = :sourceId

			    UNION ALL

			    SELECT
			        child.id,
			        child.google_file_id
			    FROM google_drive_items child
			    JOIN subtree parent
			      ON child.parent_id = parent.google_file_id
			    WHERE child.connection_id = :connectionId
			      AND child.source_id = :sourceId
			)

			DELETE FROM google_drive_items item
			USING subtree
			WHERE item.id = subtree.id
			""", nativeQuery = true)
	int deleteLocalSubtree(@Param("rootItemId") Long rootItemId,

			@Param("connectionId") Long connectionId,

			@Param("sourceId") Long sourceId);

	@Modifying
	@Query("""
			DELETE FROM GoogleDriveItem item
			WHERE item.connection.id = :connectionId
			  AND (
			        item.syncRunId IS NULL
			        OR item.syncRunId <> :syncRunId
			  )
			""")
	int deleteStaleItems(@Param("connectionId") Long connectionId,

			@Param("syncRunId") String syncRunId);

}
