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

Vite использует `VITE_*`. Эти значения попадают в browser bundle и считаются публичными.

В Docker изменение `VITE_*` обычно требует rebuild frontend, а не restart.

## Feature flags

Стараться централизовать их в `config/features.js`, а не разбрасывать `import.meta.env` по Views.

## Registration

Public registration имеет backend runtime flag и frontend build flag. Они должны быть согласованы.

## Secrets

Секреты нельзя хранить в source, commit'ить в `.env` или помещать в frontend `VITE_*`.
