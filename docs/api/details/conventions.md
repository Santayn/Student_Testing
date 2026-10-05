# Общие соглашения API

## Base URL

```text
/api/v1
```

Production frontend обращается к API относительно текущего origin. Полный внешний hostname не является частью прикладного контракта.

## Формат данных

Основные request/response body используют JSON:

```http
Content-Type: application/json
Accept: application/json
```

Исключения: multipart upload/restore и бинарные download/backup ответы.

## Идентификаторы

Большинство доменных сущностей используют числовой `Integer` ID. Вопросы, варианты и ответы тестирования местами используют `Long`. Path parameter всегда должен восприниматься как идентификатор конкретного доменного ресурса, а не как порядковый номер в UI.

## Даты и время

| Java-модель | JSON-представление |
|---|---|
| `Instant` | ISO-8601 UTC timestamp, например `2026-10-05T15:30:00Z` |
| `LocalDate` | `YYYY-MM-DD` |
| `LocalTime` | строка локального времени, например `00:30:00` |

Поля `availableFromUtc`, `availableUntilUtc`, `startedAt`, `completedAt` и аналогичные должны трактоваться как UTC timestamps.

## Списковые ответы

Текущий API возвращает обычные JSON-массивы. Унифицированной серверной пагинации (`page`, `size`, `offset`, `limit`) сейчас нет.

## Создание ресурсов

HTTP status создания пока не полностью унифицирован. Например `/auth/register` и `/topics` возвращают `201 Created`, тогда как многие другие `POST` CRUD-операции возвращают `200 OK`. Клиенты не должны автоматически предполагать `201` для каждого create endpoint.

## Удаление

Большинство `DELETE` и некоторые command endpoints используют `204 No Content`. Точный статус указан в [индексе endpoint'ов](./endpoint-index.md).

## Фильтры

GET-списки активно используют query parameters (`subjectId`, `personId`, `status`, `activeOnly` и др.). Отсутствующий parameter обычно означает более широкий запрос, однако object-level authorization может дополнительно требовать конкретный context parameter у преподавателя.

## Ошибки

Ошибки backend нормализуются в `ErrorResponse`. Подробности приведены в [errors.md](./errors.md).
