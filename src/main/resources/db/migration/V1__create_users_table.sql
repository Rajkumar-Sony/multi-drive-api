CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    google_subject_id VARCHAR(255) NOT NULL,

    email VARCHAR(255) NOT NULL,

    name VARCHAR(255),

    picture_url TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT uk_users_google_subject_id
        UNIQUE (google_subject_id)
);