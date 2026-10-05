# Резервная копия базы данных

## Штатный механизм

ADMIN может создать backup через интерфейс/API. Для PostgreSQL backend запускает:

```text
pg_dump
```

с параметрами:

```text
--format=plain
--encoding=UTF8
--blobs
--no-owner
--no-privileges
--clean
--if-exists
```

`--clean/--if-exists` включаются по умолчанию и управляются `app.database-backup.include-clean`.

## Поток

```text
ADMIN request
   ↓
DatabaseBackupService
   ↓
pg_dump → temporary .sql
   ↓
HTTP download
   ↓
temporary file deleted
```

Backend не ведёт server-side архив backup-файлов.

## Требования

Runtime backend должен иметь доступный `pg_dump`. Docker image устанавливает PostgreSQL client.

При внешнем runtime путь можно настраивать через:

```text
APP_DATABASE_BACKUP_PG_DUMP_PATH
```

если параметр проброшен в окружение процесса.

## Безопасность файла

SQL backup содержит чувствительные данные, включая:

- учётные записи;
- персональные данные;
- password hashes;
- роли/permissions;
- результаты;
- refresh-token hashes.

Приложение не шифрует backup-файл. Его следует хранить в защищённом месте и не передавать по незащищённым каналам.

## Backup не является полным backup системы

`pg_dump` не содержит физические файлы `lecture-uploads`. Для полного восстановления нужен отдельный backup файлового volume.

См. [full-system-backup.md](./full-system-backup.md).
