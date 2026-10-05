# System и health API

Служебные endpoint’ы используются для базовой диагностики приложения и Docker healthchecks.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/status` | Проверить состояние backend | Anonymous | `200` | — |
| `GET` | `/api/v1/status/readiness` | Проверить готовность backend и БД | Anonymous | `200` | — |

## `/status`

Проверяет доступность самого backend-процесса. Не требует authentication.

## `/status/readiness`

Проверяет готовность backend к обслуживанию трафика и выполняет реальную проверку соединения с PostgreSQL (`SELECT 1`). Именно readiness используется backend Docker healthcheck.

Frontend health endpoint `/health` относится к Nginx и не является частью Spring REST API.
