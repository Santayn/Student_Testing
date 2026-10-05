# Разработка обработки ошибок

## Backend

Единый `ErrorResponse` содержит machine-readable `code`, message, details и traceId.

Новая ошибка должна иметь осмысленный HTTP status и стабильный code, если frontend должен различать конфликт.

## Типовые статусы

| Ситуация | HTTP |
|---|---:|
| validation/bad request | 400 |
| authentication | 401 |
| authorization | 403 |
| not found | 404 |
| conflict | 409 |
| payload too large | 413 |
| unavailable dependency | 503 |
| unexpected | 500 |

## Frontend

Не показывать сырое backend exception message напрямую. Использовать существующий error presentation layer.

## Новый domain code

Если вводится специализированный code, нужно обновить backend handler, frontend mapping/fallback, tests и `docs/api/details/errors.md`.

Client validation улучшает UX, но не заменяет server validation.
