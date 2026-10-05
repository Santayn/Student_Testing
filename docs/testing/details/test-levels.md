# Уровни тестирования

## 1. Static quality

Frontend содержит отдельный Node-скрипт `frontend/scripts/static-quality.mjs`. Он работает без запуска приложения и проверяет структуру production-кода.

```text
source files
   ↓
forbidden patterns
   ↓
internal imports
   ↓
reachability from main.js
   ↓
unused exported symbols
```

Это самый быстрый архитектурный gate.

## 2. Unit tests

### Frontend

Vitest используется для чистых функций, API adapters, state/composable logic и небольших компонентных контрактов.

### Backend

JUnit 5 + Mockito используются для алгоритмов и сервисов без полного Spring context: password hashing, text grading, DOCX parser, object access helpers, attempt-limit logic.

## 3. Component / feature tests frontend

Vue Test Utils + jsdom монтируют компоненты и проверяют сценарии взаимодействия без настоящего браузера и backend.

Пример границы:

```text
Vue component
   ↓
mocked API/composable
   ↓
DOM assertions in jsdom
```

## 4. Source-contract tests frontend

Часть test suite читает production source через `readFileSync` и проверяет наличие/отсутствие определённых конструкций. Это полезно для архитектурных и UI-contract правил, но не заменяет runtime browser behavior.

## 5. Backend integration/security tests

`@SpringBootTest` поднимает Spring context и использует профиль `test` с H2. Здесь проверяются DataLoader, MockMvc security, student-learning boundaries и concurrency logic.

## 6. API/system smoke

`backend/scripts/full-system-smoke.ps1` работает уже против запущенного backend по HTTP и проходит крупный CRUD/security/learning workflow.

Это системная API-проверка, но не browser E2E.

## 7. Отсутствующий browser E2E

В проекте пока нет слоя:

```text
real browser
   ↓
Vue production build
   ↓
Nginx
   ↓
Spring Security
   ↓
PostgreSQL
```

Целевой вариант описан в [e2e-strategy.md](./e2e-strategy.md).
