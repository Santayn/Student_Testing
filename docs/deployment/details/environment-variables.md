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

## ИИ

| Переменная | Default | Назначение |
|---|---|---|
| `APP_AI_PROVIDER` | `none` | `none`, `yandex`, `gigachat`, `openai` |
| `APP_AI_API_KEY` | пусто | ключ API (GigaChat — ключ авторизации) |
| `APP_AI_FOLDER_ID` | пусто | каталог Yandex Cloud |
| `APP_AI_BASE_URL` | по провайдеру | адрес API, например Ollama `http://host.docker.internal:11434/v1` |
| `APP_AI_AUTH_SCHEME` | по провайдеру | `Api-Key` у Yandex, иначе `Bearer` |
| `APP_AI_TIMEOUT_SECONDS` | `90` | timeout запроса к модели |
| `APP_AI_MONTHLY_TOKEN_LIMIT` | `20000000` | токенов в месяц, `0` — без лимита |
| `APP_AI_MODEL_GRADING` | `aliceai-llm-flash` | проверка ответов и критерии |
| `APP_AI_MODEL_LECTURE` | `aliceai-llm-flash` | разметка сканов |
| `APP_AI_MODEL_PRESENTATION` | `aliceai-llm` | план презентации |
| `APP_AI_MODEL_IMAGE` | `aliceai-image-art-3.0` | картинки |
| `APP_AI_GRADING_ENABLED` | `true` | проверка свободных ответов |
| `APP_AI_GRADING_REVIEW_THRESHOLD` | `0.7` | ниже — ответ уходит преподавателю |
| `APP_AI_OCR_PROVIDER` | пусто | `yandex`, `tesseract`, `none`; пусто — по провайдеру |
| `APP_AI_IMAGE_GENERATION_ENABLED` | `true` | генерация картинок |
| `APP_AI_IMAGE_SEARCH_ENABLED` | `true` | поиск в Wikimedia Commons и Openverse |

Рекомендуемая настройка (Groq) и какие ключи где нужны — в [README → ИИ: ключи API](../../../README.md#ии-ключи-api). Ключ хранится только в локальном `.env`.

Compose хранит файлы задач ИИ в volume `ai-files` (`APP_AI_STORAGE_DIR=/app/uploads/ai`). Подробно: [ИИ](../../technical/details/ai.md).

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
