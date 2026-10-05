# Frontend testing

Frontend использует **Vitest 4**, **Vue Test Utils 2** и окружение **jsdom**. Тесты расположены в `frontend/src/__tests__/` и группируются по защищаемому продуктово-доменному контексту, а не по типу исходного файла.

## Структура

| Раздел | Spec-файлов | `it/test` | `it/test.each` | Деклараций всего |
|---|---:|---:|---:|---:|
| `admin` | 38 | 162 | 0 | 162 |
| `architecture` | 13 | 58 | 2 | 60 |
| `auth` | 8 | 33 | 1 | 34 |
| `core` | 8 | 31 | 0 | 31 |
| `navigation` | 8 | 49 | 0 | 49 |
| `profile` | 2 | 8 | 0 | 8 |
| `results` | 9 | 31 | 0 | 31 |
| `student` | 14 | 51 | 4 | 55 |
| `teacher` | 39 | 197 | 0 | 197 |
| `ui` | 27 | 112 | 1 | 113 |

Всего: **166 spec-файлов** и **740 test declarations**. Параметризованные декларации могут создавать более одного runtime test case.

## Vitest configuration

`frontend/vitest.config.js`:

- объединяет Vite config и test config;
- использует `jsdom` по умолчанию;
- исключает `e2e/**`;
- использует корень `frontend/`;
- отдельные source-contract specs при необходимости выбирают Node environment через directive.

## Основные типы frontend-проверок

### Component interaction

Компоненты монтируются через `mount`/`shallowMount`, имитируются пользовательские действия, проверяется DOM и состояние.

### API contracts

Mock HTTP/API modules проверяют точный route, method, payload, multipart fields, timeout и response sanitization.

### Race/stale-context regression

Отдельно защищаются поздние ответы, отмена запросов, route changes и смена security context.

### UI contracts

Theme, responsive, touch targets, accessibility metadata и layout rules частично проверяются runtime, частично source-contract тестами.

### Architecture contracts

Тесты в `architecture/` защищают границы модулей, stale context, security assumptions и form lifecycle.

## Что frontend tests не делают

- не запускают реальный Chrome/Firefox/WebKit;
- не поднимают настоящий Spring Boot;
- не используют PostgreSQL;
- не выполняют screenshot comparison;
- не проводят axe/Pa11y audit;
- не измеряют code coverage threshold.

Подробный список spec-файлов: [frontend-test-catalog.md](./frontend-test-catalog.md).
