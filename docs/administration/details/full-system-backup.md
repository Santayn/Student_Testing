# Полная резервная копия системы

## Состав полного backup

Полный backup Student Testing состоит минимум из:

```text
1. PostgreSQL SQL dump
2. lecture-uploads volume
3. .env / deployment configuration
4. идентификатор версии приложения
```

При этом `.env` должен храниться отдельно и безопасно, потому что содержит секреты.

## Почему SQL недостаточно

```text
PostgreSQL
→ LectureMaterial metadata

lecture-uploads
→ реальные файлы
```

Восстановление только SQL создаёт неполную систему.

## Рекомендуемый manifest

Поскольку приложение пока не создаёт backup manifest автоматически, оператору желательно самостоятельно фиксировать рядом с backup:

```text
backup timestamp
application version / Git commit
database backup filename
lecture-files archive filename
configuration version
```

Секреты в manifest не записывать.

## Рекомендуемая последовательность

```text
1. ограничить mutations / выбрать maintenance window
2. создать DB backup
3. сохранить lecture-uploads
4. зафиксировать версию приложения
5. проверить наличие обоих файлов
6. перенести backup во внешнее защищённое хранилище
7. периодически проверять возможность восстановления
```

## Retention

Встроенной retention policy нет. Её должен определить оператор. Минимально разумно иметь несколько исторических точек восстановления, а не только один последний backup.

## Off-site copy

Named volume на том же host не является резервной копией. Backup должен существовать вне исходного Docker host или хотя бы на независимом носителе.
