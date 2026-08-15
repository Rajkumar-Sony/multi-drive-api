CREATE TABLE google_drive_change_trackers (
    id BIGSERIAL PRIMARY KEY,

    connection_id BIGINT NOT NULL,

    tracker_type VARCHAR(30) NOT NULL,

    drive_id VARCHAR(255),

    page_token TEXT,

    last_synced_at TIMESTAMP,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_google_drive_change_tracker_connection
        FOREIGN KEY (connection_id)
        REFERENCES google_drive_connections(id)
        ON DELETE CASCADE,

    CONSTRAINT ck_google_drive_change_tracker_type
        CHECK (
            (
                tracker_type = 'USER'
                AND drive_id IS NULL
            )
            OR
            (
                tracker_type = 'SHARED_DRIVE'
                AND drive_id IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uk_google_drive_change_tracker_user
    ON google_drive_change_trackers(connection_id)
    WHERE tracker_type = 'USER';

CREATE UNIQUE INDEX uk_google_drive_change_tracker_shared_drive
    ON google_drive_change_trackers(
        connection_id,
        drive_id
    )
    WHERE tracker_type = 'SHARED_DRIVE';

CREATE INDEX idx_google_drive_change_tracker_connection
    ON google_drive_change_trackers(connection_id);

CREATE INDEX idx_google_drive_change_tracker_drive
    ON google_drive_change_trackers(drive_id);


CREATE TABLE google_drive_watch_channels (
    id BIGSERIAL PRIMARY KEY,

    tracker_id BIGINT NOT NULL,

    channel_id VARCHAR(64) NOT NULL,

    channel_token VARCHAR(256) NOT NULL,

    resource_id VARCHAR(255),

    resource_uri TEXT,

    expiration TIMESTAMP NOT NULL,

    last_message_number BIGINT,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_google_drive_watch_channel_tracker
        FOREIGN KEY (tracker_id)
        REFERENCES google_drive_change_trackers(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_google_drive_watch_channel_id
        UNIQUE (channel_id)
);

CREATE INDEX idx_google_drive_watch_channel_tracker
    ON google_drive_watch_channels(tracker_id);

CREATE INDEX idx_google_drive_watch_channel_status
    ON google_drive_watch_channels(status);

CREATE INDEX idx_google_drive_watch_channel_expiration
    ON google_drive_watch_channels(expiration);