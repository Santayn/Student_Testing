# Membership API

Membership — отдельная сущность связи Person с факультетом, группой или предметом. В отличие от простой many-to-many связи она содержит роль, статус, timestamps и notes.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/memberships/faculties` | Получить membership факультетов | Authenticated read / ADMIN-authority write | `200` | — |
| `GET` | `/api/v1/memberships/faculties/{facultyId}` | Получить участников факультета | Authenticated read / ADMIN-authority write | `200` | — |
| `GET` | `/api/v1/memberships/faculties/memberships/{membershipId}` | Получить membership факультета | Authenticated read / ADMIN-authority write | `200` | — |
| `POST` | `/api/v1/memberships/faculties/{facultyId}` | Добавить человека на факультет | Authenticated read / ADMIN-authority write | `200` | — |
| `PUT` | `/api/v1/memberships/faculties/memberships/{membershipId}/status` | Изменить статус membership факультета | Authenticated read / ADMIN-authority write | `200` | — |
| `PUT` | `/api/v1/memberships/faculties/memberships/{membershipId}` | Изменить membership факультета | Authenticated read / ADMIN-authority write | `200` | — |
| `GET` | `/api/v1/memberships/groups` | Получить membership учебных групп | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |
| `POST` | `/api/v1/memberships/groups/{groupId}` | Добавить человека в группу | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |
| `GET` | `/api/v1/memberships/groups/{groupId}` | Получить membership учебных групп | Authenticated read / ADMIN-authority write | `200` | — |
| `GET` | `/api/v1/memberships/groups/memberships/{membershipId}` | Получить membership группы | Authenticated read / ADMIN-authority write | `200` | — |
| `PUT` | `/api/v1/memberships/groups/memberships/{membershipId}/status` | Изменить статус membership группы | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |
| `PUT` | `/api/v1/memberships/groups/memberships/{membershipId}` | Изменить membership группы | Authenticated read / ADMIN-authority write | `200` | — |
| `GET` | `/api/v1/memberships/subjects` | Получить membership предметов | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |
| `GET` | `/api/v1/memberships/subjects/{subjectId}` | Получить участников предмета | Authenticated read / ADMIN-authority write | `200` | — |
| `GET` | `/api/v1/memberships/subjects/memberships/{membershipId}` | Получить membership предмета | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |
| `POST` | `/api/v1/memberships/subjects/{subjectId}` | Добавить человека к предмету | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |
| `PUT` | `/api/v1/memberships/subjects/memberships/{membershipId}/status` | Изменить статус membership предмета | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |
| `PUT` | `/api/v1/memberships/subjects/memberships/{membershipId}` | Изменить membership предмета | Authenticated read / ADMIN-authority write | `200` | `memberships.api.js` |

## Request

```json
{
  "personId": 10,
  "role": 1,
  "notes": "..."
}
```

Status update:

```json
{ "status": 2 }
```

Full update:

```json
{ "status": 1, "notes": "..." }
```

## Фильтрация

Списковые endpoints принимают query parameters вроде `facultyId`, `groupId`, `subjectId`, `personId`, `status`, `activeOnly`.

## Контроль доступа

GET-маршруты проходят request-level `authenticated()`, но controller дополнительно ограничивает часть списков текущим Person либо административным контекстом. Изменение membership относится к административным операциям.

## Lifecycle

`status = 3` трактуется как снятие/удаление membership и приводит к заполнению `removedAtUtc`. Возврат к активному статусу очищает `removedAtUtc`.
