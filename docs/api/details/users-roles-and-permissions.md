# Пользователи, Person, роли и permissions

API разделяет учётную запись `User` и академический профиль `Person`. Роли/permissions хранятся у User, а имя, дата рождения, email и телефон — у Person.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/users` | Получить пользователей | ADMIN / users.read | `200` | `users.api.js` |
| `GET` | `/api/v1/users/{id}` | Получить пользователя | ADMIN / users.read | `200` | — |
| `GET` | `/api/v1/users/me` | Получить сведения о текущем пользователе | Authenticated | `200` | `users.api.js` |
| `GET` | `/api/v1/users/people` | Получить профили Person | ADMIN/TEACHER or people/users permission | `200` | `users.api.js` |
| `GET` | `/api/v1/users/people/{personId}` | Получить профиль Person | ADMIN/TEACHER or people/users permission | `200` | `users.api.js` |
| `POST` | `/api/v1/users/people` | Создать профиль Person | ADMIN/TEACHER or people/users permission | `200` | `users.api.js` |
| `PUT` | `/api/v1/users/people/{personId}` | Изменить профиль Person | ADMIN/TEACHER or people/users permission | `200` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/active` | Изменить активность ресурса | ADMIN / users.write | `200` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/person` | Привязать User к Person | ADMIN / roles.manage | `204` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/roles` | Заменить роли пользователя | ADMIN / roles.manage | `200` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/permissions` | Заменить прямые permissions пользователя | ADMIN / roles.manage | `200` | — |
| `GET` | `/api/v1/roles` | Получить список ресурсов | ADMIN / roles.manage | `200` | `roles.api.js` |
| `POST` | `/api/v1/roles` | Создать ресурс | ADMIN / roles.manage | `200` | `roles.api.js` |
| `GET` | `/api/v1/roles/permissions` | Получить permissions | ADMIN / roles.manage | `200` | `roles.api.js` |
| `POST` | `/api/v1/roles/permissions` | Создать permission | ADMIN / roles.manage | `200` | `roles.api.js` |
| `PUT` | `/api/v1/roles/{id}/permissions` | Заменить permissions роли | ADMIN / roles.manage | `200` | `roles.api.js` |

## Person request

```json
{
  "firstName": "Иван",
  "lastName": "Иванов",
  "dateOfBirth": "2000-01-15",
  "email": "ivan@example.com",
  "phone": "+7..."
}
```

`firstName`/`lastName` — до 100 символов, `email` обязателен и валидируется.

## User response

```text
id, login, active, personId, roles[], permissions[]
```

## Управление связями

- `PUT /users/{id}/person` связывает аккаунт с существующим `Person`;
- `PUT /users/{id}/roles` заменяет набор role IDs;
- `PUT /users/{id}/permissions` заменяет набор прямых permission IDs;
- `PUT /users/{id}/active` активирует/деактивирует аккаунт.

## Role API

Role response включает вложенный список permissions. Отдельных update/delete endpoint'ов роли и permission в текущем API нет.

## Дублирование current user

Одновременно существуют `/auth/me` и `/users/me`. Основной frontend auth store использует `/auth/me`.
