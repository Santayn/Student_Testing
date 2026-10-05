# Диагностика инцидентов

## Система полностью недоступна

Проверить:

```bash
docker compose ps
docker compose logs --tail=200 postgres
docker compose logs --tail=200 backend
docker compose logs --tail=200 frontend
```

Затем проверить host:

- Docker daemon;
- свободный диск;
- занятость портов;
- наличие `.env`;
- volumes.

## Frontend открывается, API не работает

Проверить:

```text
backend container state
/api/v1/status/readiness
backend logs
PostgreSQL health
```

Frontend `/health` подтверждает только Nginx.

## Backend `unhealthy`

Поскольку readiness зависит от БД, сначала проверить PostgreSQL. `unhealthy` сам по себе не гарантирует автоматический restart.

## Ошибки загрузки/скачивания материалов

Проверить:

- backend logs;
- `lecture-uploads` volume;
- свободное место;
- права записи;
- наличие файла по stored path;
- согласованность DB record и filesystem.

## Backup не создаётся

Проверить:

```text
pg_dump присутствует
DB credentials
DB connectivity
APP_DATABASE_BACKUP_PG_DUMP_PATH
backend logs
```

## Restore не запускается

Проверить:

- SQL файл не пустой;
- размер <= текущего multipart limit;
- `psql` присутствует;
- достаточные DB permissions;
- свободное место;
- backend logs.

## Пользователь потерял доступ

Проверить:

```text
User.active
Person binding
roles
permissions
```

Если потерян пароль, штатного admin reset workflow сейчас нет.

## ADMIN потерял доступ

Если второго ADMIN нет, восстановление штатным UI может быть невозможно. Именно поэтому last-admin protection и bootstrap/recovery mechanism должны быть реализованы до production эксплуатации.
