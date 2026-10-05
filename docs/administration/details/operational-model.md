# Operational-модель

## Контейнеры

`docker-compose.yml` поднимает три сервиса:

```text
student-test-postgres
student-test-backend
student-test-frontend
```

Для всех задано:

```yaml
restart: unless-stopped
```

Это означает автоматический повторный запуск процесса после падения/перезагрузки host, пока контейнер не был явно остановлен оператором.

## Зависимости запуска

```text
PostgreSQL
   ↓ service_healthy
Backend
   ↓ service_healthy
Frontend
```

PostgreSQL проверяется через `pg_isready`, backend — через `/api/v1/status/readiness`.

## Persistence

```text
postgres-data
→ /var/lib/postgresql/data

lecture-uploads
→ /app/uploads/lecture-materials
```

Контейнеры можно пересоздавать без потери этих данных, пока named volumes сохранены.

## Сеть

Внешне публикуются:

```text
frontend: 80:80
backend: 127.0.0.1:${BACKEND_PORT:-8080}:8080
```

PostgreSQL host-порт не публикуется. Backend доступен с host только через loopback, а браузер в обычном сценарии обращается к API через Nginx frontend-контейнера.

## Источник конфигурации

Compose читает `.env`. Критические секреты:

```text
POSTGRES_PASSWORD
APP_JWT_SECRET
```

`.env` не должен храниться в репозитории или передаваться вместе с публичным архивом проекта.

## Граница отказа

Поскольку deployment односерверный, отказ host означает отказ всей системы. Named volumes защищают от пересоздания контейнера, но не от:

- отказа физического диска;
- удаления volumes;
- повреждения файловой системы;
- компрометации host;
- ошибочного restore.

Для этих рисков необходимы внешние резервные копии.
