# Порты, пути и сервисы

## HTTP paths

| Назначение | Path |
|---|---|
| REST API prefix | `/api/v1` |
| Swagger UI | `/swagger-ui.html` |
| OpenAPI JSON | `/v3/api-docs` |
| API status | `/api/v1/status` |
| Backend readiness | `/api/v1/status/readiness` |
| Frontend Nginx health | `/health` |

## Docker services

| Service/container | Назначение |
|---|---|
| `student-test-postgres` | PostgreSQL 16 |
| `student-test-backend` | Spring Boot backend |
| `student-test-frontend` | Nginx + Vue SPA |

## Images

```text
postgres:16-alpine
student-test-backend:latest
student-test-frontend:latest
```

## Volumes

```text
postgres-data
lecture-uploads
```

`postgres-data` содержит БД. `lecture-uploads` содержит реальные файлы материалов и не входит в SQL backup.

## Порты Docker

| Компонент | Значение |
|---|---|
| Frontend host | `80` |
| Backend container | `8080` |
| Backend host | `127.0.0.1:${BACKEND_PORT:-8080}` |
| PostgreSQL internal | `5432` |
| PostgreSQL host | не публикуется |

## Storage paths

| Контекст | Путь |
|---|---|
| Backend default lecture storage | `uploads/lecture-materials` |
| Docker backend lecture storage | `/app/uploads/lecture-materials` |
| Nginx SPA root | `/usr/share/nginx/html` |

## Local development

Vite dev proxy читает `BACKEND_PORT` из корневого `.env` и по умолчанию использует `127.0.0.1:8080`. Поэтому тот же override, который меняет Docker host mapping, автоматически меняет и frontend dev proxy target.
