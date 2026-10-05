# Изменение API-контракта

## Общая цепочка

```text
Controller request/response
→ OpenAPI
→ frontend api module
→ composable/view
→ tests
→ docs/api
```

## URL/метод

При изменении найти все usage в frontend API и tests. Не оставлять случайные legacy routes без явного решения.

## Поле response

Проверить DTO mapper, security/privacy, frontend sanitizer, nullability, compatibility и docs.

Student contract нельзя строить на management DTO, если там есть правильные ответы или чужие данные.

## Numeric values

Statuses/types/scopes часто передаются числами. Новое значение требует синхронизации backend mapping, frontend labels, API `contract-values` и tests.

## Endpoint index

Любой новый/удалённый route должен попасть в `docs/api/details/endpoint-index.md`.
