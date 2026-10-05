# Особенности резервного копирования

## Встроенный механизм

Backend содержит `DatabaseBackupService`, который использует внешние PostgreSQL utilities:

```text
pg_dump
psql
```

Поэтому runtime backend image устанавливает пакет `postgresql-client`.

## Что сохраняет SQL backup

Встроенная административная операция сохраняет **содержимое PostgreSQL**.

В неё входят записи о:

- пользователях и ролях;
- академической структуре;
- лекциях и тестах;
- попытках и результатах;
- metadata материалов лекций.

## Что SQL backup не сохраняет

Файлы материалов находятся не в PostgreSQL, а в:

```text
lecture-uploads volume
```

Поэтому SQL dump не содержит физических файлов.

## Полный backup системы

Полный резервный комплект должен состоять минимум из:

```text
1. PostgreSQL dump
2. копия lecture-uploads
```

Восстановление только первого компонента может привести к состоянию, где строки `LectureMaterial` присутствуют, но referenced file отсутствует.

## Consistency

Для строгой согласованности DB metadata и filesystem backup следует определить эксплуатационную процедуру: например, временно остановить операции изменения материалов или использовать snapshot-capable storage.

## Версия PostgreSQL client

Database container фиксирован на PostgreSQL 16, а backend runtime устанавливает distro package `postgresql-client` без явной major-version pinning. Перед production следует проверять совместимость `pg_dump`/`psql` с сервером PostgreSQL 16.

## Где описывать пользовательскую операцию

Как администратор нажимает кнопки backup/restore в UI — это `user-guide`. Здесь фиксируются только deployment/storage последствия.
