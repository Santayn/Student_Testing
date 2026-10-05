# Обновление и пересборка

## Когда достаточно restart

Если изменились только runtime-переменные backend/Nginx, которые уже пробрасываются Compose, обычно достаточно пересоздать соответствующий container:

```bash
docker compose up -d backend
docker compose up -d frontend
```

Для гарантированного пересоздания environment можно использовать Compose recreate options по необходимости.

## Когда нужен build

Пересборка необходима при изменении:

- backend source;
- frontend source;
- `pom.xml`;
- `package.json` / lockfile;
- Dockerfile;
- frontend Vite build-time variables.

Стандартно:

```bash
docker compose up -d --build
```

## Frontend build-time configuration

Особенно важно: `VITE_*` переменные компилируются в bundle.

Например изменение:

```dotenv
APP_PUBLIC_REGISTRATION_ENABLED=true
```

должно сопровождаться rebuild frontend, поскольку frontend получает `APP_PUBLIC_REGISTRATION_ENABLED` на build stage.

Простой `docker compose restart frontend` оставит старый JavaScript bundle.

## Database compatibility

Поскольку в проекте пока нет versioned migration mechanism, обновление backend между версиями требует повышенной осторожности. Local `ddl-auto=update + schema.sql` не является гарантированной production migration strategy.

Перед обновлением production-подобной среды необходим backup БД и файлов материалов.

## Images

Compose использует локальные tags:

```text
student-test-backend:latest
student-test-frontend:latest
```

Для полноценного production release process лучше использовать versioned immutable image tags, а не только `latest`.
