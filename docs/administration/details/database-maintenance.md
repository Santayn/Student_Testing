# Обслуживание базы данных

## Что делает PostgreSQL автоматически

Официальный PostgreSQL image использует стандартные механизмы PostgreSQL, включая autovacuum. Приложение не реализует собственный scheduler для `VACUUM/ANALYZE`.

## Что приложение не обслуживает автоматически

Нет scheduled cleanup для:

- истёкших refresh tokens;
- отозванных refresh tokens;
- demo/smoke данных;
- orphan lecture files.

## RefreshTokens

Таблица хранит историю refresh sessions. При долгой эксплуатации желательно определить политику удаления записей, которые давно истекли и больше не нужны для аудита/безопасности.

Автоматической retention policy сейчас нет.

## Schema management

Схема развивается через:

```text
Hibernate ddl-auto
+
schema.sql
```

Versioned migrations отсутствуют. Поэтому operator не имеет стандартной команды вроде:

```text
flyway migrate
```

или:

```text
flyway info
```

и не может получить формальный номер версии схемы.

## Ручные SQL-операции

Любые ручные изменения production БД должны предваряться backup и быть документированы. Прямое редактирование таблиц ролей, пользователей, попыток и результатов особенно рискованно из-за связей и security semantics.

## Рекомендуемые улучшения

1. Flyway/Liquibase;
2. PostgreSQL integration tests;
3. refresh-token retention job;
4. storage integrity checker;
5. backup manifest/schema version.
