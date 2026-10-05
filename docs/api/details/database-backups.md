# API резервного копирования

Строго административный API для выгрузки SQL dump и восстановления PostgreSQL.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `POST` | `/api/v1/admin/database-backups` | Создать и скачать SQL-резервную копию БД | ADMIN | `200` | `databaseBackups.api.js` |
| `POST` | `/api/v1/admin/database-backups/restore` | Восстановить БД из SQL-файла | ADMIN | `200` | `databaseBackups.api.js` |

## Создание backup

```http
POST /api/v1/admin/database-backups
```

Успешный ответ — бинарный SQL-файл. Frontend использует `responseType: blob`.

## Restore

```http
POST /api/v1/admin/database-backups/restore
Content-Type: multipart/form-data
```

Field: `file`.

Успешный JSON response содержит `status`, `fileName`, `sizeBytes`, `restoredAtUtc`.

## Безопасность

В отличие от некоторых административных namespaces, backup требует strict role `ADMIN`; одного `roles.manage` недостаточно.

## Граница backup

Этот API сохраняет PostgreSQL, но не файловый volume `lecture-uploads`. Полное резервное копирование системы требует отдельного сохранения файлов материалов.
