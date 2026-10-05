# CI и quality gates

В текущем проекте нет `.github/workflows`, GitLab CI, Jenkinsfile или другого автоматического pipeline. Проверки зависят от ручного запуска разработчиком.

## Минимальный обязательный CI

### Frontend job

```text
checkout
  ↓
setup supported Node
  ↓
npm ci
  ↓
npm run quality
```

### Backend job

```text
checkout
  ↓
setup Java 17
  ↓
mvn test
```

## Рекомендуемый PostgreSQL integration job

После добавления Testcontainers или отдельного service container:

```text
PostgreSQL 16
  ↓
schema/migrations
  ↓
integration tests
```

Он должен быть отдельным от быстрых H2 tests, чтобы сохранить короткий feedback loop.

## Рекомендуемый E2E job

```text
build/start disposable stack
  ↓
seed minimal fixtures
  ↓
Playwright critical paths
  ↓
collect logs/screenshots
  ↓
destroy stack
```

## Docker build

Docker build не заменяет tests. Backend Dockerfile выполняет package с `-DskipTests`, а frontend image выполняет production build, но не полный `npm run quality`.

Поэтому правильный pipeline:

```text
quality gates
   ↓
только после успеха
   ↓
container build
```

## Pull request gate

Для merge рекомендуется требовать минимум:

- frontend quality;
- backend tests;
- PostgreSQL integration после её добавления;
- critical E2E после появления Playwright;
- отсутствие изменений API без обновления API docs/tests.
