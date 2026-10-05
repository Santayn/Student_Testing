# Operational checklist

## После запуска

- [ ] `docker compose ps` показывает работающие сервисы.
- [ ] PostgreSQL healthy.
- [ ] Backend `/api/v1/status/readiness` возвращает `200`.
- [ ] Frontend `/health` возвращает `ok`.
- [ ] ADMIN может войти.
- [ ] Основные страницы открываются без `5xx`.

## Периодически

- [ ] Проверить свободное место на host.
- [ ] Проверить размер PostgreSQL/lecture uploads.
- [ ] Проверить свежесть внешнего DB backup.
- [ ] Проверить свежесть backup `lecture-uploads`.
- [ ] Просмотреть повторяющиеся backend/PostgreSQL errors.
- [ ] Убедиться, что существует минимум два контролируемых ADMIN перед рискованными изменениями ролей.
- [ ] Периодически проверить restore backup в отдельной среде.

## Перед обновлением

- [ ] Зафиксировать текущий commit/release.
- [ ] Создать DB backup.
- [ ] Сохранить `lecture-uploads`.
- [ ] Сохранить `.env` безопасно.
- [ ] Проверить место на диске.
- [ ] Понять изменения schema/configuration.

## После обновления

- [ ] Проверить контейнеры.
- [ ] Проверить readiness/health.
- [ ] Проверить ADMIN login.
- [ ] Проверить STUDENT базовый flow.
- [ ] Проверить TEACHER workspace.
- [ ] Проверить lecture material download.
- [ ] При изменении тестового домена выполнить attempt flow.

## Перед restore

- [ ] Убедиться, что backup доверенный.
- [ ] Проверить его размер.
- [ ] Сохранить текущее состояние БД.
- [ ] Сохранить текущий file volume.
- [ ] Остановить/ограничить пользовательские mutations.
- [ ] Убедиться в соответствии DB/file snapshots.

## После restore

- [ ] Перезапустить backend при необходимости.
- [ ] Проверить readiness.
- [ ] Проверить роли ADMIN.
- [ ] Проверить refresh/session strategy.
- [ ] Проверить lecture materials.
- [ ] Выполнить smoke/regression checks.

## Перед destructive Docker operations

Перед:

```bash
docker compose down -v
```

должны существовать **проверенные** DB и lecture-files backups.
