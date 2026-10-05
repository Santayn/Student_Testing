# Переменные окружения

## Обязательные секреты Compose

| Переменная | Назначение | Обязательность |
|---|---|---|
| `POSTGRES_PASSWORD` | пароль PostgreSQL | обязательна |
| `APP_JWT_SECRET` | подпись JWT | обязательна, минимум 32 символа |

`docker-compose.yml` использует required interpolation, поэтому без этих значений стандартный Compose не должен запускаться.

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
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | стратегия Hibernate schema management |
| `SPRING_SQL_INIT_MODE` | `always` | выполнение `schema.sql` |
| `APP_DATA_LOADER_ENABLED` | `true` | demo/system seed через DataLoader |
| `APP_PUBLIC_REGISTRATION_ENABLED` | `false` | публичная регистрация |
| `APP_CORS_ALLOWED_ORIGINS` | localhost origins | CORS allowlist |
| `BACKEND_PORT` | `8080` | host-порт backend |

Внутри контейнера backend всегда слушает `8080`; `BACKEND_PORT` меняет только host mapping.

## Frontend build-time параметры

| Переменная | Default | Назначение |
|---|---|---|
| `VITE_API_TIMEOUT_MS` | `15000` | обычные API requests |
| `VITE_SUBMIT_TIMEOUT_MS` | `180000` | длинный submit теста |
| `VITE_FILE_TRANSFER_TIMEOUT_MS` | `0` | загрузка/скачивание файлов |
| `APP_PUBLIC_REGISTRATION_ENABLED` | `false` | передаётся build arg как `VITE_PUBLIC_REGISTRATION_ENABLED` |

Эти параметры считываются **при `npm run build`** и встраиваются в JavaScript bundle. Изменение `.env` требует rebuild frontend image.

## Nginx runtime параметры

| Переменная | Default | Назначение |
|---|---|---|
| `FRONTEND_PROXY_SEND_TIMEOUT` | `300s` | proxy send timeout |
| `FRONTEND_PROXY_READ_TIMEOUT` | `300s` | proxy read timeout |

Compose передаёт их как runtime environment в frontend container.

## Backend поддерживает дополнительные переменные

`application.yml` также понимает:

```text
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE
SPRING_JPA_SHOW_SQL
APP_STORAGE_LECTURE_MATERIALS_DIR
APP_DATABASE_BACKUP_PG_DUMP_PATH
APP_DATABASE_BACKUP_PSQL_PATH
APP_DATABASE_BACKUP_SCHEMA
APP_DATABASE_BACKUP_INCLUDE_CLEAN
APP_TEXT_ANSWER_LOCAL_LLM_ENABLED
APP_TEXT_ANSWER_LOCAL_LLM_ENDPOINT
APP_TEXT_ANSWER_LOCAL_LLM_MODEL
APP_TEXT_ANSWER_LOCAL_LLM_TIMEOUT_SECONDS
```

Однако текущий `docker-compose.yml` **не пробрасывает все эти значения из `.env`**. Значение, просто добавленное в `.env`, не обязательно попадёт в backend container, пока соответствующая строка не добавлена в `environment:` Compose.

## Runtime против build-time

```text
Backend environment
→ можно изменить и пересоздать backend container

Frontend VITE_*
→ требуется пересобрать frontend image
```

Особенно важно для публичной регистрации: backend flag меняется runtime, а frontend visibility — build-time. Они должны оставаться синхронизированными.
