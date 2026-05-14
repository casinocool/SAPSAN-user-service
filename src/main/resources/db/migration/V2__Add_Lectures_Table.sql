CREATE TABLE lectures (
                          id BIGSERIAL PRIMARY KEY,
                          title VARCHAR(255) NOT NULL,
                          original_file_name VARCHAR(255) NOT NULL,
                          minio_key VARCHAR(255) NOT NULL,
                          uploader_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)