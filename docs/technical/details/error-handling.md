# Обработка ошибок

## Backend

Ошибки REST API централизуются через controller advice и приводятся к единому response-формату.

Концептуально response содержит:

```json
{
  "code": "...",
  "message": "...",
  "details": "...",
  "traceId": "..."
}
```

Точная форма конкретных endpoint'ов должна считаться API-контрактом и документироваться в `docs/api/`.

## Основные HTTP-классы ошибок

Backend различает типовые случаи:

| HTTP | Назначение |
|---:|---|
| 400 | невалидные данные / бизнес-валидация |
| 401 | authentication отсутствует или недействительна |
| 403 | недостаточно прав |
| 404 | ресурс не найден |
| 409 | конфликт текущего состояния |
| 413 | превышен размер upload |
| 500 | непредвиденная серверная ошибка |
| 503 | временно недоступная зависимость/операция |

## Security errors

`SecurityConfig` явно формирует JSON-ответ для unauthenticated и access denied случаев, чтобы security filter chain не возвращал несогласованный HTML/default response.

## Frontend normalization

Frontend API-слой и auth utilities преобразуют backend/network errors в понятные сообщения для feature UI.

Важно различать:

- server validation;
- authorization;
- network failure;
- timeout;
- stale session operation;
- отменённый request;
- неопределённый outcome опасной mutation.

## `traceId`

`traceId` помогает сопоставить ошибку интерфейса с backend-событием. На текущем этапе полноценная централизованная structured logging/correlation infrastructure не выделена, поэтому `traceId` полезен, но не заменяет полноценную observability-систему.

## Не полагаться на текст сообщения как контракт

Frontend по возможности должен использовать стабильный error `code`, а не парсить человеческий `message`.

В аудите обнаружены места, где frontend ожидает более специализированные коды ошибок, чем backend гарантирует сейчас. Это отмечено в [technical-limitations.md](./technical-limitations.md).
