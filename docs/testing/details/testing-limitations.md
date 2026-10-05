# Ограничения тестирования

## Высокий приоритет

### Нет PostgreSQL integration suite

Backend integration tests используют H2. Не проверяются production-specific DDL, constraints, partial indexes, extensions и точная lock semantics.

### `schema.sql` исключён из JUnit lifecycle

Test profile задаёт `spring.sql.init.mode=never` и `ddl-auto=create-drop`, поэтому ошибки ручного schema patching могут оставаться незамеченными.

### Нет browser E2E

Frontend и backend проверяются раздельно; отсутствует автоматический тест всей цепочки через реальный браузер.

## Средний приоритет

### System smoke не синхронизирован с API

Текущий static audit показывает published API operations, для которых нет прямого smoke-вызова, при том что script ожидает полный охват OpenAPI.

### Smoke зависит от DataLoader

Известные demo credentials и существующее demo environment являются скрытой fixture dependency.

### Smoke загрязняет БД

Нет полного isolated environment cleanup после системного прогона.

### CI отсутствует

Quality gates не являются обязательными для каждого push/merge.

### JWT lifecycle покрыт частично

MockMvc tests часто инжектируют Authentication напрямую и не проверяют production Bearer processing path.

## Низкий/эволюционный приоритет

### Нет formal coverage

Не настроены Vitest coverage и JaCoCo.

### UI contracts частично source-based

Проверка CSS/ARIA tokens в исходнике полезна, но не равна реальному browser rendering или accessibility engine.

### Нет visual regression

Изменение визуального вида не сравнивается screenshot-to-screenshot.

### Нет performance/load tests

Не измеряются response latency, concurrent users, question-bank scale или большие result datasets.

### Нет security dependency/scanning pipeline

Отсутствует автоматизированный dependency/security scan в CI.

## Приоритет развития

Рекомендуемая последовательность:

1. PostgreSQL/Testcontainers integration для schema + attempts;
2. обязательный CI frontend/backend;
3. исправление API smoke и disposable fixtures;
4. Playwright critical-path E2E;
5. JWT lifecycle integration;
6. coverage reports;
7. accessibility/visual regression;
8. performance/security automation по мере необходимости.
