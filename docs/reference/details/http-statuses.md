# HTTP-статусы

| HTTP | Использование в проекте |
|---:|---|
| `200 OK` | Успешное чтение и многие create/update операции. |
| `201 Created` | Часть create endpoint'ов, например регистрация и отдельные ресурсы. |
| `204 No Content` | Некоторые delete/revoke операции без body. |
| `400 Bad Request` | Невалидный request или бизнес-валидация. |
| `401 Unauthorized` | Нет/невалидна authentication. |
| `403 Forbidden` | Нет роли/permission/ownership. |
| `404 Not Found` | Ресурс не найден. |
| `409 Conflict` | Конфликт данных или состояния. |
| `413 Payload Too Large` | Превышены multipart limits. |
| `500 Internal Server Error` | Внутренняя ошибка backend. |
| `503 Service Unavailable` | Readiness при недоступной БД или ошибка backup/restore service. |

Frontend умеет представлять `429 Too Many Requests`, но текущий backend не содержит собственного rate limiter, поэтому `429` не является гарантированным штатным ответом.

Коды успешного создания в API пока не полностью унифицированы: не следует автоматически предполагать `201` для каждого `POST`.
