# Quality gates

## Frontend

Канонический gate:

```bash
cd frontend
npm ci
npm run quality
```

Он выполняет lint → static quality → Vitest → production build.

Доступны отдельные команды `npm run lint`, `npm run quality:static`, `npm run test:unit -- --run`, `npm run build`.

## Backend

Минимально:

```powershell
cd backend
.\mvnw.cmd test
```

Для package:

```powershell
.\mvnw.cmd package
```

Отдельного Java lint/format gate пока нет.

## Infrastructure

При изменении Docker/env/Nginx/filesystem/DB bootstrap дополнительно проверять Docker build/start и healthchecks.

## Database

PostgreSQL-specific изменение нельзя считать подтверждённым только H2 tests.

## Docs

Перед handoff проверить относительные Markdown-ссылки и соответствие docs фактическому контракту.
