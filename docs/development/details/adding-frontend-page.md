# Добавление новой frontend-страницы

## 1. Workspace

Определить: public, STUDENT, TEACHER, ADMIN или dev-only.

## 2. View

Создать route-level View. Использовать `@/components/ui`; сложную API/state logic вынести в composable.

## 3. API adapter

Расширить существующий `src/api/<domain>.api.js` или создать доменный модуль и export через `src/api/index.js`.

## 4. Composable

Для сложного workflow вынести load/filter state, mutations/forms, race guards и error presentation.

## 5. Route

Добавить route в соответствующий файл:

```text
router/routes/public.js
router/routes/student.js
router/routes/teacher.js
router/routes/admin.js
router/routes/dev.js
```

Проверить meta:

```text
requiresAuth
roles
workspaceRoles
navKey
breadcrumbKey
```

## 6. Navigation

Если страница рабочая, обновить sidebar/breadcrumb config.

## 7. Tests

Проверить role visibility, route guard, API contract, loading/error и race behavior при cascading requests.

## 8. Static quality

Новый production module должен быть достижим от `main.js`.
