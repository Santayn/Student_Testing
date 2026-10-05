# Configuration и feature flags

## Backend

При новой настройке проверить:

```text
application.yml default
→ env name
→ docker-compose pass-through
→ .env.example
→ tests
→ deployment docs
```

Наличие env placeholder в Spring config не означает, что Docker Compose уже пробрасывает эту переменную.

## Frontend

Vite читает корневой `.env` через `envDir`. Стандартные `VITE_*` попадают в browser bundle и считаются публичными. Дополнительно через `define` публикуется только один безопасный `APP_*` flag — `APP_PUBLIC_REGISTRATION_ENABLED`; остальные `APP_*` клиенту не экспонируются.

В Docker изменение frontend build-time значений требует rebuild frontend, а не restart.

## Feature flags

Стараться централизовать их в `config/features.js`, а не разбрасывать `import.meta.env` по Views.

## Registration

Public registration имеет один канонический flag `APP_PUBLIC_REGISTRATION_ENABLED`. Backend читает его runtime, а Vite публикует тот же flag в frontend build/dev environment.

## Secrets

Секреты нельзя хранить в source, commit'ить в `.env`, помещать в `VITE_*` или явно публиковать через Vite `define`.
