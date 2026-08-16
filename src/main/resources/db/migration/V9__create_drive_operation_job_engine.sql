CREATE TABLE drive_operation_jobs (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    operation_type VARCHAR(30) NOT NULL,
    strategy_type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    conflict_strategy VARCHAR(30) NOT NULL DEFAULT 'KEEP_BOTH',

    source_item_id BIGINT NOT NULL,
    source_connection_id BIGINT NOT NULL,
    source_source_id BIGINT NOT NULL,
    source_google_file_id VARCHAR(255) NOT NULL,
    source_name TEXT NOT NULL,
    source_mime_type VARCHAR(255),

    destination_source_id BIGINT NOT NULL,
    destination_connection_id BIGINT NOT NULL,
    destination_parent_item_id BIGINT,
    destination_parent_google_file_id VARCHAR(255) NOT NULL,

    requested_name TEXT,
    result_item_id BIGINT,
    result_google_file_id VARCHAR(255),

    total_items BIGINT NOT NULL DEFAULT 1,
    completed_items BIGINT NOT NULL DEFAULT 0,
    failed_items BIGINT NOT NULL DEFAULT 0,
    total_bytes BIGINT,
    transferred_bytes BIGINT NOT NULL DEFAULT 0,

    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    next_attempt_at TIMESTAMP,

    cancel_requested BOOLEAN NOT NULL DEFAULT FALSE,
    idempotency_key VARCHAR(128),

    worker_id VARCHAR(64),
    lease_expires_at TIMESTAMP,
    last_heartbeat_at TIMESTAMP,

    error_code VARCHAR(100),
    error_message TEXT,

    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_drive_operation_jobs_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_drive_operation_jobs_type
        CHECK (
            operation_type IN (
                'COPY',
                'MOVE'
            )
        ),

    CONSTRAINT ck_drive_operation_jobs_status
        CHECK (
            status IN (
                'QUEUED',
                'VALIDATING',
                'PLANNING',
                'RUNNING',
                'VERIFYING',
                'COMMITTING',
                'COMPLETED',
                'PARTIAL',
                'FAILED',
                'CANCELLED',
                'CLEANUP_REQUIRED'
            )
        ),

    CONSTRAINT ck_drive_operation_jobs_conflict_strategy
        CHECK (
            conflict_strategy IN (
                'KEEP_BOTH',
                'RENAME',
                'SKIP',
                'REPLACE',
                'FAIL'
            )
        ),

    CONSTRAINT ck_drive_operation_jobs_progress
        CHECK (
            total_items >= 0
            AND completed_items >= 0
            AND failed_items >= 0
            AND transferred_bytes >= 0
        ),

    CONSTRAINT ck_drive_operation_jobs_attempts
        CHECK (
            attempt_count >= 0
            AND max_attempts >= 1
        )
);

CREATE INDEX idx_drive_operation_jobs_user_created
    ON drive_operation_jobs (
        user_id,
        created_at DESC
    );

CREATE INDEX idx_drive_operation_jobs_user_status
    ON drive_operation_jobs (
        user_id,
        status,
        created_at DESC
    );

CREATE INDEX idx_drive_operation_jobs_queue
    ON drive_operation_jobs (
        status,
        next_attempt_at,
        created_at
    );

CREATE INDEX idx_drive_operation_jobs_lease
    ON drive_operation_jobs (
        lease_expires_at
    )
    WHERE lease_expires_at IS NOT NULL;

CREATE UNIQUE INDEX uk_drive_operation_jobs_idempotency
    ON drive_operation_jobs (
        user_id,
        idempotency_key
    )
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE drive_operation_items (
    id BIGSERIAL PRIMARY KEY,

    job_id BIGINT NOT NULL,
    sequence_no INTEGER NOT NULL,

    source_local_item_id BIGINT,
    source_google_file_id VARCHAR(255) NOT NULL,
    source_name TEXT,
    source_mime_type VARCHAR(255),
    source_parent_google_file_id VARCHAR(255),
    source_path TEXT,

    destination_local_item_id BIGINT,
    destination_google_file_id VARCHAR(255),
    destination_parent_google_file_id VARCHAR(255),
    destination_path TEXT,

    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',

    size_bytes BIGINT,
    transferred_bytes BIGINT NOT NULL DEFAULT 0,
    attempt_count INTEGER NOT NULL DEFAULT 0,

    error_code VARCHAR(100),
    error_message TEXT,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_drive_operation_items_job
        FOREIGN KEY (job_id)
        REFERENCES drive_operation_jobs(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_drive_operation_items_sequence
        UNIQUE (
            job_id,
            sequence_no
        ),

    CONSTRAINT ck_drive_operation_items_status
        CHECK (
            status IN (
                'QUEUED',
                'RUNNING',
                'VERIFYING',
                'COMPLETED',
                'SKIPPED',
                'FAILED',
                'CANCELLED',
                'CLEANUP_REQUIRED'
            )
        ),

    CONSTRAINT ck_drive_operation_items_progress
        CHECK (
            transferred_bytes >= 0
            AND attempt_count >= 0
        )
);

CREATE INDEX idx_drive_operation_items_job
    ON drive_operation_items (
        job_id
    );

CREATE INDEX idx_drive_operation_items_job_status
    ON drive_operation_items (
        job_id,
        status
    );

CREATE INDEX idx_drive_operation_items_source_file
    ON drive_operation_items (
        source_google_file_id
    );
