# Health и readiness

## PostgreSQL

Compose использует:

```text
pg_isready
```

Это проверяет, что PostgreSQL принимает подключения.

## Backend readiness

```http
GET /api/v1/status/readiness
```

Backend выполняет реальный:

```sql
SELECT 1
```

и при успехе возвращает HTTP `200` с состоянием `ready`. При недоступной БД возвращается `503`.

Проверка с host:

```bash
curl http://127.0.0.1:8080/api/v1/status/readiness
```

если `BACKEND_PORT` не изменён.

## Frontend health

Nginx предоставляет:

```http
GET /health
```

Проверка:

```bash
curl http://localhost/health
```

Ответ `ok` подтверждает работу Nginx, но не подтверждает работоспособность backend.

## Что readiness не проверяет

Текущий backend readiness **не проверяет**:

- доступность/writable-состояние `lecture-uploads`;
- свободное место на диске;
- наличие `pg_dump` и `psql`;
- совместимость схемы БД с приложением;
- Local LLM;
- frontend;
- корректность demo/system seed.

Поэтому health endpoint — базовый сигнал, а не полная функциональная диагностика.

## Unhealthy и restart

`restart: unless-stopped` не является health-based self-healing. Если процесс backend продолжает работать, но Docker помечает контейнер `unhealthy`, обычный Docker Compose сам по себе не обязан его перезапустить.

Оператор должен:

1. проверить логи;
2. устранить причину;
3. при необходимости выполнить `docker compose restart <service>`.
