CREATE TABLE lunch_breaks (
    id BIGSERIAL PRIMARY KEY,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME,
    end_time TIME,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_lunch_breaks_dow UNIQUE (day_of_week)
);

-- Заполняем расписание обеда: Пн — выходной, остальные дни 13:00 - 14:00
INSERT INTO lunch_breaks (day_of_week, start_time, end_time, is_active) VALUES
(1, NULL, NULL, FALSE), -- Понедельник — выходной
(2, '13:00:00', '14:00:00', TRUE),
(3, '13:00:00', '14:00:00', TRUE),
(4, '13:00:00', '14:00:00', TRUE),
(5, '13:00:00', '14:00:00', TRUE),
(6, '13:00:00', '14:00:00', TRUE),
(7, '13:00:00', '14:00:00', TRUE);
