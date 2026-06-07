DROP TABLE IF EXISTS lectures;

CREATE TABLE lectures (
                          id                  BIGSERIAL    PRIMARY KEY,
                          title               VARCHAR(255) NOT NULL,
                          original_file_name  VARCHAR(255) NOT NULL,
                          minio_key           VARCHAR(255) NOT NULL UNIQUE,
                          content_type        VARCHAR(255),
                          size_bytes          BIGINT       NOT NULL,
                          uploader_id         UUID         NOT NULL REFERENCES users(id)        ON DELETE CASCADE,
                          department_id       BIGINT       NOT NULL REFERENCES departments(id)  ON DELETE CASCADE,
                          created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lectures_department ON lectures (department_id);
CREATE INDEX idx_lectures_created    ON lectures (created_at DESC);