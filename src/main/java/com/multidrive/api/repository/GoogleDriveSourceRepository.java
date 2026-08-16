package com.multidrive.api.repository;

import com.multidrive.api.dto.GoogleDriveSourceResponse;
import com.multidrive.api.entity.GoogleDriveSource;
import com.multidrive.api.entity.GoogleDriveSourceStatus;
import com.multidrive.api.entity.GoogleDriveSourceType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GoogleDriveSourceRepository extends JpaRepository<GoogleDriveSource, Long> {

	List<GoogleDriveSource> findAllByConnection_Id(Long connectionId);

	List<GoogleDriveSource> findAllByConnection_IdAndStatusOrderBySourceTypeAscNameAsc(Long connectionId,
			GoogleDriveSourceStatus status);

	List<GoogleDriveSource> findAllByConnection_IdAndSourceTypeAndStatusOrderByNameAsc(Long connectionId,
			GoogleDriveSourceType sourceType, GoogleDriveSourceStatus status);

	Optional<GoogleDriveSource> findByConnection_IdAndSourceTypeAndGoogleDriveIdIsNull(Long connectionId,
			GoogleDriveSourceType sourceType);

	Optional<GoogleDriveSource> findByConnection_IdAndSourceTypeAndGoogleDriveId(Long connectionId,
			GoogleDriveSourceType sourceType, String googleDriveId);

	/*
	 * Single-operation lookup.
	 *
	 * Source + connection + application owner are fetched in one SQL query.
	 *
	 * This avoids lazy-loading extra queries during create-folder operations.
	 */
	@Query("""
			SELECT source
			FROM GoogleDriveSource source
			JOIN FETCH source.connection connection
			JOIN FETCH connection.user owner
			WHERE source.id = :sourceId
			  AND source.status = :status
			  AND owner.googleSubjectId = :googleSubjectId
			""")
	Optional<GoogleDriveSource> findOwnedSourceForOperation(@Param("sourceId") Long sourceId,

			@Param("googleSubjectId") String googleSubjectId,

			@Param("status") GoogleDriveSourceStatus status);

	@Query("""
			SELECT new com.multidrive.api.dto.GoogleDriveSourceResponse(
			    source.id,
			    source.connection.id,
			    source.connection.googleEmail,
			    source.sourceType,
			    source.googleDriveId,
			    source.rootFolderId,
			    source.name,
			    source.status,
			    source.lastDiscoveredAt
			)
			FROM GoogleDriveSource source
			WHERE source.connection.user.id = :userId
			  AND source.status = :status
			ORDER BY
			    source.connection.id ASC,
			    source.sourceType ASC,
			    source.name ASC
			""")
	List<GoogleDriveSourceResponse> findSourceResponsesByUserIdAndStatus(@Param("userId") Long userId,

			@Param("status") GoogleDriveSourceStatus status);

	@Query("""
			SELECT new com.multidrive.api.dto.GoogleDriveSourceResponse(
			    source.id,
			    source.connection.id,
			    source.connection.googleEmail,
			    source.sourceType,
			    source.googleDriveId,
			    source.rootFolderId,
			    source.name,
			    source.status,
			    source.lastDiscoveredAt
			)
			FROM GoogleDriveSource source
			WHERE source.connection.user.id = :userId
			  AND source.connection.id = :connectionId
			  AND source.status = :status
			ORDER BY
			    source.sourceType ASC,
			    source.name ASC
			""")
	List<GoogleDriveSourceResponse> findSourceResponsesByUserIdAndConnectionIdAndStatus(@Param("userId") Long userId,

			@Param("connectionId") Long connectionId,

			@Param("status") GoogleDriveSourceStatus status);

	@Modifying
	@Query("""
			UPDATE GoogleDriveSource source
			SET source.status = :inactiveStatus,
			    source.updatedAt = :updatedAt
			WHERE source.connection.id = :connectionId
			  AND source.sourceType = :sourceType
			  AND source.status = :activeStatus
			  AND (
			        source.discoveryRunId IS NULL
			        OR source.discoveryRunId <> :discoveryRunId
			  )
			""")
	int markUndiscoveredSharedDriveSourcesInactive(@Param("connectionId") Long connectionId,

			@Param("sourceType") GoogleDriveSourceType sourceType,

			@Param("activeStatus") GoogleDriveSourceStatus activeStatus,

			@Param("inactiveStatus") GoogleDriveSourceStatus inactiveStatus,

			@Param("discoveryRunId") String discoveryRunId,

			@Param("updatedAt") LocalDateTime updatedAt);

}