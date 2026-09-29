# Архитектурные рекомендации — Student Testing

Дата создания: 29.09.2026

Этот файл содержит рекомендации, которые нельзя корректно отнести только к frontend или только к backend. Они затрагивают несколько слоёв системы, инфраструктуру разработки, тестовые окружения или общие архитектурные инварианты.

Статусы:

- **OPEN** — рекомендация актуальна и не реализована;
- **PARTIAL** — реализована частично;
- **DEFERRED** — осознанно отложена до выполнения указанного prerequisite;
- **DONE** — полностью реализована и подтверждена тестами/инфраструктурой.

---

## ARCH-01 — Воспроизводимая E2E test infrastructure

**Приоритет:** P2  
**Статус:** DEFERRED

### Проблема

Frontend имеет сильное unit/contract покрытие и static quality gate, однако полноценного browser-level E2E слоя нет.

Добавлять Playwright поверх текущего обычного seeder нецелесообразно: seeder планируется отключать, а реальные данные БД не являются стабильной тестовой фикстурой.

E2E, зависящий от ручного состояния БД, быстро становится нестабильным:

```text
данные существуют → тест зелёный
данные удалены/изменены → тест падает
пустая БД → сценарий невозможно выполнить
```

### Архитектурное требование

Перед полноценным E2E необходимо создать отдельный воспроизводимый test lifecycle:

```text
isolated E2E database
        ↓
database migrations
        ↓
E2E-only bootstrap / fixtures
        ↓
backend startup
        ↓
frontend startup
        ↓
Playwright smoke tests
        ↓
database cleanup / recreation
```

Обычный development/production seeder не должен являться зависимостью E2E.

### Требования к test-data/bootstrap

Механизм должен:

- работать только в тестовом окружении;
- не зависеть от существующих пользовательских данных;
- создавать предсказуемые аккаунты и роли;
- создавать минимально необходимый academic context;
- создавать student/teacher/admin сценарии;
- быть идемпотентным либо запускаться на чистой БД;
- позволять полностью пересоздать состояние перед прогоном;
- не включаться случайно в production.

### Минимальные E2E fixtures

Достаточно небольшого набора:

```text
ADMIN account
TEACHER account
STUDENT account

1 faculty
1 subject
1 student group
1 teacher subject membership
1 teaching assignment
1 topic
1 lecture
1 published test
1 test assignment
```

Дополнительные данные создаются только если конкретный smoke-flow их требует.

### Минимальный Playwright smoke-suite после реализации bootstrap

1. login / logout;
2. session survives page reload;
3. workspace role switching;
4. student: subject → lecture → test → submit → result;
5. reload незавершённой попытки;
6. один teacher CRUD flow;
7. один admin CRUD flow;
8. mobile sidebar/navigation.

Не требуется переносить все 600+ unit/contract tests в E2E.

### Что не считать полноценным решением

Полностью замоканный `page.route()` E2E может быть полезен для:

- focus management;
- browser navigation;
- mobile drawer;
- file/browser API edge cases.

Но он **не заменяет** реальный frontend ↔ backend E2E, потому что не проверяет:

- Spring Security;
- реальные REST contracts;
- serialization/deserialization;
- database constraints;
- migrations;
- transaction behavior.

### Условие возобновления

ARCH-01 следует переводить из `DEFERRED` в `OPEN`, когда проект получит возможность запускать отдельную тестовую БД и детерминированно заполнять её E2E-only fixtures независимо от обычного seeder.

До этого момента Playwright integration не является обязательным quality gate.

---

## Правило классификации архитектурных рекомендаций

В этот реестр следует помещать проблемы, которые требуют согласованного изменения нескольких слоёв системы, например:

- test environment / E2E infrastructure;
- cross-service transaction boundaries;
- deployment topology;
- shared observability;
- distributed cache/session strategy;
- общие data ownership правила;
- contract/versioning strategy;
- backup/recovery infrastructure;
- CI/CD architecture.

Локальные изменения одного слоя должны оставаться соответственно в frontend- или backend-рекомендациях.
