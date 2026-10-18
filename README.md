# LabTech Server

REST-сервер на Ktor (Kotlin) для учёта лабораторного оборудования: регистрация и вход по JWT,
роли `ADMIN` / `USER`, управление пользователями и каталогом оборудования. Данные хранятся в PostgreSQL (Exposed ORM).

## Запуск

Нужны JDK 21 и PostgreSQL с базой `lab_db`. Таблицы создаются автоматически при старте.

```bash
./gradlew run      # запуск сервера на http://localhost:8080
./gradlew test     # тесты (БД не нужна, используется H2 в памяти)
./gradlew build    # сборка
```

### Настройки

Значения по умолчанию лежат в `src/main/resources/application.yaml`.
Любое из них можно переопределить переменной окружения, не меняя файл:

| Переменная     | Что задаёт                | По умолчанию                               |
|----------------|---------------------------|--------------------------------------------|
| `DB_URL`       | JDBC-адрес базы           | `jdbc:postgresql://localhost:5432/lab_db`  |
| `DB_USER`      | пользователь БД           | `postgres`                                 |
| `DB_PASSWORD`  | пароль БД                 | см. `application.yaml`                     |
| `JWT_SECRET`   | секрет подписи токенов    | см. `application.yaml`                     |

> Перед публикацией или деплоем обязательно задайте свои `DB_PASSWORD` и `JWT_SECRET`.

## API

Токен, полученный в `/login`, передаётся в заголовке `Authorization: Bearer <token>`.

| Метод и путь                       | Доступ         | Описание                                          |
|------------------------------------|----------------|---------------------------------------------------|
| `GET /`                            | все            | проверка, что сервер работает                     |
| `POST /register`                   | все            | регистрация (`email`, `plainPassword`)            |
| `POST /login`                      | все            | вход, возвращает `{"token": "..."}`               |
| `GET /api/me`                      | авторизованные | профиль текущего пользователя                     |
| `GET /api/users`                   | ADMIN          | список пользователей                              |
| `POST /api/admin/delete-user/{id}` | ADMIN          | удалить пользователя (себя удалить нельзя)        |
| `GET /api/equipment`               | авторизованные | список оборудования, `?available=true` — только доступное |
| `GET /api/equipment/{id}`          | авторизованные | одна запись                                       |
| `POST /api/equipment`              | ADMIN          | добавить оборудование                             |
| `PUT /api/equipment/{id}`          | ADMIN          | изменить оборудование                             |
| `DELETE /api/equipment/{id}`       | ADMIN          | удалить оборудование                              |

Роль выдаёт сервер: **первый зарегистрированный пользователь становится `ADMIN`**, все остальные — `USER`.
Поле `role` в запросе регистрации игнорируется.

### Примеры

```bash
curl -X POST localhost:8080/register -H 'Content-Type: application/json' \
  -d '{"email":"admin@lab.com","plainPassword":"Passw0rd!x"}'

TOKEN=$(curl -s -X POST localhost:8080/login -H 'Content-Type: application/json' \
  -d '{"email":"admin@lab.com","plainPassword":"Passw0rd!x"}' | sed 's/.*"token": *"\([^"]*\)".*/\1/')

curl -X POST localhost:8080/api/equipment -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Microscope","labName":"Lab 1","hourlyRate":100}'
```

Пароль: минимум 8 символов, с заглавной и строчной буквой, цифрой и спецсимволом.
Ошибки возвращаются в виде `{"status": 400, "error": "...", "timestamp": ...}`.

## Структура

```
src/main/kotlin
├── main.kt              точка входа, сборка зависимостей
├── plagins/             настройка Ktor: JWT, JSON, обработка ошибок, роутинг
├── routes/              HTTP-эндпоинты
├── service/             бизнес-логика и валидация
├── repository/          доступ к БД (Exposed)
├── database/            таблицы и подключение
├── models/              DTO и модели
└── Exception/           прикладные исключения -> HTTP-коды
```
