package com.multidrive.api.repository;

import com.multidrive.api.entity.DriveOperationItemStatus;
import com.multidrive.api.entity.DriveOperationJobStatus;
import com.multidrive.api.entity.DriveOperationType;
import com.multidrive.api.entity.GoogleDriveSourceType;
import com.multidrive.api.model.DriveOperationItemExecutionSnapshot;
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

	private static final String CLAIM_EXECUTABLE_OPERATION_SQL = """
			WITH candidate AS (
			    SELECT id
			    FROM drive_operation_jobs
			    WHERE status = 'QUEUED'
			      AND strategy_type IN (
			            'NATIVE_MOVE',
			            'NATIVE_COPY',
			            'RECURSIVE_FOLDER_COPY'
			      )
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

	public Optional<Long> claimNextExecutableOperation(String workerId, LocalDateTime now,
			LocalDateTime leaseExpiresAt) {

		List<Long> result = jdbcTemplate.query(CLAIM_EXECUTABLE_OPERATION_SQL, preparedStatement -> {

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

	public Optional<Long> claimNextNativeOperation(String workerId, LocalDateTime now, LocalDateTime leaseExpiresAt) {

		return claimNextExecutableOperation(workerId, now, leaseExpiresAt);
	}

	public Optional<Long> claimNextNativeMove(String workerId, LocalDateTime now, LocalDateTime leaseExpiresAt) {

		return claimNextNativeOperation(workerId, now, leaseExpiresAt);
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
				    job.source_name,
				    job.source_mime_type,
				    job.destination_source_id,
				    job.destination_connection_id,
				    destination_source.source_type AS destination_source_type,
				    destination_source.google_drive_id AS destination_google_drive_id,
				    job.destination_parent_item_id,
				    job.destination_parent_google_file_id,
				    job.requested_name,
				    job.attempt_count,
				    job.max_attempts,
				    job.cancel_requested,
				    job.error_code,
				    job.created_at
				FROM drive_operation_jobs job
				JOIN users app_user
				  ON app_user.id = job.user_id
				JOIN google_drive_sources destination_source
				  ON destination_source.id = job.destination_source_id
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
						resultSet.getString("source_name"), resultSet.getString("source_mime_type"),
						resultSet.getLong("destination_source_id"), resultSet.getLong("destination_connection_id"),
						GoogleDriveSourceType.valueOf(resultSet.getString("destination_source_type")),
						resultSet.getString("destination_google_drive_id"),
						getNullableLong(resultSet, "destination_parent_item_id"),
						resultSet.getString("destination_parent_google_file_id"), resultSet.getString("requested_name"),
						resultSet.getInt("attempt_count"), resultSet.getInt("max_attempts"),
						resultSet.getBoolean("cancel_requested"), resultSet.getString("error_code"),
						resultSet.getTimestamp("created_at").toLocalDateTime()),
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

	@Transactional
	public void planRecursiveFolderCopy(Long jobId, LocalDateTime now) {

		List<Timestamp> plannedValues = jdbcTemplate.query("""
				SELECT planned_at
				FROM drive_operation_jobs
				WHERE id = ?
				  AND strategy_type = 'RECURSIVE_FOLDER_COPY'
				FOR UPDATE
				""", (resultSet, rowNumber) -> resultSet.getTimestamp("planned_at"), jobId);

		if (plannedValues.isEmpty()) {

			throw new IllegalStateException("Recursive folder copy job not found");
		}

		if (plannedValues.getFirst() != null) {

			return;
		}

		jdbcTemplate.update("""
				DELETE FROM drive_operation_items
				WHERE job_id = ?
				  AND sequence_no > 0
				""", jobId);

		int rootUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    source_path = COALESCE(source_path, source_name),
				    updated_at = ?
				WHERE job_id = ?
				  AND sequence_no = 0
				""", Timestamp.valueOf(now), jobId);

		if (rootUpdated != 1) {

			throw new IllegalStateException("Recursive folder copy root operation item is missing");
		}

		jdbcTemplate.update("""
				WITH RECURSIVE job_context AS (
				    SELECT
				        job.id AS job_id,
				        job.source_connection_id,
				        job.source_source_id,
				        job.source_google_file_id,
				        job.source_name
				    FROM drive_operation_jobs job
				    WHERE job.id = ?
				),
				tree AS (
				    SELECT
				        child.connection_id,
				        child.source_id,
				        child.id AS source_local_item_id,
				        child.google_file_id AS source_google_file_id,
				        child.name AS source_name,
				        child.mime_type AS source_mime_type,
				        child.parent_id AS source_parent_google_file_id,
				        child.size_bytes,
				        1 AS depth,
				        ARRAY[
				            context.source_google_file_id,
				            child.google_file_id
				        ]::VARCHAR[] AS visited,
				        (context.source_name || '/' || child.name)::TEXT AS source_path
				    FROM job_context context
				    JOIN google_drive_items child
				      ON child.connection_id = context.source_connection_id
				     AND child.source_id = context.source_source_id
				     AND child.parent_id = context.source_google_file_id
				    WHERE child.trashed = FALSE
				    UNION ALL
				    SELECT
				        child.connection_id,
				        child.source_id,
				        child.id,
				        child.google_file_id,
				        child.name,
				        child.mime_type,
				        child.parent_id,
				        child.size_bytes,
				        parent.depth + 1,
				        parent.visited || child.google_file_id,
				        (parent.source_path || '/' || child.name)::TEXT
				    FROM tree parent
				    JOIN google_drive_items child
				      ON child.connection_id = parent.connection_id
				     AND child.source_id = parent.source_id
				     AND child.parent_id = parent.source_google_file_id
				    WHERE child.trashed = FALSE
				      AND NOT child.google_file_id = ANY(parent.visited)
				),
				ordered_tree AS (
				    SELECT
				        tree.*,
				        ROW_NUMBER() OVER (
				            ORDER BY depth ASC, source_path ASC, source_google_file_id ASC
				        ) AS generated_sequence
				    FROM tree
				)
				INSERT INTO drive_operation_items (
				    job_id,
				    sequence_no,
				    source_local_item_id,
				    source_google_file_id,
				    source_name,
				    source_mime_type,
				    source_parent_google_file_id,
				    source_path,
				    destination_parent_google_file_id,
				    status,
				    size_bytes,
				    transferred_bytes,
				    attempt_count,
				    mutation_started,
				    created_at,
				    updated_at
				)
				SELECT
				    ?,
				    generated_sequence::INTEGER,
				    source_local_item_id,
				    source_google_file_id,
				    source_name,
				    source_mime_type,
				    source_parent_google_file_id,
				    source_path,
				    NULL,
				    'QUEUED',
				    size_bytes,
				    0,
				    0,
				    FALSE,
				    ?,
				    ?
				FROM ordered_tree
				""", jobId, jobId, Timestamp.valueOf(now), Timestamp.valueOf(now));

		jdbcTemplate.update("""
				UPDATE drive_operation_jobs job
				SET
				    total_items = stats.total_items,
				    total_bytes = stats.total_bytes,
				    planned_at = ?,
				    updated_at = ?,
				    version = version + 1
				FROM (
				    SELECT
				        COUNT(*)::BIGINT AS total_items,
				        COALESCE(SUM(COALESCE(size_bytes, 0)), 0)::BIGINT AS total_bytes
				    FROM drive_operation_items
				    WHERE job_id = ?
				) stats
				WHERE job.id = ?
				""", Timestamp.valueOf(now), Timestamp.valueOf(now), jobId, jobId);
	}

	public List<DriveOperationItemExecutionSnapshot> findReadyOperationItems(Long jobId, int limit) {

		if (limit < 1) {

			throw new IllegalArgumentException("limit must be greater than 0");
		}

		return jdbcTemplate.query("""
				SELECT
				    id,
				    sequence_no,
				    source_local_item_id,
				    source_google_file_id,
				    source_name,
				    source_mime_type,
				    source_parent_google_file_id,
				    source_path,
				    destination_parent_google_file_id,
				    destination_local_item_id,
				    destination_google_file_id,
				    size_bytes,
				    attempt_count,
				    error_code,
				    mutation_started
				FROM drive_operation_items
				WHERE job_id = ?
				  AND status = 'QUEUED'
				  AND destination_parent_google_file_id IS NOT NULL
				ORDER BY sequence_no ASC
				LIMIT ?
				""", (resultSet, rowNumber) -> mapOperationItem(resultSet), jobId, limit);
	}

	public Optional<DriveOperationItemExecutionSnapshot> findRootOperationItem(Long jobId) {

		List<DriveOperationItemExecutionSnapshot> results = jdbcTemplate.query("""
				SELECT
				    id,
				    sequence_no,
				    source_local_item_id,
				    source_google_file_id,
				    source_name,
				    source_mime_type,
				    source_parent_google_file_id,
				    source_path,
				    destination_parent_google_file_id,
				    destination_local_item_id,
				    destination_google_file_id,
				    size_bytes,
				    attempt_count,
				    error_code,
				    mutation_started
				FROM drive_operation_items
				WHERE job_id = ?
				  AND sequence_no = 0
				""", (resultSet, rowNumber) -> mapOperationItem(resultSet), jobId);

		if (results.isEmpty()) {

			return Optional.empty();
		}

		return Optional.of(results.getFirst());
	}

	public long countIncompleteOperationItems(Long jobId) {

		Long count = jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM drive_operation_items
				WHERE job_id = ?
				  AND status <> 'COMPLETED'
				""", Long.class, jobId);

		return count == null ? 0 : count;
	}

	public boolean markOperationItemRunning(Long jobId, Long itemId, LocalDateTime now) {

		return jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'RUNNING',
				    attempt_count = attempt_count + 1,
				    error_code = NULL,
				    error_message = NULL,
				    updated_at = ?
				WHERE id = ?
				  AND job_id = ?
				  AND status = 'QUEUED'
				""", Timestamp.valueOf(now), itemId, jobId) == 1;
	}

	public boolean markOperationItemMutationStarted(Long jobId, Long itemId, LocalDateTime now) {

		return jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    mutation_started = TRUE,
				    updated_at = ?
				WHERE id = ?
				  AND job_id = ?
				  AND status = 'RUNNING'
				""", Timestamp.valueOf(now), itemId, jobId) == 1;
	}

	public boolean markOperationItemVerifying(Long jobId, Long itemId, LocalDateTime now) {

		return jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'VERIFYING',
				    updated_at = ?
				WHERE id = ?
				  AND job_id = ?
				  AND status = 'RUNNING'
				""", Timestamp.valueOf(now), itemId, jobId) == 1;
	}

	@Transactional
	public void completeRecursiveOperationItem(Long jobId, Long itemId, String sourceGoogleFileId,
			Long destinationLocalItemId, String destinationGoogleFileId, boolean folder, LocalDateTime now) {

		List<Long> logicalBytes = jdbcTemplate.query("""
				UPDATE drive_operation_items
				SET
				    status = 'COMPLETED',
				    destination_local_item_id = ?,
				    destination_google_file_id = ?,
				    transferred_bytes = COALESCE(size_bytes, 0),
				    error_code = NULL,
				    error_message = NULL,
				    updated_at = ?
				WHERE id = ?
				  AND job_id = ?
				  AND status IN (
				        'RUNNING',
				        'VERIFYING'
				  )
				RETURNING COALESCE(size_bytes, 0)::BIGINT AS logical_bytes
				""", preparedStatement -> {

			if (destinationLocalItemId == null) {
				preparedStatement.setNull(1, java.sql.Types.BIGINT);
			}
			else {
				preparedStatement.setLong(1, destinationLocalItemId);
			}

			preparedStatement.setString(2, destinationGoogleFileId);
			preparedStatement.setTimestamp(3, Timestamp.valueOf(now));
			preparedStatement.setLong(4, itemId);
			preparedStatement.setLong(5, jobId);
		}, (resultSet, rowNumber) -> resultSet.getLong("logical_bytes"));

		if (logicalBytes.size() != 1) {

			throw new IllegalStateException("Recursive operation item could not be completed");
		}

		jdbcTemplate.update("""
				UPDATE drive_operation_jobs
				SET
				    completed_items = completed_items + 1,
				    transferred_bytes = transferred_bytes + ?,
				    updated_at = ?,
				    version = version + 1
				WHERE id = ?
				""", logicalBytes.getFirst(), Timestamp.valueOf(now), jobId);

		if (folder) {

			jdbcTemplate.update("""
					UPDATE drive_operation_items
					SET
					    destination_parent_google_file_id = ?,
					    updated_at = ?
					WHERE job_id = ?
					  AND source_parent_google_file_id = ?
					  AND destination_parent_google_file_id IS NULL
					  AND status = 'QUEUED'
					""", destinationGoogleFileId, Timestamp.valueOf(now), jobId, sourceGoogleFileId);
		}
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

		completeSingleItemJob(jobId, workerId, resultItemId, resultGoogleFileId, now);
	}

	@Transactional
	public void completeNativeCopy(Long jobId, String workerId, Long resultItemId, String resultGoogleFileId,
			LocalDateTime now) {

		completeSingleItemJob(jobId, workerId, resultItemId, resultGoogleFileId, now);
	}

	@Transactional
	public void completeRecursiveCopy(Long jobId, String workerId, Long resultItemId, String resultGoogleFileId,
			LocalDateTime now) {

		int updated = jdbcTemplate.update("""
				UPDATE drive_operation_jobs job
				SET
				    status = 'COMPLETED',
				    result_item_id = ?,
				    result_google_file_id = ?,
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
				WHERE job.id = ?
				  AND job.worker_id = ?
				  AND job.status = 'COMMITTING'
				  AND NOT EXISTS (
				        SELECT 1
				        FROM drive_operation_items item
				        WHERE item.job_id = job.id
				          AND item.status <> 'COMPLETED'
				  )
				""", resultItemId, resultGoogleFileId, Timestamp.valueOf(now), Timestamp.valueOf(now), jobId, workerId);

		if (updated != 1) {

			throw new IllegalStateException("Recursive folder copy job could not be completed");
		}
	}

	private void completeSingleItemJob(Long jobId, String workerId, Long resultItemId, String resultGoogleFileId,
			LocalDateTime now) {

		int itemUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'COMPLETED',
				    destination_local_item_id = ?,
				    destination_google_file_id = ?,
				    error_code = NULL,
				    error_message = NULL,
				    transferred_bytes = COALESCE(size_bytes, transferred_bytes),
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
				    transferred_bytes = COALESCE(total_bytes, transferred_bytes),
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
				  AND status IN (
				        'RUNNING',
				        'VERIFYING'
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
				    status =
				        CASE
				            WHEN ? = 'FAILED'
				                 AND completed_items > 0
				            THEN 'PARTIAL'
				            ELSE ?
				        END,
				    failed_items = GREATEST(failed_items, 1),
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
				""", terminalStatus.name(), terminalStatus.name(), errorCode, errorMessage, Timestamp.valueOf(now),
				Timestamp.valueOf(now), jobId, workerId);

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
				  AND status IN (
				        'RUNNING',
				        'VERIFYING'
				  )
				""", itemStatus.name(), errorCode, errorMessage, Timestamp.valueOf(now), jobId);

		jdbcTemplate.update("""
				UPDATE drive_operation_items
				SET
				    status = 'CANCELLED',
				    updated_at = ?
				WHERE job_id = ?
				  AND status = 'QUEUED'
				""", Timestamp.valueOf(now), jobId);

		return true;
	}

	@Transactional
	public boolean cancelOwnedJob(Long jobId, String workerId, LocalDateTime now) {

		int jobUpdated = jdbcTemplate.update("""
				UPDATE drive_operation_jobs
				SET
				    status =
				        CASE
				            WHEN completed_items > 0
				            THEN 'PARTIAL'
				            ELSE 'CANCELLED'
				        END,
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

	private DriveOperationItemExecutionSnapshot mapOperationItem(ResultSet resultSet) throws SQLException {

		return new DriveOperationItemExecutionSnapshot(resultSet.getLong("id"), resultSet.getInt("sequence_no"),
				getNullableLong(resultSet, "source_local_item_id"), resultSet.getString("source_google_file_id"),
				resultSet.getString("source_name"), resultSet.getString("source_mime_type"),
				resultSet.getString("source_parent_google_file_id"), resultSet.getString("source_path"),
				resultSet.getString("destination_parent_google_file_id"),
				getNullableLong(resultSet, "destination_local_item_id"),
				resultSet.getString("destination_google_file_id"), getNullableLong(resultSet, "size_bytes"),
				resultSet.getInt("attempt_count"), resultSet.getString("error_code"),
				resultSet.getBoolean("mutation_started"));
	}

	private Long getNullableLong(ResultSet resultSet, String column) throws SQLException {

		long value = resultSet.getLong(column);

		if (resultSet.wasNull()) {
			return null;
		}

		return value;
	}

}
