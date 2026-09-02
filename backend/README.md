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
