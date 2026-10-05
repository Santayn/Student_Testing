# Целевая E2E strategy

В текущем проекте нет browser E2E. Рекомендуемый следующий уровень — Playwright поверх disposable PostgreSQL environment.

## Целевая схема

```text
PostgreSQL 16 disposable
        ↓
backend
        ↓
frontend/Nginx
        ↓
Playwright browser
```

## Почему PostgreSQL обязателен для E2E

H2 уже используется для быстрых backend integration tests. E2E должен закрывать другой класс рисков:

- production DDL/schema;
- PostgreSQL constraints/indexes;
- real transaction behavior;
- Nginx proxy;
- actual JWT over HTTP;
- frontend API adapters;
- browser routing/session storage.

## Минимальный набор E2E сценариев

### Authentication

- login/logout;
- multi-role workspace switch;
- expired session/refresh при возможности стабильно смоделировать.

### STUDENT

```text
login
→ subject
→ lecture
→ start test
→ answer
→ submit
→ result
```

Отдельный test:

```text
start
→ answer partially
→ page reload/navigation
→ draft restored
→ continue
```

### TEACHER

```text
login
→ create topic/question
→ create test
→ assign group
→ create/edit lecture
→ inspect results
```

### ADMIN

```text
login
→ create/bind user/person
→ assign role
→ group membership
→ teaching assignment
```

Restore database лучше не включать в основной E2E suite; его следует тестировать в отдельной disposable infrastructure job.

## Fixtures

E2E не должен зависеть от runtime demo DataLoader. Перед suite создаётся минимальный test bootstrap, после suite уничтожается БД/stack целиком.

## Артефакты при падении

Рекомендуется сохранять:

- screenshot;
- trace;
- browser console;
- network log;
- backend logs;
- container health/status.

## Visual/accessibility extension

После базового functional E2E можно добавить:

- Playwright screenshot regression для нескольких ключевых страниц;
- axe-core scan основных STUDENT/TEACHER/ADMIN screens.
