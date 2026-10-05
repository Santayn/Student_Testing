# Ошибки REST API

## Единый ErrorResponse

Типичная ошибка:

```json
{
  "code": "validation_failed",
  "message": "Validation error",
  "details": [
    { "field": "name", "issue": "must not be blank" }
  ],
  "traceId": "..."
}
```

`traceId` позволяет связать сообщение клиента с конкретной ошибкой, хотя полноценная централизованная correlation logging инфраструктура пока не реализована.

## Основные backend codes

```text
validation_failed
bad_request
unauthorized
forbidden
not_found
conflict
payload_too_large
database_backup_failed
internal_error
```

## Основные HTTP статусы

| HTTP | Смысл |
|---:|---|
| `400` | невалидный request / business input |
| `401` | authentication отсутствует/недействительна |
| `403` | недостаточно прав или object access |
| `404` | ресурс не найден |
| `409` | конфликт с существующими данными/ограничениями |
| `413` | слишком большой payload |
| `500` | внутренняя ошибка |
| `503` | сервис временно недоступен, например readiness/backup dependency |

## Несоответствие frontend error dictionary

Frontend дополнительно знает коды вроде:

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

Текущий backend их системно не возвращает. Например FK/data-integrity конфликт обычно нормализуется к `code: "conflict"`.

## 429

Frontend готов обработать `429 Too Many Requests` и `Retry-After`, но rate limiting backend в текущей реализации не обнаружен. Это подготовленный клиентский контракт, а не гарантия сервера.
