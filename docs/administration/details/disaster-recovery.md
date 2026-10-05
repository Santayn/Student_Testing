# Disaster recovery

## Что считается disaster scenario

Примеры:

- потеря Docker host;
- повреждение PostgreSQL volume;
- ошибочное `docker compose down -v`;
- критически некорректный restore;
- потеря `lecture-uploads`;
- компрометация security secrets.

## Необходимые артефакты

Для полного восстановления нужны:

```text
исходники или versioned images
.env/configuration
PostgreSQL backup
lecture-uploads backup
```

Желательно также иметь manifest версии.

## Общая процедура нового host

```text
1. установить Docker/Compose
2. получить нужную версию проекта
3. восстановить безопасный .env
4. поднять базовый stack
5. остановить пользовательский traffic
6. восстановить PostgreSQL
7. восстановить lecture-uploads
8. перезапустить backend/frontend
9. проверить health
10. проверить ADMIN/TEACHER/STUDENT flows
11. проверить материалы лекций
12. только затем открыть систему пользователям
```

## Security после disaster recovery

Если backup старый или причина аварии связана с компрометацией:

- ротировать `APP_JWT_SECRET`;
- пересмотреть refresh sessions;
- сменить DB credentials при необходимости;
- проверить ADMIN accounts/roles;
- не полагаться на старые активные сессии.

## Проверка восстановления

Backup считается надёжным только если периодически проверяется его restore в отдельной тестовой среде. Наличие `.sql` файла само по себе не гарантирует успешное восстановление.
