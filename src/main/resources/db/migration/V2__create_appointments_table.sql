CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    date_time TIMESTAMP NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'CONFIRMED', 'CANCELED', 'COMPLETED')),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Уникальная запись только для активной записи
CREATE UNIQUE INDEX uk_appointment_active_datetime
ON appointments(date_time, user_id)
WHERE status = 'ACTIVE';

-- Индексы для ускорения поиска
CREATE INDEX idx_appointments_datetime ON appointments(date_time);
CREATE INDEX idx_appointments_status ON appointments(status);
