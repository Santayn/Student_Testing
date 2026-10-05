# Ограничения эксплуатации

## Критичные/высокоприоритетные

### Нет штатного создания User

При выключенной публичной регистрации ADMIN не имеет полноценного UI/API workflow создания нового User.

### Нет password recovery/reset

Нет admin reset, forgot-password или recovery token flow.

### Last-admin lockout

Нет гарантии сохранения хотя бы одного активного ADMIN при изменении ролей/активности.

### Деактивация не мгновенно инвалидирует access JWT

Старый access token может оставаться рабочим до expiration.

### Полный backup состоит из двух источников

SQL backup не включает `lecture-uploads`.

## Средний приоритет

### Restore по работающей системе

Maintenance mode отсутствует; concurrent requests технически возможны во время restore.

### Restore limit 50 MB

Backup может стать больше лимита загрузки restore.

### Нет backup manifest/schema version

Нельзя автоматически проверить совместимость backup с версией приложения.

### Старый backup может вернуть старые refresh sessions

После restore нужен отдельный session-security plan.

### Нет scheduled/off-site backup automation

Штатный backup выполняется вручную.

### Нет централизованных логов/audit trail

Критические административные действия не формируют отдельную операционную историю.

### `traceId` не коррелируется с backend logs

Пользовательский error identifier сейчас слаб для расследования.

### Нет versioned image rollback

Используются `latest` images.

### Нет migration-based rollback

Schema lifecycle зависит от Hibernate + `schema.sql`.

## Дополнительные ограничения

- healthchecks не проверяют файловый storage и disk capacity;
- Docker `unhealthy` не равен автоматическому restart;
- нет метрик/alerting;
- нет cleanup expired refresh tokens;
- нет orphan lecture-file cleanup;
- нет maintenance mode;
- нет HA/multi-host orchestration;
- DataLoader выполняется только на startup.

## Приоритет улучшений

```text
1. admin bootstrap + User creation + password recovery
2. last-admin protection + immediate disabled-user rejection
3. full backup strategy DB + files
4. maintenance-mode restore workflow
5. migrations + versioned releases
6. scheduled backups + restore verification
7. structured logging/MDC/audit log
8. metrics/alerting/storage monitoring
9. token/file retention cleanup jobs
```
