# Восстановление базы данных

## Штатный механизм

ADMIN загружает SQL backup, после чего backend запускает:

```text
psql
--set ON_ERROR_STOP=1
--single-transaction
```

Это уменьшает риск частично применённого restore и останавливает выполнение при SQL error.

## Multipart limit

Backend по умолчанию ограничивает один загружаемый файл:

```text
50 MB
```

а request — `200 MB`. Frontend также ограничивает SQL restore 50 МБ.

При этом `pg_dump` backup не ограничен 50 МБ. Поэтому по мере роста БД возможен сценарий:

```text
backup успешно создан (>50 MB)
        ↓
restore через штатный UI невозможен
```

До серьёзной эксплуатации лимит restore необходимо согласовать с реальным размером БД.

## Restore выполняется по живой системе

Текущая версия не имеет maintenance mode. Во время restore backend продолжает обслуживать другие запросы.

Для production restore рекомендуется операционная процедура:

```text
1. остановить пользовательский traffic
2. сохранить текущую БД и lecture-files
3. выполнить restore
4. восстановить согласованную копию файлов
5. перезапустить backend
6. проверить health
7. выполнить smoke/regression checks
8. только затем вернуть traffic
```

## Доверие к SQL

Backend проверяет, что файл не пустой, но не проверяет:

- цифровую подпись;
- manifest;
- принадлежность Student Testing;
- версию схемы;
- checksum.

Восстанавливать следует только доверенные backup-файлы.

## Совместимость версий

Backup не содержит явного application/schema version manifest. Из-за отсутствия Flyway/Liquibase нельзя автоматически гарантировать совместимость старого dump с новой версией backend.

## После restore

Необходимо отдельно проверить:

- административный вход;
- роли и permissions;
- refresh sessions;
- lecture materials;
- readiness;
- основные STUDENT/TEACHER сценарии.
