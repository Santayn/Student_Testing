# Backend

Spring Boot REST backend приложения Student Testing.

## API

Единственный канонический префикс REST API:

```text
/api/v1/**
```

Legacy aliases `/api/**` удалены. Swagger после запуска backend:

```text
http://localhost:8080/swagger-ui.html
```

## Локальный запуск без Docker

```bash
./mvnw spring-boot:run
```

На Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Docker Compose запускается из корня репозитория.
## Production database migrations

Production uses schema validation and does not execute `schema.sql` automatically. Before deploying a backend version with database changes, apply the pending PostgreSQL scripts in the canonical order documented here:

```text
backend/docs/sql/README.md
```

Do not enable development SQL initialization in production as a migration substitute.
