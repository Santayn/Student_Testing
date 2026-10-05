# Backend development

## Слои

```text
REST Controller
      ↓
Service
      ↓
Repository
      ↓
JPA Entity
      ↓
PostgreSQL
```

Для защищённых предметных объектов между HTTP и бизнес-операцией добавляется `CurrentUserAccessService`.

## Entity

Entity отражает persistent model. Имена таблиц/колонок часто явно quoted и совместимы с существующей схемой. Не переименовывать legacy identifiers без отдельной database-change задачи.

## Repository

Repository описывает persistence queries. Используются derived queries, JPQL и pessimistic locks.

Бизнес-процесс должен оставаться в Service.

## Service

Service — главный слой бизнес-правил и транзакций.

Read обычно помечается `@Transactional(readOnly = true)`, mutation — `@Transactional`.

## Controller

Controller должен оставаться тонким:

```text
input
→ validation
→ access check
→ service
→ DTO
→ HTTP response
```

## DTO

Фактическая конвенция проекта:

- request records часто вложены в controller;
- основные response records находятся в `ApiResponses`;
- специализированные student/result DTO могут быть рядом с controller.

## Concurrency

Для attempt и других конкурентных flows нужно проектировать одновременно transaction boundary, lock и DB constraint, а не полагаться только на `if` в Java.
