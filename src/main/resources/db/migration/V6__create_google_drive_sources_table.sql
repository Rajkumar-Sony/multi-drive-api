CREATE TABLE google_drive_sources (
    id BIGSERIAL PRIMARY KEY,

    connection_id BIGINT NOT NULL,

    source_type VARCHAR(30) NOT NULL,

    google_drive_id VARCHAR(255),

    root_folder_id VARCHAR(255),

    name VARCHAR(255),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    discovery_run_id VARCHAR(36),

    last_discovered_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_google_drive_sources_connection
        FOREIGN KEY (connection_id)
        REFERENCES google_drive_connections(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_google_drive_sources_type
        CHECK (
            (
                source_type = 'MY_DRIVE'
                AND google_drive_id IS NULL
            )
            OR
            (
                source_type = 'SHARED_DRIVE'
                AND google_drive_id IS NOT NULL
            )
        ),

    CONSTRAINT ck_google_drive_sources_status
        CHECK (
            status IN (
                'ACTIVE',
                'INACTIVE'
            )
        )
);

CREATE UNIQUE INDEX uk_google_drive_source_my_drive
    ON google_drive_sources(connection_id)
    WHERE source_type = 'MY_DRIVE';

CREATE UNIQUE INDEX uk_google_drive_source_shared_drive
    ON google_drive_sources(
        connection_id,
        google_drive_id
    )
    WHERE source_type = 'SHARED_DRIVE';

CREATE INDEX idx_google_drive_sources_connection
    ON google_drive_sources(connection_id);

CREATE INDEX idx_google_drive_sources_status
    ON google_drive_sources(
        connection_id,
        status
    );

CREATE INDEX idx_google_drive_sources_drive
    ON google_drive_sources(
        google_drive_id
    )
    WHERE google_drive_id IS NOT NULL;


-- Backfill one My Drive source for every existing connection.

INSERT INTO google_drive_sources (
    connection_id,
    source_type,
    google_drive_id,
    root_folder_id,
    name,
    status,
    discovery_run_id,
    last_discovered_at,
    created_at,
    updated_at
)
SELECT
    connection.id,
    'MY_DRIVE',
    NULL,
    NULL,
    'My Drive',
    'ACTIVE',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM google_drive_connections connection;


-- Backfill Shared Drive sources from existing trackers.

INSERT INTO google_drive_sources (
    connection_id,
    source_type,
    google_drive_id,
    root_folder_id,
    name,
    status,
    discovery_run_id,
    last_discovered_at,
    created_at,
    updated_at
)
SELECT DISTINCT
    tracker.connection_id,
    'SHARED_DRIVE',
    tracker.drive_id,
    tracker.drive_id,
    NULL,
    'ACTIVE',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM google_drive_change_trackers tracker
WHERE tracker.tracker_type = 'SHARED_DRIVE'
  AND tracker.drive_id IS NOT NULL;


-- Backfill any Shared Drive sources that exist in the local
-- item index but somehow do not have a tracker.

INSERT INTO google_drive_sources (
    connection_id,
    source_type,
    google_drive_id,
    root_folder_id,
    name,
    status,
    discovery_run_id,
    last_discovered_at,
    created_at,
    updated_at
)
SELECT DISTINCT
    item.connection_id,
    'SHARED_DRIVE',
    item.drive_id,
    item.drive_id,
    NULL,
    'ACTIVE',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM google_drive_items item
WHERE item.source_type = 'SHARED_DRIVE'
  AND item.drive_id IS NOT NULL
  AND NOT EXISTS (
        SELECT 1
        FROM google_drive_sources source
        WHERE source.connection_id = item.connection_id
          AND source.source_type = 'SHARED_DRIVE'
          AND source.google_drive_id = item.drive_id
  );