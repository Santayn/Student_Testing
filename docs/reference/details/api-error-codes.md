# Коды ошибок API

## Реальные backend codes

| Code | Типичная ситуация |
|---|---|
| `validation_failed` | Bean Validation или ошибка полей request. |
| `bad_request` | Некорректный запрос/argument. |
| `unauthorized` | Не выполнена authentication. |
| `forbidden` | Недостаточно прав или нет доступа к объекту. |
| `not_found` | Ресурс не найден. |
| `conflict` | Конфликт текущего состояния/ограничений данных. |
| `payload_too_large` | Multipart request/file превышает лимит. |
| `database_backup_failed` | Ошибка backup/restore БД. |
| `internal_error` | Необработанная внутренняя ошибка. |

Типовая форма:

```json
{
  "code": "validation_failed",
  "message": "Validation error",
  "details": [],
  "traceId": "..."
}
```

## Frontend-reserved / anticipated codes

Frontend дополнительно знает значения, которые текущий backend обычно не генерирует как гарантированный контракт:

```text
VALIDATION_ERROR
ACCESS_DENIED
RESOURCE_NOT_FOUND
RATE_LIMITED
GROUP_HAS_DEPENDENCIES
FACULTY_HAS_DEPENDENCIES
SUBJECT_HAS_DEPENDENCIES
TOPIC_HAS_DEPENDENCIES
```

Не следует документировать эти коды как гарантированные backend responses, пока сервер не начнёт выдавать их явно.
