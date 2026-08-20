ALTER TABLE drive_operation_jobs
    ADD COLUMN planned_at TIMESTAMP;

ALTER TABLE drive_operation_items
    ADD COLUMN mutation_started BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_drive_operation_jobs_recursive_dispatch
    ON drive_operation_jobs (
        status,
        strategy_type,
        next_attempt_at,
        created_at,
        id
    )
    WHERE cancel_requested = FALSE
      AND strategy_type = 'RECURSIVE_FOLDER_COPY';

CREATE INDEX idx_drive_operation_items_job_source_parent
    ON drive_operation_items (
        job_id,
        source_parent_google_file_id
    );

CREATE INDEX idx_drive_operation_items_job_source_file
    ON drive_operation_items (
        job_id,
        source_google_file_id
    );

CREATE INDEX idx_drive_operation_items_job_ready
    ON drive_operation_items (
        job_id,
        sequence_no
    )
    WHERE status = 'QUEUED'
      AND destination_parent_google_file_id IS NOT NULL;
