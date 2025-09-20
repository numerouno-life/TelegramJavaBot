CREATE TABLE work_schedule (
    id BIGSERIAL PRIMARY KEY,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME,
    end_time TIME,
    is_working_day BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_work_schedule_dow UNIQUE (day_of_week)
);

-- Заполняем расписание: Пн — выходной, остальные дни 10:00–20:00
INSERT INTO work_schedule (day_of_week, start_time, end_time, is_working_day) VALUES
(1, NULL, NULL, FALSE), -- Понедельник — выходной
(2, '10:00:00', '20:00:00', TRUE),
(3, '10:00:00', '20:00:00', TRUE),
(4, '10:00:00', '20:00:00', TRUE),
(5, '10:00:00', '20:00:00', TRUE),
(6, '10:00:00', '20:00:00', TRUE),
(7, '10:00:00', '20:00:00', TRUE);
