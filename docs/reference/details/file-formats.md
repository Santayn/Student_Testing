# Файлы и форматы

## Импорт вопросов

Поддерживается:

```text
.docx
```

Парсинг выполняется backend через Apache POI. Формат документа должен соответствовать ожидаемой структуре вопросов проекта.

## Материалы лекций

Специального allowlist расширений в текущем backend нет. Ограничения определяются multipart limits и проверками непустого файла.

Файлы хранятся вне PostgreSQL; в БД находятся метаданные и путь.

## Database backup

Создаётся SQL dump:

```text
application/sql
student-test-database-backup-<timestamp>.sql
```

Backup создаётся через `pg_dump` и не включает volume `lecture-uploads`.

## Database restore

Web UI ориентирован на `.sql` и принимает SQL-файл через `multipart/form-data`.

Практический лимит штатного web restore — 50 MB из-за multipart configuration/UI validation. Это может быть меньше размера успешно созданного backup по мере роста базы.

## Binary download

Материалы лекций и SQL backup возвращаются бинарным response с `Content-Disposition: attachment`; frontend использует `responseType: blob`.
