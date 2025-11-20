create TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON delete SET NULL, -- Может быть NULL для клиентов без аккаунта
    client_phone_number VARCHAR(20), -- Дублируем для поиска без JOIN
    client_name VARCHAR(255),
    amount DECIMAL(10,2) NOT NULL,
    service_type VARCHAR(100),
    service_date TIMESTAMP NOT NULL, -- Дата оказания услуги
    payment_date TIMESTAMP, -- Фактическое время оплаты
    created_by BIGINT NOT NULL, -- ID администратора, который внёс платёж
    comment TEXT
);

-- Индексы для ускорения поиска
create index idx_payments_service_date on payments(service_date);
create index idx_payments_client_phone on payments(client_phone_number);
create index idx_payments_user_id on payments(user_id);