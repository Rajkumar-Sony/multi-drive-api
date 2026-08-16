ALTER TABLE google_drive_items
    ADD COLUMN sync_run_id VARCHAR(36);

CREATE INDEX idx_google_drive_items_sync_run
    ON google_drive_items(
        connection_id,
        sync_run_id
    );