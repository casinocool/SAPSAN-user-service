-- 1. Таблица Институтов (ИВТИ, ИИТ и т.д.)
CREATE TABLE institutes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- 2. Таблица Направлений (Информатика, Прикладная математика)
CREATE TABLE directions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50), -- Например, 09.03.01
    institute_id BIGINT NOT NULL REFERENCES institutes(id) ON DELETE CASCADE
);

-- 3. Таблица Кафедр (ВТ, ИТ, ПМ)
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    direction_id BIGINT NOT NULL REFERENCES directions(id) ON DELETE CASCADE
);

-- 4. Таблица Групп (402, 101)
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    number VARCHAR(50) NOT NULL,
    department_id BIGINT NOT NULL REFERENCES departments(id) ON DELETE CASCADE
);

-- 5. Таблица Пользователей (наша локальная копия данных из Keycloak)
CREATE TABLE users (
    id UUID PRIMARY KEY, -- Внутренний ID (генерируется Java)
    keycloak_id UUID UNIQUE NOT NULL, -- ID из Keycloak (для связи)
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    middle_name VARCHAR(100),
    user_type VARCHAR(20) NOT NULL, -- STUDENT, TEACHER, ADMIN
    status VARCHAR(20) DEFAULT 'ACTIVE' -- ACTIVE, INACTIVE, GRADUATED
);

-- 6. История групп студента (для кейса бакалавриат -> магистратура)
CREATE TABLE student_group_history (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT TRUE, -- Текущая ли это группа
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Направления, которыми управляет преподаватель
CREATE TABLE teacher_direction (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    direction_id BIGINT NOT NULL REFERENCES directions(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, direction_id)
);

-- 8. ВНЕШНЯЯ ТАБЛИЦА (Имитация "чужой" БД университета)
-- Отсюда преподаватель будет "затягивать" студентов в систему
CREATE TABLE ext_university_data (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    group_number VARCHAR(50),
    department_name VARCHAR(255),
    direction_name VARCHAR(255),
    institute_name VARCHAR(255)
);

-- Наполним внешнюю БД одним тестовым студентом для проверки
INSERT INTO ext_university_data (full_name, email, group_number, department_name, direction_name, institute_name)
VALUES ('Иван Иванович Иванов', 'ivanov@sapsan.local', 'A-06-22', 'ВТ', 'ИВТ', 'ИВТИ');