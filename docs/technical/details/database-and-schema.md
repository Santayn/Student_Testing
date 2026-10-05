# База данных и схема

## PostgreSQL

Основная БД проекта — PostgreSQL 16. Backend использует PostgreSQL JDBC driver и Spring Data JPA/Hibernate.

В Docker состояние БД хранится в volume `postgres-data`.

## JPA/Hibernate

В production-oriented конфигурации значение по умолчанию:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

То есть backend ожидает, что существующая схема соответствует JPA-модели.

В local profile и текущем `docker-compose.yml` по умолчанию используется:

```yaml
spring.jpa.hibernate.ddl-auto: update
spring.sql.init.mode: always
```

Это означает, что локальная схема изменяется одновременно двумя механизмами:

1. Hibernate `ddl-auto=update`;
2. SQL-скриптом `backend/src/main/resources/schema.sql`.

## `schema.sql`

Скрипт содержит в основном идемпотентные операции:

```sql
CREATE TABLE IF NOT EXISTS ...
ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...
CREATE INDEX IF NOT EXISTS ...
```

Он используется как слой совместимости/доработки существующей схемы и добавляет таблицы, колонки, индексы и ограничения.

## Индексы и ограничения целостности

Особенно важные ограничения относятся к тестовому домену.

### Уникальный ordinal попытки

```text
(TestAssignmentId, PersonId, Ordinal) must be unique
```

Это не позволяет создать две попытки с одинаковым порядковым номером для одного assignment и Person.

### Только одна активная попытка

В PostgreSQL присутствует частичный уникальный индекс `IX_TestAttempts_One_InProgress` на `TestAssignmentId + PersonId` для активного состояния. Он служит последней линией защиты от гонок при одновременном старте попытки.

### Ответ на вопрос в попытке

`QuestionResponses` ограничены уникальностью пары попытка/вопрос, что предотвращает дублирование response-записи одного и того же вопроса в рамках попытки.

## Quoted identifiers

Проект использует `QuotedIdentifierNamingStrategy`, а таблицы и колонки в моделях задаются с quoted identifiers. Это важно учитывать при ручном SQL: имена чувствительны к точному регистру и кавычкам.

## Расширение `citext`

Для соединения datasource используется `connection-init-sql`:

```sql
CREATE EXTENSION IF NOT EXISTS citext
```

Следовательно, пользователь БД должен иметь право создать/использовать соответствующее расширение в целевой среде.

## Отсутствие версионированных миграций

На текущем этапе Flyway/Liquibase в зависимостях отсутствуют. Схема развивается через сочетание JPA и `schema.sql`.

Это создаёт ограничения:

- нет явной последовательности версий схемы;
- сложнее воспроизвести путь обновления старой БД;
- откат миграции не формализован;
- Hibernate update может вести себя иначе при сложных изменениях типов/constraints.

Для зрелого production-процесса рекомендуется перейти на версионированные миграции.

## Найденное несоответствие схемы

В текущем `schema.sql` присутствует создание внешнего ключа:

```sql
"Tests"."AuthorPersonId" REFERENCES "People"("Id")
```

При этом JPA-сущность `Person` отображена на таблицу:

```text
Person
```

Это несоответствие должно быть проверено и исправлено до того, как `schema.sql` станет надёжным источником схемы. Подробно оно зафиксировано в [технических ограничениях](./technical-limitations.md).

## Backup и схема

SQL-backup, создаваемый `DatabaseBackupService`, работает через PostgreSQL tools (`pg_dump`/`psql`). Он охватывает БД, но не внешний volume материалов лекций. Полный backup системы должен учитывать оба хранилища.
