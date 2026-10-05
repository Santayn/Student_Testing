# Health, runtime и контейнерная среда

## Docker Compose

Текущий stack состоит из трёх сервисов:

```text
postgres
backend
frontend
```

Volumes:

```text
postgres-data
lecture-uploads
```

## Backend container

Backend собирается multi-stage Dockerfile:

1. Maven build на `maven:3.9.9-eclipse-temurin-17`;
2. runtime на `eclipse-temurin:17-jre`.

Runtime image дополнительно содержит:

- `curl` для healthcheck;
- `postgresql-client` для `pg_dump` и `psql`.

Приложение запускается от непривилегированного системного пользователя `studenttest`.

## Frontend container

Frontend также собирается multi-stage:

1. Node 22 выполняет `npm ci` и `npm run build`;
2. Nginx 1.27 раздаёт `/dist`.

## Nginx

Nginx выполняет:

- SPA fallback на `index.html`;
- reverse proxy `/api/` на backend;
- long-lived immutable caching для `/assets/`;
- no-cache для `index.html`;
- health endpoint `/health`;
- ограничение body `200m`.

Установлены security headers:

- `X-Content-Type-Options: nosniff`;
- `X-Frame-Options: DENY`;
- `Referrer-Policy`;
- `Permissions-Policy`;
- Content-Security-Policy.

## Backend status endpoints

Spring Actuator в текущем проекте не используется. Реализованы собственные endpoint'ы:

```text
/api/v1/status
/api/v1/status/readiness
```

Readiness выполняет реальную проверку PostgreSQL (`SELECT 1`).

Backend Docker healthcheck обращается к readiness endpoint.

## Startup dependency order

Compose использует health-based dependencies:

```text
postgres healthy
    ↓
backend starts / becomes healthy
    ↓
frontend starts
```

Это уменьшает вероятность запуска backend до готовности БД и frontend до готовности API.

## Local profile

`SPRING_PROFILES_ACTIVE=local` по умолчанию включён в текущем compose. Local profile активирует:

- `ddl-auto=update`;
- `spring.sql.init.mode=always`;
- `app.data-loader.enabled=true`.

Это удобно для локальной/демо-среды, но не должно автоматически переноситься в production.

## CORS

Backend поддерживает configurable `APP_CORS_ALLOWED_ORIGINS`. В Docker production-подобной схеме основной browser traffic идёт same-origin через Nginx, поэтому CORS меньше влияет на обычный пользовательский поток, но остаётся важен для dev-среды и прямых API-клиентов.

## Backup runtime dependency

Функция backup/restore зависит от наличия совместимых `pg_dump` и `psql` внутри backend runtime. Dockerfile эту зависимость обеспечивает через `postgresql-client`.
