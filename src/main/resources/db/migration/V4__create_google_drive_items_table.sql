CREATE TABLE google_drive_items (
    id BIGSERIAL PRIMARY KEY,

    connection_id BIGINT NOT NULL,

    google_file_id VARCHAR(255) NOT NULL,

    name TEXT NOT NULL,

    mime_type VARCHAR(255) NOT NULL,

    category VARCHAR(30) NOT NULL,

    source_type VARCHAR(30) NOT NULL,

    parent_id VARCHAR(255),

    drive_id VARCHAR(255),

    web_view_link TEXT,

    thumbnail_link TEXT,

    icon_link TEXT,

    size_bytes BIGINT,

    google_created_time TIMESTAMP WITH TIME ZONE,

    google_modified_time TIMESTAMP WITH TIME ZONE,

    trashed BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_google_drive_items_connection
        FOREIGN KEY (connection_id)
        REFERENCES google_drive_connections(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_google_drive_item
        UNIQUE (
            connection_id,
            google_file_id
        ),

    CONSTRAINT ck_google_drive_item_category
        CHECK (
            category IN (
                'FOLDER',
                'DOCUMENT',
                'IMAGE',
                'VIDEO',
                'OTHER'
            )
        ),

    CONSTRAINT ck_google_drive_item_source_type
        CHECK (
            source_type IN (
                'MY_DRIVE',
                'SHARED_DRIVE'
            )
        ),

    CONSTRAINT ck_google_drive_item_drive_id
        CHECK (
            (
                source_type = 'MY_DRIVE'
                AND drive_id IS NULL
            )
            OR
            (
                source_type = 'SHARED_DRIVE'
                AND drive_id IS NOT NULL
            )
        )
);

CREATE INDEX idx_google_drive_items_connection
    ON google_drive_items(connection_id);

CREATE INDEX idx_google_drive_items_category
    ON google_drive_items(category);

CREATE INDEX idx_google_drive_items_mime_type
    ON google_drive_items(mime_type);

CREATE INDEX idx_google_drive_items_parent
    ON google_drive_items(
        connection_id,
        parent_id
    );

CREATE INDEX idx_google_drive_items_drive
    ON google_drive_items(
        connection_id,
        drive_id
    );

CREATE INDEX idx_google_drive_items_dashboard
    ON google_drive_items(
        connection_id,
        trashed,
        google_modified_time DESC
    );

CREATE INDEX idx_google_drive_items_category_view
    ON google_drive_items(
        connection_id,
        category,
        trashed,
        google_modified_time DESC
    );