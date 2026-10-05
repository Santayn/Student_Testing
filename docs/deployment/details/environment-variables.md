# Переменные окружения

## Единый источник конфигурации

В проекте используется один канонический шаблон:

```text
/.env.example
```

и один локальный файл конкретной машины:

```text
/.env
```

Отдельные `frontend/.env*` и `backend/.env*` не используются. Реальный `.env` рекомендуется держать минимальным: только секреты и значения, которые отличаются на конкретной машине. Неизменённые параметры берутся из defaults `docker-compose.yml`, Spring/Vite/Nginx.

## Обязательные локальные секреты

| Переменная | Назначение | Обязательность |
|---|---|---|
| `POSTGRES_PASSWORD` | пароль PostgreSQL | обязательна для Compose |
| `APP_JWT_SECRET` | подпись JWT | обязательна, минимум 32 символа |

## Локальный host override

| Переменная | Default | Назначение |
|---|---|---|
| `BACKEND_PORT` | `8080` | host-порт backend и target Vite dev proxy |

Внутри Docker backend по-прежнему слушает `8080`. Например `BACKEND_PORT=8081` означает `127.0.0.1:8081 -> backend:8080`, а локальный `npm run dev` проксирует `/api` на `127.0.0.1:8081`.

## PostgreSQL

| Переменная | Default | Назначение |
|---|---|---|
| `POSTGRES_DB` | `student_test` | имя БД |
| `POSTGRES_USER` | `student_test` | пользователь БД |
| `POSTGRES_PASSWORD` | — | пароль БД |

## Backend / Spring

| Переменная | Docker default | Назначение |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` | активный Spring profile |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Hibernate schema management |
| `SPRING_JPA_SHOW_SQL` | `false` | SQL logging |
| `SPRING_SQL_INIT_MODE` | `always` | выполнение `schema.sql` |
| `SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE` | `50MB` | лимит одного файла |
| `SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE` | `200MB` | лимит multipart request |
| `APP_DATA_LOADER_ENABLED` | `true` | demo/system seed |
| `APP_PUBLIC_REGISTRATION_ENABLED` | `false` | единый backend/frontend flag регистрации |
| `APP_CORS_ALLOWED_ORIGINS` | localhost patterns | CORS allowlist |

## Backup

| Переменная | Default | Назначение |
|---|---|---|
| `APP_DATABASE_BACKUP_PG_DUMP_PATH` | `pg_dump` | путь к `pg_dump` |
| `APP_DATABASE_BACKUP_PSQL_PATH` | `psql` | путь к `psql` |
| `APP_DATABASE_BACKUP_SCHEMA` | пусто | ограничение схемой |
| `APP_DATABASE_BACKUP_INCLUDE_CLEAN` | `true` | clean statements |

## Local LLM

| Переменная | Default | Назначение |
|---|---|---|
| `APP_TEXT_ANSWER_LOCAL_LLM_ENABLED` | `false` | включить evaluator |
| `APP_TEXT_ANSWER_LOCAL_LLM_ENDPOINT` | `http://127.0.0.1:11434/api/generate` | endpoint |
| `APP_TEXT_ANSWER_LOCAL_LLM_MODEL` | `qwen2.5:1.5b-instruct` | модель |
| `APP_TEXT_ANSWER_LOCAL_LLM_TIMEOUT_SECONDS` | `12` | timeout |

Эти параметры теперь явно пробрасываются Compose в backend container.

## Frontend build/dev (`VITE_*`)

| Переменная | Default | Назначение |
|---|---|---|
| `VITE_API_BASE_URL` | `/api/v1` | API base path |
| `VITE_API_TIMEOUT_MS` | `15000` | обычные API requests |
| `VITE_SUBMIT_TIMEOUT_MS` | `180000` | submit теста |
| `VITE_FILE_TRANSFER_TIMEOUT_MS` | `0` | upload/download timeout |

Vite читает эти значения из корневого `.env`. При Docker build Compose передаёт их как build args. Изменение `VITE_*` в production требует rebuild frontend image.

`APP_PUBLIC_REGISTRATION_ENABLED` не дублируется отдельной `VITE_PUBLIC_REGISTRATION_ENABLED`: Vite конфигурация явно публикует только этот конкретный feature flag через `define`.

## Nginx runtime

| Переменная | Default | Назначение |
|---|---|---|
| `FRONTEND_PROXY_SEND_TIMEOUT` | `300s` | proxy send timeout |
| `FRONTEND_PROXY_READ_TIMEOUT` | `300s` | proxy read timeout |

Docker frontend продолжает обращаться к backend по внутреннему адресу `backend:8080`; `BACKEND_PORT` влияет только на host mapping и Vite dev proxy.
