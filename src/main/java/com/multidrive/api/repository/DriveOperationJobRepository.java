package com.multidrive.api.repository;

import com.multidrive.api.dto.DriveOperationJobResponse;
import com.multidrive.api.entity.DriveOperationJob;
import com.multidrive.api.entity.DriveOperationJobStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DriveOperationJobRepository extends JpaRepository<DriveOperationJob, Long> {

	Optional<DriveOperationJob> findByIdAndUser_GoogleSubjectId(Long id, String googleSubjectId);

	@Query("""
			SELECT new com.multidrive.api.dto.DriveOperationJobResponse(
			    job.id,
			    job.operationType,
			    job.strategyType,
			    job.status,
			    job.conflictStrategy,
			    job.sourceItemId,
			    job.sourceName,
			    job.sourceMimeType,
			    job.sourceSourceId,
			    job.destinationSourceId,
			    job.destinationParentItemId,
			    job.destinationParentGoogleFileId,
			    job.requestedName,
			    job.resultItemId,
			    job.resultGoogleFileId,
			    job.totalItems,
			    job.completedItems,
			    job.failedItems,
			    job.totalBytes,
			    job.transferredBytes,
			    job.attemptCount,
			    job.maxAttempts,
			    job.cancelRequested,
			    job.errorCode,
			    job.errorMessage,
			    job.nextAttemptAt,
			    job.startedAt,
			    job.completedAt,
			    job.createdAt,
			    job.updatedAt
			)
			FROM DriveOperationJob job
			WHERE job.id = :jobId
			  AND job.user.googleSubjectId = :googleSubjectId
			""")
	Optional<DriveOperationJobResponse> findResponseByIdAndGoogleSubjectId(@Param("jobId") Long jobId,

			@Param("googleSubjectId") String googleSubjectId);

	@Query("""
			SELECT new com.multidrive.api.dto.DriveOperationJobResponse(
			    job.id,
			    job.operationType,
			    job.strategyType,
			    job.status,
			    job.conflictStrategy,
			    job.sourceItemId,
			    job.sourceName,
			    job.sourceMimeType,
			    job.sourceSourceId,
			    job.destinationSourceId,
			    job.destinationParentItemId,
			    job.destinationParentGoogleFileId,
			    job.requestedName,
			    job.resultItemId,
			    job.resultGoogleFileId,
			    job.totalItems,
			    job.completedItems,
			    job.failedItems,
			    job.totalBytes,
			    job.transferredBytes,
			    job.attemptCount,
			    job.maxAttempts,
			    job.cancelRequested,
			    job.errorCode,
			    job.errorMessage,
			    job.nextAttemptAt,
			    job.startedAt,
			    job.completedAt,
			    job.createdAt,
			    job.updatedAt
			)
			FROM DriveOperationJob job
			WHERE job.user.googleSubjectId = :googleSubjectId
			  AND job.idempotencyKey = :idempotencyKey
			""")
	Optional<DriveOperationJobResponse> findResponseByGoogleSubjectIdAndIdempotencyKey(
			@Param("googleSubjectId") String googleSubjectId,

			@Param("idempotencyKey") String idempotencyKey);

	@Query(value = """
			SELECT new com.multidrive.api.dto.DriveOperationJobResponse(
			    job.id,
			    job.operationType,
			    job.strategyType,
			    job.status,
			    job.conflictStrategy,
			    job.sourceItemId,
			    job.sourceName,
			    job.sourceMimeType,
			    job.sourceSourceId,
			    job.destinationSourceId,
			    job.destinationParentItemId,
			    job.destinationParentGoogleFileId,
			    job.requestedName,
			    job.resultItemId,
			    job.resultGoogleFileId,
			    job.totalItems,
			    job.completedItems,
			    job.failedItems,
			    job.totalBytes,
			    job.transferredBytes,
			    job.attemptCount,
			    job.maxAttempts,
			    job.cancelRequested,
			    job.errorCode,
			    job.errorMessage,
			    job.nextAttemptAt,
			    job.startedAt,
			    job.completedAt,
			    job.createdAt,
			    job.updatedAt
			)
			FROM DriveOperationJob job
			WHERE job.user.googleSubjectId = :googleSubjectId
			ORDER BY job.createdAt DESC, job.id DESC
			""", countQuery = """
			SELECT COUNT(job)
			FROM DriveOperationJob job
			WHERE job.user.googleSubjectId = :googleSubjectId
			""")
	Page<DriveOperationJobResponse> findResponsesByGoogleSubjectId(@Param("googleSubjectId") String googleSubjectId,

			Pageable pageable);

	@Query(value = """
			SELECT new com.multidrive.api.dto.DriveOperationJobResponse(
			    job.id,
			    job.operationType,
			    job.strategyType,
			    job.status,
			    job.conflictStrategy,
			    job.sourceItemId,
			    job.sourceName,
			    job.sourceMimeType,
			    job.sourceSourceId,
			    job.destinationSourceId,
			    job.destinationParentItemId,
			    job.destinationParentGoogleFileId,
			    job.requestedName,
			    job.resultItemId,
			    job.resultGoogleFileId,
			    job.totalItems,
			    job.completedItems,
			    job.failedItems,
			    job.totalBytes,
			    job.transferredBytes,
			    job.attemptCount,
			    job.maxAttempts,
			    job.cancelRequested,
			    job.errorCode,
			    job.errorMessage,
			    job.nextAttemptAt,
			    job.startedAt,
			    job.completedAt,
			    job.createdAt,
			    job.updatedAt
			)
			FROM DriveOperationJob job
			WHERE job.user.googleSubjectId = :googleSubjectId
			  AND job.status = :status
			ORDER BY job.createdAt DESC, job.id DESC
			""", countQuery = """
			SELECT COUNT(job)
			FROM DriveOperationJob job
			WHERE job.user.googleSubjectId = :googleSubjectId
			  AND job.status = :status
			""")
	Page<DriveOperationJobResponse> findResponsesByGoogleSubjectIdAndStatus(
			@Param("googleSubjectId") String googleSubjectId,

			@Param("status") DriveOperationJobStatus status,

			Pageable pageable);

}
