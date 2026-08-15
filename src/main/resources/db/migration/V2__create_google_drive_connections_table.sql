CREATE TABLE google_drive_connections (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    google_subject_id VARCHAR(255) NOT NULL,

    google_email VARCHAR(255) NOT NULL,

    encrypted_access_token TEXT,

    encrypted_refresh_token TEXT,

    access_token_expiry TIMESTAMP,

    scopes TEXT,

    status VARCHAR(50) NOT NULL DEFAULT 'CONNECTED',

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_google_drive_connections_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_user_google_account
        UNIQUE (user_id, google_subject_id)
);

CREATE INDEX idx_google_drive_connections_user_id
    ON google_drive_connections(user_id);