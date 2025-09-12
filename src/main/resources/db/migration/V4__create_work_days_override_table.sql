CREATE TABLE work_days_override (
    id BIGSERIAL PRIMARY KEY,
    date DATE UNIQUE NOT NULL,
    start_time TIME,
    end_time TIME,
    is_working_day BOOLEAN DEFAULT TRUE,
    reason VARCHAR (255)
);