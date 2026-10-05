# Переменные окружения

## Канонические файлы

```text
/.env.example  — полный шаблон/справочник
/.env          — секреты и локальные overrides конкретной машины
```

Отдельные env-файлы frontend/backend не используются.

## PostgreSQL / host

| Variable | Назначение | Default/примечание |
|---|---|---|
| `POSTGRES_DB` | Имя БД | `student_test` |
| `POSTGRES_USER` | Пользователь БД | `student_test` |
| `POSTGRES_PASSWORD` | Пароль БД | обязателен в Compose |
| `BACKEND_PORT` | Host backend + Vite proxy target | `8080` |

## Spring/backend

| Variable | Назначение | Docker default |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `local` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Hibernate schema mode | `update` |
| `SPRING_JPA_SHOW_SQL` | SQL logging | `false` |
| `SPRING_SQL_INIT_MODE` | `schema.sql` init | `always` |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | Max одного файла | `50MB` |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | Max request | `200MB` |
| `APP_DATA_LOADER_ENABLED` | Seed/demo loader | `true` |
| `APP_PUBLIC_REGISTRATION_ENABLED` | Backend + frontend registration flag | `false` |
| `APP_JWT_SECRET` | JWT secret | обязателен, >=32 символов |
| `APP_CORS_ALLOWED_ORIGINS` | CORS origins | localhost patterns |

`SERVER_PORT=8080`, datasource URL/user/password и lecture storage path задаются Compose внутренней конфигурацией контейнера. При прямом запуске backend их нужно передавать процессу отдельно.

## Backup

| Variable | Default |
|---|---|
| `APP_DATABASE_BACKUP_PG_DUMP_PATH` | `pg_dump` |
| `APP_DATABASE_BACKUP_PSQL_PATH` | `psql` |
| `APP_DATABASE_BACKUP_SCHEMA` | пусто |
| `APP_DATABASE_BACKUP_INCLUDE_CLEAN` | `true` |

## Local LLM

| Variable | Default |
|---|---|
| `APP_TEXT_ANSWER_LOCAL_LLM_ENABLED` | `false` |
| `APP_TEXT_ANSWER_LOCAL_LLM_ENDPOINT` | `http://127.0.0.1:11434/api/generate` |
| `APP_TEXT_ANSWER_LOCAL_LLM_MODEL` | `qwen2.5:1.5b-instruct` |
| `APP_TEXT_ANSWER_LOCAL_LLM_TIMEOUT_SECONDS` | `12` |

## Frontend build/dev

| Variable | Назначение | Default |
|---|---|---|
| `VITE_API_BASE_URL` | API base path | `/api/v1` |
| `VITE_API_TIMEOUT_MS` | JSON timeout | `15000` |
| `VITE_SUBMIT_TIMEOUT_MS` | Submit timeout | `180000` |
| `VITE_FILE_TRANSFER_TIMEOUT_MS` | File transfer timeout | `0` |
| `APP_PUBLIC_REGISTRATION_ENABLED` | UI регистрации | тот же canonical flag backend |

Vite читает эти значения из корневого `.env`. Отдельного `VITE_PUBLIC_REGISTRATION_ENABLED` больше нет.

## Frontend Nginx runtime

| Variable | Default |
|---|---|
| `FRONTEND_PROXY_SEND_TIMEOUT` | `300s` |
| `FRONTEND_PROXY_READ_TIMEOUT` | `300s` |

Подробнее: [Deployment: environment variables](../../deployment/details/environment-variables.md).
