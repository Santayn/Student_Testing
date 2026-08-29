# Student Testing

Проект разделён на backend и frontend. Docker-конфигурация находится в корне репозитория.

```text
Student_Testing/
├── backend/              # Spring Boot REST API
│   ├── src/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── docs/
│   └── scripts/
├── frontend/             # отдельный frontend
│   └── legacy-static/    # прежний встроенный HTML/CSS/JS интерфейс
├── Dockerfile            # сборка backend
├── docker-compose.yml    # PostgreSQL + backend
├── .dockerignore
└── .env.example
```

## REST API

Backend публикует только версионированный API:

```text
/api/v1/**
```

Старые aliases `/api/**` удалены.

После запуска:

- Backend: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI: `http://localhost:8081/v3/api-docs`
- Health/status API: `GET http://localhost:8081/api/v1/status`

## Docker

Из корня проекта:

```bash
docker compose up --build
```

Сервисы:

- `postgres` — PostgreSQL 16
- `backend` — Spring Boot backend

Frontend полностью исключён из Docker. Compose запускает только PostgreSQL и Spring Boot backend, а `.dockerignore` исключает каталог `frontend/` из build context. Текущий `frontend/legacy-static` хранится отдельно и запускается вне Docker.
