# Тестирование попыток и конкурентности

Attempt lifecycle является одной из наиболее критичных бизнес-областей проекта и имеет отдельные frontend/backend regression tests.

## Backend: attempt limit

`TestServiceAttemptLimitTests` фиксирует правило:

```text
лимит попыток считается по Test + Person
```

а не независимо для каждого `TestAssignment`.

Также test проверяет наличие `PESSIMISTIC_WRITE` на repository methods, используемых при создании попытки.

## Backend: concurrency

`TestServiceConcurrencyIntegrationTests` запускает два конкурентных `startAttempt()` при разрешённой одной попытке.

Ожидаемая инварианта:

```text
Thread A ─┐
          ├─ startAttempt → только 1 TestAttempt
Thread B ─┘
```

Один поток должен завершиться успешно, второй — получить ошибку бизнес-ограничения; в БД должна существовать одна попытка.

## Ограничение

Integration test выполняется на H2, поэтому он не полностью доказывает эквивалентное поведение PostgreSQL pessimistic locks/partial unique index.

Рекомендуется добавить Testcontainers PostgreSQL test, который проверит конкурентный старт уже на production-compatible engine.

## Frontend attempt regression

Frontend дополнительно защищает:

- start/resume flow;
- draft persistence;
- reload;
- route change;
- submit behavior;
- stale response/race cases;
- student-result visibility.

Таким образом server attempt является источником истины, а frontend tests защищают UX lifecycle вокруг него.
