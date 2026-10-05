# Добавление backend endpoint

## До кода

Зафиксировать:

```text
METHOD
PATH
AUTH
ROLE/PERMISSION
OWNERSHIP
REQUEST
SUCCESS RESPONSE
ERRORS
```

## Шаги

1. Добавить/расширить Service.
2. Добавить repository query, только если существующих недостаточно.
3. Создать request record с Bean Validation.
4. Определить response DTO.
5. Добавить request-level security в `SecurityConfig`.
6. Добавить object-level ownership check.
7. Обновить `OpenApiConfig`, если route требует ручного описания.
8. Добавить backend tests.
9. Добавить вызов в `frontend/src/api/`.
10. Обновить `docs/api/`.

## Минимальные тесты

- happy path;
- validation;
- forbidden/ownership;
- domain conflict, если он возможен.

## Student/public endpoint

Отдельно проверить, что response не содержит management-only данных и правильных ответов.

## Definition of Done

Endpoint без security/test/client-contract проверки не считается завершённым.
