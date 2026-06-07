-- В V2 таблица lectures была привязана к group_id. Меняем модель:
-- лекция теперь привязана к направлению (direction), её видят все студенты
-- этого направления независимо от группы.
--
-- Данных в lectures пока нет, поэтому простой DROP/CREATE без миграции значений.

DROP TABLE IF EXISTS lectures;

CREATE TABLE lectures (
    id                  BIGSERIAL    PRIMARY KEY,
    title               VARCHAR(255) NOT NULL,
    original_file_name  VARCHAR(255) NOT NULL,
    minio_key           VARCHAR(255) NOT NULL UNIQUE,
    content_type        VARCHAR(255),
    size_bytes          BIGINT       NOT NULL,
    uploader_id         UUID         NOT NULL REFERENCES users(id)      ON DELETE CASCADE,
    direction_id        BIGINT       NOT NULL REFERENCES directions(id) ON DELETE CASCADE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lectures_direction ON lectures (direction_id);
CREATE INDEX idx_lectures_created   ON lectures (created_at DESC);
