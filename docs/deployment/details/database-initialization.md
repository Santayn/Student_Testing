# Инициализация базы данных

## Local Docker configuration

Стандартный Compose запускает backend с:

```text
SPRING_PROFILES_ACTIVE=local
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_SQL_INIT_MODE=always
APP_DATA_LOADER_ENABLED=true
```

`application-local.yml` также задаёт `ddl-auto=update`, `sql.init.mode=always` и включает DataLoader.

## Порядок инициализации

Упрощённо локальный первый старт выглядит так:

```text
PostgreSQL container
        ↓
DataSource connection-init-sql
CREATE EXTENSION IF NOT EXISTS citext
        ↓
Hibernate ddl-auto=update
        ↓
schema.sql
        ↓
DataLoader
```

`spring.jpa.defer-datasource-initialization=true` позволяет запускать SQL init после JPA schema processing.

## `citext`

Некоторые поля используют PostgreSQL `citext` для case-insensitive uniqueness/сравнений. Backend выполняет:

```sql
CREATE EXTENSION IF NOT EXISTS citext;
```

через Hikari `connection-init-sql`, а `schema.sql` также содержит создание extension.

При использовании внешней PostgreSQL пользователь приложения должен иметь необходимые права или DBA должен установить `citext` заранее.

## Hibernate

Local environment использует:

```text
ddl-auto=update
```

Это удобно для разработки, поскольку Hibernate может создавать/дополнять schema из JPA entities.

Production default `application.yml`, если переменная не задана, — `validate`.

## `schema.sql`

`schema.sql` содержит дополнительные `CREATE TABLE IF NOT EXISTS`, `ALTER TABLE`, constraints и indexes, появившиеся по мере развития проекта.

Это не полноценная версия миграций. В проекте сейчас нет Flyway/Liquibase последовательности `V1`, `V2`, ... .

## Важное ограничение

В текущем `schema.sql` присутствует FK:

```text
Tests.AuthorPersonId → People.Id
```

тогда как текущая JPA-модель использует таблицу `Person`. До использования `schema.sql` как надёжного production bootstrap это несовпадение должно быть исправлено.

## Production

Комбинация:

```text
ddl-auto=validate
sql.init.mode=never
```

предполагает, что схема уже создана корректным внешним mechanism. Такого версионированного migration pipeline в текущем проекте ещё нет.
