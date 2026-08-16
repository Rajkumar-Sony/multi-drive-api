package com.multidrive.api.repository;

import com.multidrive.api.entity.DriveOperationItemStatus;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.model.DriveOperationJobExecutionSnapshot;
import com.multidrive.api.model.DriveOperationJobProgressSnapshot;
import com.multidrive.api.model.DriveOperationStrategyType;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class DriveOperationJobExecutionStore {

	private static final String CLAIM_NATIVE_MOVE_SQL = """
			WITH candidate AS (
			    SELECT id
			    FROM drive_operation_jobs
			    WHERE status = 'QUEUED'
			      AND strategy_type = 'NATIVE_MOVE'
			      AND cancel_requested = FALSE
			      AND attempt_count < max_attempts
			      AND (
			            next_attempt_at IS NULL
			            OR next_attempt_at <= ?
			      )
			    ORDER BY created_at ASC, id ASC
			    FOR UPDATE SKIP LOCKED
			    LIMIT 1
			)
			UPDATE drive_operation_jobs job
			SET
			    status = 'VALIDATING',
			    worker_id = ?,
			    lease_expires_at = ?,
			    last_heartbeat_at = ?,
			    started_at = COALESCE(started_at, ?),
			    next_attempt_at = NULL,
			    error_code = NULL,
			    error_message = NULL,
			    attempt_count = attempt_count + 1,
			    updated_at = ?,
			    version = version + 1
			FROM candidate
			WHERE job.id = candidate.id
			RETURNING job.id
			""";

	private final JdbcTemplate jdbcTemplate;

	public DriveOperationJobExecutionStore(JdbcTemplate jdbcTemplate) {

		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<Long> claimNextNativeMove(String workerId, LocalDateTime now, LocalDateTime leaseExpiresAt) {

		List<Long> result = jdbcTemplate.query(CLAIM_NATIVE_MOVE_SQL, preparedStatement -> {

			preparedStatement.setTimestamp(1, Timestamp.valueOf(now));

			preparedStatement.setString(2, workerId);

			preparedStatement.setTimestamp(3, Timestamp.valueOf(leaseExpiresAt));

			preparedStatement.setTimestamp(4, Timestamp.valueOf(now));

			preparedStatement.setTimestamp(5, Timestamp.valueOf(now));

			preparedStatement.setTimestamp(6, Timestamp.valueOf(now));
		}, (resultSet, rowNumber) -> resultSet.getLong("id"));

		if (result.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(result.getFirst());
	}

	public Optional<DriveOperationJobExecutionSnapshot> findExecutionSnapshot(Long jobId) {

		String sql = """
				SELECT
				    job.id,
				    job.user_id,
				    app_user.google_subject_id,
				    job.operation_type,
				    job.strategy_type,
				    job.status,
				    job.source_item_id,
				    job.source_connection_id,
				    job.source_source_id,
				    job.source_google_file_id,
				    job.destination_source_id,
				    job.destination_connection_id,
				    job.destination_parent_item_id,
				    job.destination_parent_google_file_id,
				    job.attempt_count,
				    job.max_attempts,
				    job.cancel_requested
				FROM drive_operation_jobs job
				JOIN users app_user
				  ON app_user.id = job.user_id
				WHERE job.id = ?
				""";

		List<DriveOperationJobExecutionSnapshot> results = jdbcTemplate.query(sql,
				(resultSet, rowNumber) -> new DriveOperationJobExecutionSnapshot(resultSet.getLong("id"),
						resultSet.getLong("user_id"), resultSet.getString("google_subject_id"),
						DriveOperationType.valueOf(resultSet.getString("operation_type")),
						DriveOperationStrategyType.valueOf(resultSet.getString("strategy_type")),
						DriveOperationJobStatus.valueOf(resultSet.getString("status")),
						resultSet.getLong("source_item_id"), resultSet.getLong("source_connection_id"),
						resultSet.getLong("source_source_id"), resultSet.getString("source_google_file_id"),
						resultSet.getLong("destination_source_id"), resultSet.getLong("destination_connection_id"),
						getNullableLong(resultSet, "destination_parent_item_id"),
						resultSet.getString("destination_parent_google_file_id"), resultSet.getInt("attempt_count"),
						resultSet.getInt("max_attempts"), resultSet.getBoolean("cancel_requested")),
				jobId);

		if (results.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(results.getFirst());
	}

	public Optional<DriveOperationJobProgressSnapshot> findProgressSnapshot(Long jobId) {

		String sql = """
				SELECT
				    job.id,
				    app_user.google_subject_id,
				    job.status,
				    job.total_items,
				    job.completed_items,
				    job.failed_items,
				    job.total_bytes,
				    job.transferred_bytes,
				    job.attempt_count,
				    job.max_attempts,
				    job.cancel_requested,
				    job.error_code,
				    job.error_message,
				    job.updated_at
				FROM drive_operation_jobs job
				JOIN users app_user
				  ON app_user.id = job.user_id
				WHERE job.id = ?
				""";

		List<DriveOperationJobProgressSnapshot> results = jdbcTemplate.query(sql,
				(resultSet, rowNumber) -> new DriveOperationJobProgressSnapshot(resultSet.getLong("id"),
						resultSet.getString("google_subject_id"),
						DriveOperationJobStatus.valueOf(resultSet.getString("status")),
						resultSet.getLong("total_items"), resultSet.getLong("completed_items"),
						resultSet.getLong("failed_items"), getNullableLong(resultSet, "total_bytes"),
						resultSet.getLong("transferred_bytes"), resultSet.getInt("attempt_count"),
						resultSet.getInt("max_attempts"), resultSet.getBoolean("cancel_requested"),
						resultSet.getString("error_code"), resultSet.getString("error_message"),
						resultSet.getTimestamp("updated_at").toLocalDateTime()),
				jobId);

		if (results.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(results.getFirst());
	}

	public boolean transition(Long jobId, String workerId, DriveOperationJobStatus status, LocalDateTime now,
			LocalDateTime leaseExpiresAt) {

		String sql = """
				UPDATE drive_operation_jobs
				SET
				    status = ?,
				    lease_expires_at = ?,
				    last_heartbeat_at = ?,
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				  AND worker_id = ?
				  AND status NOT IN (
				        'COMPLETED',
				        'PARTIAL',
				        'FAILED',
				        'CANCELLED',
				        'CLEANUP_REQUIRED'
				  )
				""";

		int updated = jdbcTemplate.update(sql, status.name(), Timestamp.valueOf(leaseExpiresAt), Timestamp.valueOf(now),
				Timestamp.valueOf(now), jobId, workerId);

		return updated == 1;
	}

	public boolean extendLease(Long jobId, String workerId, LocalDateTime now, LocalDateTime leaseExpiresAt) {

		String sql = """
				UPDATE drive_operation_jobs
				SET
				    lease_expires_at = ?,
				    last_heartbeat_at = ?,
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				  AND worker_id = ?
				  AND status IN (
				        'VALIDATING',
				        'PLANNING',
				        'RUNNING',
				        'VERIFYING',
				        'COMMITTING'
				  )
				""";

		return jdbcTemplate.update(sql, Timestamp.valueOf(leaseExpiresAt), Timestamp.valueOf(now),
				Timestamp.valueOf(now), jobId, workerId) == 1;
	}

	public boolean isCancelRequested(Long jobId) {

		List<Boolean> values = jdbcTemplate.query("""
				SELECT cancel_requested
				FROM drive_operation_jobs
				WHERE id = ?
				""", (resultSet, rowNumber) -> resultSet.getBoolean("cancel_requested"), jobId);

		return !values.isEmpty() && Boolean.TRUE.equals(values.getFirst());
	}

	public void markRootItemRunning(Long jobId) {

		jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'RUNNING',
				    attempt_count = attempt_count + 1,
				    error_code = NULL,
				    error_message = NULL,
				    updated_at = CURRENT_TIMESTAMP
				WHERE job_id = ?
				  AND sequence_no = 0
				  AND status NOT IN (
				        'COMPLETED',
				        'SKIPPED',
				        'FAILED',
				        'CANCELLED',
				        'CLEANUP_REQUIRED'
				  )
				""", jobId);
	}

	public void markRootItemVerifying(Long jobId) {

		jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'VERIFYING',
				    updated_at = CURRENT_TIMESTAMP
				WHERE job_id = ?
				  AND sequence_no = 0
				  AND status IN (
				        'QUEUED',
				        'RUNNING',
				        'VERIFYING'
				  )
				""", jobId);
	}

	@Transactional
	public void completeNativeMove(Long jobId, String workerId, Long resultItemId, String resultGoogleFileId,
			LocalDateTime now) {

		int itemUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'COMPLETED',
				    destination_local_item_id = ?,
				    destination_google_file_id = ?,
				    error_code = NULL,
				    error_message = NULL,
				    updated_at = ?
				WHERE job_id = ?
				  AND sequence_no = 0
				""", resultItemId, resultGoogleFileId, Timestamp.valueOf(now), jobId);

		if (itemUpdated != 1) {

			throw new IllegalStateException("Root operation item could not be completed");
		}

		int jobUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_jobs
				SET
				    status = 'COMPLETED',
				    result_item_id = ?,
				    result_google_file_id = ?,
				    completed_items = 1,
				    failed_items = 0,
				    completed_at = ?,
				    worker_id = NULL,
				    lease_expires_at = NULL,
				    last_heartbeat_at = NULL,
				    next_attempt_at = NULL,
				    error_code = NULL,
				    error_message = NULL,
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				  AND worker_id = ?
				  AND status = 'COMMITTING'
				""", resultItemId, resultGoogleFileId, Timestamp.valueOf(now), Timestamp.valueOf(now), jobId, workerId);

		if (jobUpdated != 1) {

			throw new IllegalStateException("Drive operation job could not be completed");
		}
	}

	@Transactional
	public boolean scheduleRetry(Long jobId, String workerId, LocalDateTime nextAttemptAt, String errorCode,
			String errorMessage, LocalDateTime now) {

		int jobUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_jobs
				SET
				    status = 'QUEUED',
				    worker_id = NULL,
				    lease_expires_at = NULL,
				    last_heartbeat_at = NULL,
				    next_attempt_at = ?,
				    error_code = ?,
				    error_message = ?,
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				  AND worker_id = ?
				  AND status IN (
				        'VALIDATING',
				        'PLANNING',
				        'RUNNING',
				        'VERIFYING',
				        'COMMITTING'
				  )
				""", Timestamp.valueOf(nextAttemptAt), errorCode, errorMessage, Timestamp.valueOf(now), jobId,
				workerId);

		if (jobUpdated != 1) {
			return false;
		}

		jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'QUEUED',
				    error_code = ?,
				    error_message = ?,
				    updated_at = ?
				WHERE job_id = ?
				  AND sequence_no = 0
				  AND status NOT IN (
				        'COMPLETED',
				        'SKIPPED',
				        'CANCELLED'
				  )
				""", errorCode, errorMessage, Timestamp.valueOf(now), jobId);

		return true;
	}

	@Transactional
	public boolean markFailure(Long jobId, String workerId, DriveOperationJobStatus terminalStatus, String errorCode,
			String errorMessage, LocalDateTime now) {

		if (terminalStatus != DriveOperationJobStatus.FAILED
				&& terminalStatus != DriveOperationJobStatus.CLEANUP_REQUIRED) {

			throw new IllegalArgumentException("Invalid terminal failure status");
		}

		int jobUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_jobs
				SET
				    status = ?,
				    failed_items = 1,
				    error_code = ?,
				    error_message = ?,
				    completed_at = ?,
				    worker_id = NULL,
				    lease_expires_at = NULL,
				    last_heartbeat_at = NULL,
				    next_attempt_at = NULL,
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				  AND worker_id = ?
				  AND status IN (
				        'VALIDATING',
				        'PLANNING',
				        'RUNNING',
				        'VERIFYING',
				        'COMMITTING'
				  )
				""", terminalStatus.name(), errorCode, errorMessage, Timestamp.valueOf(now), Timestamp.valueOf(now),
				jobId, workerId);

		if (jobUpdated != 1) {
			return false;
		}

		DriveOperationItemStatus itemStatus = terminalStatus == DriveOperationJobStatus.CLEANUP_REQUIRED
				? DriveOperationItemStatus.CLEANUP_REQUIRED : DriveOperationItemStatus.FAILED;

		jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = ?,
				    error_code = ?,
				    error_message = ?,
				    updated_at = ?
				WHERE job_id = ?
				  AND sequence_no = 0
				  AND status NOT IN (
				        'COMPLETED',
				        'SKIPPED',
				        'CANCELLED'
				  )
				""", itemStatus.name(), errorCode, errorMessage, Timestamp.valueOf(now), jobId);

		return true;
	}

	@Transactional
	public boolean cancelOwnedJob(Long jobId, String workerId, LocalDateTime now) {

		int jobUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_jobs
				SET
				    status = 'CANCELLED',
				    completed_at = ?,
				    worker_id = NULL,
				    lease_expires_at = NULL,
				    last_heartbeat_at = NULL,
				    next_attempt_at = NULL,
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				  AND worker_id = ?
				  AND cancel_requested = TRUE
				  AND status IN (
				        'VALIDATING',
				        'PLANNING',
				        'RUNNING',
				        'VERIFYING',
				        'COMMITTING'
				  )
				""", Timestamp.valueOf(now), Timestamp.valueOf(now), jobId, workerId);

		if (jobUpdated != 1) {
			return false;
		}

		jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'CANCELLED',
				    updated_at = ?
				WHERE job_id = ?
				  AND status NOT IN (
				        'COMPLETED',
				        'SKIPPED',
				        'FAILED',
				        'CLEANUP_REQUIRED'
				  )
				""", Timestamp.valueOf(now), jobId);

		return true;
	}

	@Transactional
	public boolean requestCancellation(String googleSubjectId, Long jobId, LocalDateTime now) {

		List<String> statuses = jdbcTemplate.query("""
				UPDATE drive_operation_jobs job
				SET
				    cancel_requested = TRUE,
				    status =
				        CASE
				            WHEN job.status = 'QUEUED'
				            THEN 'CANCELLED'
				            ELSE job.status
				        END,
				    completed_at =
				        CASE
				            WHEN job.status = 'QUEUED'
				            THEN ?
				            ELSE job.completed_at
				        END,
				    updated_at = ?,
				    version = version + 1
				FROM users app_user
				WHERE job.user_id = app_user.id
				  AND app_user.google_subject_id = ?
				  AND job.id = ?
				  AND job.status NOT IN (
				        'COMPLETED',
				        'PARTIAL',
				        'FAILED',
				        'CANCELLED',
				        'CLEANUP_REQUIRED'
				  )
				RETURNING job.status
				""", preparedStatement -> {

			preparedStatement.setTimestamp(1, Timestamp.valueOf(now));

			preparedStatement.setTimestamp(2, Timestamp.valueOf(now));

			preparedStatement.setString(3, googleSubjectId);

			preparedStatement.setLong(4, jobId);
		}, (resultSet, rowNumber) -> resultSet.getString("status"));

		if (statuses.isEmpty()) {
			return false;
		}

		if ("CANCELLED".equals(statuses.getFirst())) {

			jdbcTemplate.update("""
					UPDATE drive_operation_items
					SET
					    status = 'CANCELLED',
					    updated_at = ?
					WHERE job_id = ?
					  AND status NOT IN (
					        'COMPLETED',
					        'SKIPPED',
					        'FAILED',
					        'CLEANUP_REQUIRED'
					  )
					""", Timestamp.valueOf(now), jobId);
		}

		return true;
	}

	public void releaseRejectedClaim(Long jobId, String workerId, LocalDateTime retryAt, LocalDateTime now) {

		jdbcTemplate.update("""
				UPDATE drive_operation_jobs
				SET
				    status = 'QUEUED',
				    worker_id = NULL,
				    lease_expires_at = NULL,
				    last_heartbeat_at = NULL,
				    next_attempt_at = ?,
				    attempt_count =
				        GREATEST(
				            attempt_count - 1,
				            0
				        ),
				    error_code = 'WORKER_QUEUE_REJECTED',
				    error_message = 'Local worker executor queue was full',
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				  AND worker_id = ?
				  AND status = 'VALIDATING'
				""", Timestamp.valueOf(retryAt), Timestamp.valueOf(now), jobId, workerId);
	}

	@Transactional
	public int recoverExpiredLeases(LocalDateTime now, LocalDateTime retryAt) {

		String sql = """
				WITH recovered AS (
				    UPDATE drive_operation_jobs
				    SET
				        status =
				            CASE
				                WHEN cancel_requested = TRUE
				                    THEN 'CANCELLED'
				                WHEN attempt_count >= max_attempts
				                     AND status IN (
				                            'RUNNING',
				                            'VERIFYING',
				                            'COMMITTING'
				                     )
				                    THEN 'CLEANUP_REQUIRED'
				                WHEN attempt_count >= max_attempts
				                    THEN 'FAILED'
				                ELSE 'QUEUED'
				            END,
				        worker_id = NULL,
				        lease_expires_at = NULL,
				        last_heartbeat_at = NULL,
				        next_attempt_at =
				            CASE
				                WHEN cancel_requested = FALSE
				                     AND attempt_count < max_attempts
				                    THEN ?
				                ELSE NULL
				            END,
				        error_code =
				            CASE
				                WHEN cancel_requested = TRUE
				                    THEN 'CANCEL_REQUESTED'
				                ELSE 'WORKER_LEASE_EXPIRED'
				            END,
				        error_message =
				            CASE
				                WHEN cancel_requested = TRUE
				                    THEN 'Job cancellation was detected after worker lease expiry'
				                ELSE 'Worker lease expired before the job completed'
				            END,
				        completed_at =
				            CASE
				                WHEN cancel_requested = TRUE
				                     OR attempt_count >= max_attempts
				                    THEN ?
				                ELSE completed_at
				            END,
				        updated_at = ?,
				        version = version + 1
				    WHERE status IN (
				            'VALIDATING',
				            'PLANNING',
				            'RUNNING',
				            'VERIFYING',
				            'COMMITTING'
				    )
				      AND lease_expires_at IS NOT NULL
				      AND lease_expires_at < ?
				    RETURNING
				        id,
				        status
				),
				updated_items AS (
				    UPDATE drive_operation_items item
				    SET
				        status =
				            CASE recovered.status
				                WHEN 'QUEUED'
				                    THEN 'QUEUED'
				                WHEN 'CANCELLED'
				                    THEN 'CANCELLED'
				                WHEN 'CLEANUP_REQUIRED'
				                    THEN 'CLEANUP_REQUIRED'
				                ELSE 'FAILED'
				            END,
				        error_code =
				            CASE
				                WHEN recovered.status = 'CANCELLED'
				                    THEN 'CANCEL_REQUESTED'
				                ELSE 'WORKER_LEASE_EXPIRED'
				            END,
				        error_message =
				            'Operation item recovered after worker lease expiry',
				        updated_at = ?
				    FROM recovered
				    WHERE item.job_id = recovered.id
				      AND item.status IN (
				            'QUEUED',
				            'RUNNING',
				            'VERIFYING'
				      )
				    RETURNING item.id
				)
				SELECT COUNT(*) AS recovered_count
				FROM recovered
				""";

		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, Timestamp.valueOf(retryAt),
				Timestamp.valueOf(now), Timestamp.valueOf(now), Timestamp.valueOf(now), Timestamp.valueOf(now));

		return count == null ? 0 : count;
	}

	private Long getNullableLong(ResultSet resultSet, String column) throws SQLException {

		long value = resultSet.getLong(column);

		if (resultSet.wasNull()) {
			return null;
		}

		return value;
	}

}
