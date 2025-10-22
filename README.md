# ✂️ Telegram Java Bot — Умный бот для парикмахерской на Spring Boot

[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=java&logoColor=white)](https://openjdk.org/)
[![Spring Boot 3.4.1](https://img.shields.io/badge/Spring%20Boot-3.4.1-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis&logoColor=white)](https://redis.io/)

> Бот для автоматизации записи клиентов в парикмахерскую — с админкой, уведомлениями, расписанием и геолокацией. Написан на **Java 21** с использованием **Spring Boot 3.4.1**, работает через **Long Polling**, хранит данные в **PostgreSQL**, а сессии и кэш — в **Redis**.

---

## 🌟 Возможности

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
- 🗓 **Управление расписанием**:  
  - Редактировать график на неделю  
  - Добавлять исключения (например, выходной в будний день)
- 📝 **Работа с записями**:  
  - Просмотр и отмена активных записей  
  - Записи на сегодня / завтра  
  - Ручное создание записи от имени клиента
- 👥 **Управление пользователями**:  
  - Блокировка / разблокировка  
  - Назначение новых администраторов

---

## 🛠 Технологии

- **Сборка**: Maven  
- **Основная БД**: PostgreSQL  
- **Тесты**: H2 (встроенная БД)  
- **Кэш / сессии**: Redis  
- **Режим работы**: Long Polling (без вебхуков)  
- **Язык**: Java 21  
- **Фреймворк**: Spring Boot 3.4.1  

---

## 🚀 Быстрый старт

1. Склонируйте репозиторий:
   ```bash
   git clone https://github.com/numerouno-life/TelegramJavaBot.git

Бот не доделан до своей финальной версии. Возможно в скором времени появится доп функционал
