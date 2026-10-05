# Routing и navigation

## Route groups

Маршруты разделены на public, student, teacher, admin и dev. Dev routes подключаются только в development mode.

## Metadata

Новая страница может требовать:

```text
requiresAuth
roles
workspaceRoles
navKey
breadcrumbKey
```

## Workspace

Frontend знает три режима: `STUDENT`, `TEACHER`, `ADMIN`. Многоролевой аккаунт переключает активный workspace; navigation должна учитывать именно его.

## Sidebar

Sidebar конфигурируется централизованно. Не создавать hardcoded menu внутри отдельной View.

## Breadcrumb

Рабочий route должен иметь корректный breadcrumb context, включая dynamic labels при необходимости.

## Tests

Изменение navigation требует проверить role access, direct URL, workspace switching, sidebar, breadcrumb и mobile drawer.
