# Regression strategy

Проектная test suite исторически ориентирована на предотвращение повторного появления уже найденных дефектов. Для Student Testing это оправдано: большая часть сложных ошибок связана не с чистыми функциями, а с контекстом пользователя, асинхронностью и бизнес-ограничениями.

## Главные классы регрессий

### Security regression

- STUDENT не должен получить teacher/admin API;
- student должен видеть только свои результаты;
- owner/context checks должны выполняться server-side;
- student DTO не должен раскрывать правильные ответы.

### Attempt regression

- metadata GET не расходует попытку;
- start выполняется через assignment;
- resume не создаёт лишнюю попытку;
- draft относится к конкретной попытке;
- limit считается по правильному business key;
- конкурентный start не создаёт две активные попытки.

### Stale context / race regression

- поздний response старого route/filter не заменяет актуальный state;
- logout/login меняет security epoch;
- старый user response не попадает в новую сессию;
- AbortController/latest-request guards работают при быстрых переключениях.

### Teacher/admin workflow regression

- subject/group context корректно обновляется;
- dependent filters сбрасываются;
- multi-step test creation не оставляет inconsistent UI state;
- membership status transitions работают ожидаемо.

### UI regression

- dark/light theme contract;
- mobile drawer/focus behavior;
- touch targets;
- responsive layout;
- ARIA metadata.

## Правило добавления regression test

Если обнаружен дефект, который можно воспроизвести автоматически, исправление считается неполным без теста, фиксирующего исходную ошибку или её инварианту.

Предпочтительно тестировать **наблюдаемое правило**, а не конкретную внутреннюю реализацию. Исключение — архитектурные source-contract tests, которые специально защищают выбранное техническое ограничение.
