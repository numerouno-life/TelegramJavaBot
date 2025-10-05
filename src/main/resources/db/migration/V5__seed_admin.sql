-- Сначала проверяем существует ли уже админ
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE telegram_id = 719564670) THEN
        INSERT INTO users (telegram_id, username, first_name, last_name, role, is_blocked, created_at)
        VALUES (719564670, 'numerouno_life', 'Александр', NULL, 'ADMIN', FALSE, now());
    ELSE
        UPDATE users 
        SET role = 'ADMIN', is_blocked = FALSE, username = 'numerouno_life', first_name = 'Александр'
        WHERE telegram_id = 719564670;
    END IF;
END $$;