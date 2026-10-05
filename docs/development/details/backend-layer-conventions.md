# Backend layer conventions

## Entity

- использовать существующий ID type домена (`Integer` или `Long`);
- сохранять quoted database identifiers;
- явно определять nullable/FK semantics;
- не возвращать Entity напрямую из REST, если домен использует DTO.

## Repository

Repository отвечает за запросы, например `findBy...`, `existsBy...`, `findForUpdate...`.

Orchestration вроде «создать тест и назначить нескольким группам» принадлежит Service.

## Service

Service должен:

- проверять business invariants;
- управлять transactions;
- координировать repositories;
- выдавать семантические exceptions;
- тестироваться отдельно от HTTP.

## Exceptions

Предпочтительно:

| Ситуация | Механизм |
|---|---|
| not found | `ResourceNotFoundException` |
| conflict | conflict exception |
| forbidden | `AccessDeniedException` |
| field/input | Bean Validation / осмысленный bad request |

Не использовать `IllegalArgumentException` как универсальный бизнес-протокол.

## Controller

При новом route проверить SecurityConfig, ownership, OpenAPI, tests, frontend adapter и API docs.

## Mapping

Если response уже принадлежит `ApiResponses`, mapper лучше держать рядом с существующими mapper-функциями.
