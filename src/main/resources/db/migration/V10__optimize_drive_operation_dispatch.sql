CREATE INDEX idx_drive_operation_jobs_native_move_dispatch
    ON drive_operation_jobs (
        status,
        strategy_type,
        next_attempt_at,
        created_at,
        id
    )
    WHERE cancel_requested = FALSE;
