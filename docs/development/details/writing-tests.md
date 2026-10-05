# Написание тестов

## Frontend

Выбирать уровень по риску:

- pure utility test;
- composable test;
- component test;
- API/architecture contract;
- race/cancellation regression.

Тесты размещаются по домену в `src/__tests__/`.

## Backend

- plain JUnit — чистая логика;
- Mockito — изолированный Service;
- Spring integration — security/repository/transaction;
- PostgreSQL integration — нужен для DB-specific поведения, но пока отсутствует.

## Bugfix

Исправление регрессии должно оставить тест, воспроизводящий исходную проблему.

## API

Проверять route/method, payload, response sanitization и access matrix.

## UI

При затрагивании shared UI проверить mobile/dark/accessibility contract.

Подробная стратегия находится в [docs/testing](../../testing/README.md).
