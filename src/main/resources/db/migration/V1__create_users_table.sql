CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    telegram_id BIGINT,
    username VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    client_phone_number VARCHAR(50),
    role VARCHAR(20) DEFAULT 'USER',
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);
