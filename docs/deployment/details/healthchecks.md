# Healthchecks

## PostgreSQL

Compose использует:

```text
pg_isready -U <user> -d <database>
```

Параметры:

```text
interval: 5s
timeout: 5s
retries: 30
start_period: 10s
```

Backend запускается только после `service_healthy` PostgreSQL.

## Backend readiness

Endpoint:

```text
GET /api/v1/status/readiness
```

не является только проверкой JVM. Он выполняет проверку готовности работы с PostgreSQL.

Docker image использует его в HEALTHCHECK.

Frontend зависит от backend condition `service_healthy`.

## Frontend

Frontend Nginx предоставляет:

```text
GET /health
```

и отвечает `200 ok`.

Это подтверждает работоспособность Nginx/static frontend container.

## Интерпретация

```text
postgres healthy
→ БД принимает соединения

backend healthy
→ приложение работает и readiness DB успешна

frontend healthy
→ Nginx принимает HTTP
```

Frontend health endpoint сам по себе не подтверждает доступность backend.

## Диагностика

```bash
docker compose ps
```

Затем при необходимости:

```bash
docker compose logs postgres
docker compose logs backend
docker compose logs frontend
```

Для backend readiness с host:

```text
http://localhost:8080/api/v1/status/readiness
```

при стандартном `BACKEND_PORT=8080`.
