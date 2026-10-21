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

## AI

| Variable | Default |
|---|---|
| `APP_AI_PROVIDER` | `none` |
| `APP_AI_API_KEY` | пусто |
| `APP_AI_FOLDER_ID` | пусто |
| `APP_AI_BASE_URL` | по провайдеру |
| `APP_AI_AUTH_SCHEME` | по провайдеру |
| `APP_AI_TIMEOUT_SECONDS` | `90` |
| `APP_AI_MONTHLY_TOKEN_LIMIT` | `20000000` |
| `APP_AI_MODEL_GRADING` | `aliceai-llm-flash` |
| `APP_AI_MODEL_LECTURE` | `aliceai-llm-flash` |
| `APP_AI_MODEL_PRESENTATION` | `aliceai-llm` |
| `APP_AI_MODEL_IMAGE` | `aliceai-image-art-3.0` |
| `APP_AI_GRADING_ENABLED` | `true` |
| `APP_AI_GRADING_REVIEW_THRESHOLD` | `0.7` |
| `APP_AI_OCR_PROVIDER` | пусто |
| `APP_AI_IMAGE_GENERATION_ENABLED` | `true` |
| `APP_AI_IMAGE_SEARCH_ENABLED` | `true` |
| `APP_AI_STORAGE_DIR` | `uploads/ai` |

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
