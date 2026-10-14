# ✂️ Telegram Java Bot — Умный бот для парикмахерской на Spring Boot

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=java&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3.4.1](https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis&logoColor=white)](https://redis.io/)

> Бот для автоматизации записи клиентов в парикмахерскую — с админкой, уведомлениями, расписанием и геолокацией. Написан на **Java 21** с использованием **Spring Boot 3.4.1**, работает через **Long Polling**, хранит данные в **PostgreSQL**, а сессии и кэш — в **Redis**.

---

# 🌟 Возможности

### 👤 Для клиентов:
- Записаться на стрижку или отменить запись  
- Просмотр активных и прошлых записей  
- Получать уведомления:  
  - 💬 за **1 день** до визита  
  - ⏰ за **2 часа** до визита  
- Узнать местоположение парикмахерской (геолокация)

### 👨‍💼 Для администраторов:
- 📊 **Статистика в один клик**:  
  - Всего / уникальных пользователей  
  - Активные и завершённые записи  
  - Количество заблокированных
- 💰 **Управление платежами**
  - Внесение платежей за оказанные услуги
  - Привязка платежей к клиентам по номеру телефона
  - Поддержка различных услуг: стрижки, бороды, комплексные услуги
- **📊 Финансовая статистика**
  - Статистика за сегодня - почасовая детализация и последние платежи
  - Статистика за вчера - подробный отчет за предыдущий день
  - Статистика за неделю - детализация по дням с понедельника
  - Статистика за месяц - общий доход за текущий месяц
  - Произвольный период - статистика за любой выбранный период
- 🗓 **Управление расписанием**:  
  - Редактировать график на неделю  
  - Добавлять исключения (например, выходной в будний день)
  - Возможность создать график обедов на неделю
- 📝 **Работа с записями**:  
  - Просмотр и отмена активных записей  
  - Записи на сегодня / завтра  
  - Ручное создание записи от имени клиента
- 👥 **Управление пользователями**:  
  - Блокировка / разблокировка  
  - Назначение новых администраторов

---

### 🛠 Технологии

- **Сборка**: Maven  
- **Основная БД**: PostgreSQL  
- **Тесты**: H2 (встроенная БД)  
- **Кэш / сессии**: Redis  
- **Режим работы**: Long Polling (без вебхуков)  
- **Язык**: Java 21  
- **Фреймворк**: Spring Boot 3.4.1  

### 📦 Добавил защиту от флуда и спама от пользователей.
----

## ⚙️ Настройка перед запуском
1. Создайте файл `.env` в корне проекта на основе [`.env.example`](.env.example):
   ```env
   BOT_NAME=your_bot_name
   BOT_TOKEN=your_bot_token_from_botfather
   JWT_SECRET=generate_with_openssl_rand_hex_32
   MINI_APP_URL=
   MINI_APP_ORIGIN=http://localhost:18081
   ```

### 🛰 Развёртывание (Docker Compose)
Запуск всех сервисов (бэкенд, база данных, Redis, Mini App):
```bash
docker compose up -d --build
```

---

### 📱 Telegram Mini App (Локальное тестирование в браузере)
Вы можете протестировать полный флоу работы Mini App (`авторизация Telegram → выпуск JWT → запросы к REST API`) локально в браузере без Telegram и без туннелей:

1. Сгенерируйте валидную строку `initData` с помощью скрипта (передав `BOT_TOKEN` и `telegram_id`):
   ```bash
   python3 scripts/gen_init_data.py "$BOT_TOKEN" 719564670
   ```
2. Откройте полученную ссылку в браузере:
   ```
   http://localhost:18081/?devInitData=<полученная_строка>
   ```
Приложение передаст `initData` на бэкенд (`POST /api/auth/telegram`), бэкенд проверит подпись HMAC-SHA256, авторизует пользователя и выпустит JWT, после чего откроется полнофункциональный интерфейс.

---

### 🌐 Cloudflared (dev-туннель для тестирования в реальном Telegram)

Для открытия Mini App внутри реального клиента Telegram (iOS, Android, Desktop) требуется публичный HTTPS-адрес. В проект встроен бесплатный Cloudflare Quick Tunnel (без регистрации и покупки доменов):

1. **Запустите сервис туннеля:**
   ```bash
   docker compose up -d cloudflared
   ```

2. **Получите сгенерированный URL:**
   ```bash
   docker logs cloudflared 2>&1 | grep trycloudflare
   ```
   *Пример вывода: `https://xxxx-xxxx-xxxx.trycloudflare.com`*

3. **Скопируйте полученный URL в файл `.env`:**
   ```env
   MINI_APP_URL=https://xxxx-xxxx-xxxx.trycloudflare.com
   MINI_APP_ORIGIN=https://xxxx-xxxx-xxxx.trycloudflare.com
   ```
   > ⚠️ **Важно:** Значение `MINI_APP_ORIGIN` обязательно должно совпадать с `MINI_APP_URL`, иначе CORS заблокирует запросы фронтенда к бэкенду из реального Telegram!
   > ⚠️ **Внимание:** URL в режиме Quick Tunnel меняется при каждом перезапуске контейнера `cloudflared`.

4. **Перезапустите бэкенд для применения нового URL:**
   ```bash
   docker compose restart barber-server
   ```

5. **Настройте кнопку в Telegram через [@BotFather](https://t.me/BotFather):**
   - Отправьте команду `/mybots`
   - Выберите вашего бота → **Bot Settings** → **Menu Button** → **Configure menu button**
   - Отправьте полученный URL (например, `https://xxxx-xxxx-xxxx.trycloudflare.com`)
   - Укажите название кнопки (например, `Записаться`)


