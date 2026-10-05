# Логи и диагностика

## Основной источник логов

Приложение пишет в stdout/stderr контейнеров. Основные команды:

```bash
docker compose logs
```

Последние строки:

```bash
docker compose logs --tail=200 backend
```

Поток в реальном времени:

```bash
docker compose logs -f backend
```

Аналогично:

```bash
docker compose logs -f postgres
docker compose logs -f frontend
```

## Рекомендуемый порядок диагностики

```text
1. docker compose ps
2. postgres health/log
3. backend readiness/log
4. frontend /health/log
5. воспроизвести конкретный сценарий
6. проверить HTTP status и ErrorResponse
```

## Trace ID

API errors содержат `traceId`, но текущая реализация генерирует его в `ApiExceptionHandler` и не связывает системно с MDC/логами.

Следовательно `traceId` пока нельзя считать полноценным correlation identifier для поиска stack trace в контейнерных логах.

## Ограничение application logging

Проект не имеет полноценного audit/event logging для действий вроде:

```text
role changed
user disabled
database restore started
backup created
teaching assignment changed
```

Также нет централизованного лог-сервиса, retention policy или явно настроенной Docker log rotation.

## Что сохранять при инциденте

Перед destructive recovery желательно сохранить:

```bash
docker compose ps > compose-status.txt
docker compose logs --no-color > compose.log
```

и отдельно зафиксировать:

- время проблемы;
- URL/действие;
- HTTP status;
- `traceId`, если был;
- версию исходников/image;
- текущий `.env` без передачи секретов третьим лицам.
