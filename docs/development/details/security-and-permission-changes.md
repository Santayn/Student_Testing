# Security и permission changes

## Два уровня

```text
SecurityConfig
→ request-level access

CurrentUserAccessService
→ object-level ownership
```

Новая операция должна пройти оба уровня, если предметный объект имеет владельца/контекст.

## Добавление permission

Типичный путь:

```text
bootstrap/DataLoader
→ Permission
→ RolePermission
→ SecurityConfig
→ ownership check
→ tests
→ docs
```

Admin UI получает permissions динамически, поэтому frontend constant для каждой permission обычно не нужен.

## Новая workspace role

Это гораздо более крупное изменение. Frontend жёстко знает `STUDENT`, `TEACHER`, `ADMIN` через route meta, workspace labels/priority, navigation, guards, role switcher и tests.

## Student contract

Student response не должен раскрывать:

- `correctAnswer`;
- `option.correct`;
- чужие results;
- management-only metadata.

## Regression

Security change должен проверять и разрешённый, и запрещённый соседний сценарий.
