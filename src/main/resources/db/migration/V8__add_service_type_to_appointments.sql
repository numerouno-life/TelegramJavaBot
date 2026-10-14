-- V8: добавляем тип услуги в таблицу записей (nullable — бот-флоу не трогаем)
ALTER TABLE appointments ADD COLUMN service_type VARCHAR(50);
