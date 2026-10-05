# OpenAPI и Swagger

## Доступные endpoints

При запущенном backend доступны:

```text
/swagger-ui.html
/swagger-ui/**
/v3/api-docs
```

Эти маршруты разрешены без authentication отдельной non-API security chain.

## Назначение

OpenAPI даёт интерактивную схему и позволяет изучать DTO/operations. Markdown-раздел `docs/api` дополняет Swagger сведениями, которые трудно выразить автоматически: ownership rules, student-vs-management boundaries, числовые status semantics и известные ограничения.

## Bearer scheme

`OpenApiConfig` объявляет JWT Bearer security scheme. Однако текущая схема не полностью отражает реальную endpoint-specific security matrix: наличие scheme не объясняет, какой route anonymous, где нужен ADMIN, где teacher permission и где object ownership.

## Ручные descriptions

`OpenApiConfig` содержит большой набор вручную заданных summary/description. По результатам аудита часть endpoint'ов всё ещё попадает в generic fallback. Особенно важно проверять новые routes для topics, materials, backup и прочих поддоменов.

## Error responses

Controller methods почти не описывают полный набор `@ApiResponse` для `400/401/403/404/409/...`, поэтому Swagger успешных DTO не заменяет документацию error contract.

## Поддержание актуальности

При добавлении endpoint следует обновлять mapping, DTO, security, OpenAPI metadata и `docs/api` одновременно.
