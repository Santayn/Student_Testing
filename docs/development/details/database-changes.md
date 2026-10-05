# Изменение базы данных

## Текущее состояние

В проекте нет Flyway/Liquibase. Схема развивается комбинацией:

```text
JPA Entity
+
Hibernate ddl-auto
+
schema.sql
```

## Добавление поля

Проверить:

1. JPA Entity;
2. PostgreSQL type/nullability;
3. default для существующих строк;
4. index/unique/FK;
5. `schema.sql`, если нужен compatibility DDL;
6. DTO/API;
7. H2 test profile;
8. clean PostgreSQL start;
9. upgrade существующей БД.

## Добавление таблицы

Нужно определить quoted name, ID type, foreign keys, indexes, delete lifecycle, JPA relationships и ownership.

## Почему Entity недостаточно

`ddl-auto=update` может помочь локально, но не является воспроизводимой production migration. JUnit profile использует H2/create-drop и не запускает `schema.sql`.

Поэтому зелёные JUnit tests не доказывают корректность production DDL.

## Известный класс риска

Ранее в `schema.sql` было обнаружено расхождение `"People"` / `"Person"`. Такой дефект возможен именно потому, что test profile не исполняет production schema patches.

## Целевая модель

```text
Flyway/Liquibase
→ versioned migrations
→ PostgreSQL integration tests
→ controlled upgrades
```

До появления миграций database change требует отдельной PostgreSQL-проверки.
