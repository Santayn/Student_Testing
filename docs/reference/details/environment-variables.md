# Переменные окружения

Переменные разделены по месту применения. Наличие переменной в backend `application.yml` не означает, что текущий `docker-compose.yml` автоматически пробрасывает её из `.env`.

## PostgreSQL / Compose

| Variable | Назначение | Default/примечание |
|---|---|---|
| `POSTGRES_DB` | Имя БД | `student_test` |
| `POSTGRES_USER` | Пользователь БД | `student_test` |
| `POSTGRES_PASSWORD` | Пароль БД | обязателен в Compose |
| `BACKEND_PORT` | Host-порт backend | `8080` |

## Spring datasource/runtime

| Variable | Назначение | Default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile | Compose: `local` |
| `SPRING_DATASOURCE_URL` | JDBC URL | обязателен вне Compose wiring |
| `SPRING_DATASOURCE_USERNAME` | DB user | — |
| `SPRING_DATASOURCE_PASSWORD` | DB password | — |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema mode | app default `validate`, Compose `update` |
| `SPRING_JPA_SHOW_SQL` | SQL logging | `false` |
| `SPRING_SQL_INIT_MODE` | `schema.sql` init mode | app default `never`, Compose `always` |
| `SERVER_PORT` | Spring Boot port | `8080` |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | Max одного файла | `50MB` |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | Max request | `200MB` |

`SPRING_JPA_DATABASE_PLATFORM` также задаётся Compose напрямую как PostgreSQL dialect.

## Application

| Variable | Назначение | Default |
|---|---|---|
| `APP_DATA_LOADER_ENABLED` | Seed/demo loader | app `false`, Compose `true` |
| `APP_PUBLIC_REGISTRATION_ENABLED` | Публичная регистрация | `false` |
| `APP_JWT_SECRET` | JWT secret | обязателен; минимум 32 символа по конфигурации приложения |
| `APP_CORS_ALLOWED_ORIGINS` | Allowed CORS origins | localhost patterns |
| `APP_STORAGE_LECTURE_MATERIALS_DIR` | Каталог материалов | `uploads/lecture-materials` |

## Backup

| Variable | Назначение | Default |
|---|---|---|
| `APP_DATABASE_BACKUP_PG_DUMP_PATH` | Путь к `pg_dump` | `pg_dump` |
| `APP_DATABASE_BACKUP_PSQL_PATH` | Путь к `psql` | `psql` |
| `APP_DATABASE_BACKUP_SCHEMA` | Ограничение схемой | пусто |
| `APP_DATABASE_BACKUP_INCLUDE_CLEAN` | Добавлять clean statements | `true` |

## Local LLM

| Variable | Назначение | Default |
|---|---|---|
| `APP_TEXT_ANSWER_LOCAL_LLM_ENABLED` | Включить LLM evaluator | `false` |
| `APP_TEXT_ANSWER_LOCAL_LLM_ENDPOINT` | Endpoint | `http://127.0.0.1:11434/api/generate` |
| `APP_TEXT_ANSWER_LOCAL_LLM_MODEL` | Модель | `qwen2.5:1.5b-instruct` |
| `APP_TEXT_ANSWER_LOCAL_LLM_TIMEOUT_SECONDS` | Timeout | `12` |

## Frontend build-time (`VITE_*`)

| Variable | Назначение | Default |
|---|---|---|
| `VITE_API_BASE_URL` | API base path | `.env.example`: `/api/v1`; production code имеет same-origin fallback |
| `VITE_PUBLIC_REGISTRATION_ENABLED` | UI регистрации | `false` |
| `VITE_API_TIMEOUT_MS` | Обычные JSON requests | `15000` |
| `VITE_SUBMIT_TIMEOUT_MS` | Submit теста | `180000` |
| `VITE_FILE_TRANSFER_TIMEOUT_MS` | Upload/download timeout | `0` |

`VITE_*` встраиваются при `npm run build`; для изменения production frontend требуется rebuild.

## Frontend Nginx runtime

Compose-level variables:

| Variable | Назначение | Default |
|---|---|---|
| `FRONTEND_PROXY_SEND_TIMEOUT` | Proxy send timeout | `300s` |
| `FRONTEND_PROXY_READ_TIMEOUT` | Proxy read timeout | `300s` |

Внутри frontend-container они преобразуются в:

```text
BACKEND_HOST
BACKEND_PORT
PROXY_SEND_TIMEOUT
PROXY_READ_TIMEOUT
```

Подробнее: [Deployment: environment variables](../../deployment/details/environment-variables.md).
