# Файлы, multipart и binary responses

Большая часть API использует JSON, но несколько контрактов работают с файлами.

## Multipart requests

### Импорт вопросов

```http
POST /api/v1/questions/import
Content-Type: multipart/form-data
```

Передаётся DOCX-файл и контекст импорта, определённый controller contract.

### Материалы лекции

```http
POST /api/v1/lectures/{lectureId}/materials
Content-Type: multipart/form-data
```

Field: `files`, допускается несколько файлов.

### Restore DB

```http
POST /api/v1/admin/database-backups/restore
Content-Type: multipart/form-data
```

Field: `file`.

## Binary responses

Lecture material download:

```text
GET /lectures/{lectureId}/materials/{materialId}/download
GET /public/learning/lectures/{lectureId}/materials/{materialId}/download
```

Database backup:

```text
POST /admin/database-backups
```

Ответы содержат бинарный body и headers `Content-Type`, `Content-Disposition`, `Content-Length` при наличии размера.

## Размеры upload

Runtime backend и Nginx настроены на ограничение одного файла и общего запроса. Актуальные значения и deployment-настройки описаны в `docs/deployment`.

## Безопасность пути

Физическое имя stored file формируется backend, а пользовательское имя хранится как metadata. Backend нормализует пути и не должен доверять переданному filename как файловому пути.
