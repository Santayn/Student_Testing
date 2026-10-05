# Академическая структура

CRUD API для факультетов, групп и предметов. Эти справочники образуют основу административной настройки учебного процесса.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/faculties` | Получить список ресурсов | Authenticated read / ADMIN or academic.manage write | `200` | `faculties.api.js` |
| `GET` | `/api/v1/faculties/{id}` | Получить ресурс по идентификатору | Authenticated read / ADMIN or academic.manage write | `200` | `faculties.api.js` |
| `GET` | `/api/v1/faculties/{id}/subjects` | Получить связанные предметы | Authenticated read / ADMIN or academic.manage write | `200` | `faculties.api.js` |
| `POST` | `/api/v1/faculties` | Создать ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `faculties.api.js` |
| `PUT` | `/api/v1/faculties/{id}` | Изменить ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `faculties.api.js` |
| `DELETE` | `/api/v1/faculties/{id}` | Удалить ресурс | Authenticated read / ADMIN or academic.manage write | `204` | `faculties.api.js` |
| `POST` | `/api/v1/faculties/{facultyId}/subjects/{subjectId}` | Связать факультет и предмет | Authenticated read / ADMIN or academic.manage write | `204` | `faculties.api.js` |
| `DELETE` | `/api/v1/faculties/{facultyId}/subjects/{subjectId}` | Удалить связь факультета и предмета | Authenticated read / ADMIN or academic.manage write | `204` | `faculties.api.js` |
| `GET` | `/api/v1/groups` | Получить список ресурсов | Authenticated read / ADMIN or academic.manage write | `200` | `groups.api.js` |
| `GET` | `/api/v1/groups/{id}` | Получить ресурс по идентификатору | Authenticated read / ADMIN or academic.manage write | `200` | `groups.api.js` |
| `POST` | `/api/v1/groups` | Создать ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `groups.api.js` |
| `PUT` | `/api/v1/groups/{id}` | Изменить ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `groups.api.js` |
| `DELETE` | `/api/v1/groups/{id}` | Удалить ресурс | Authenticated read / ADMIN or academic.manage write | `204` | `groups.api.js` |
| `GET` | `/api/v1/subjects` | Получить список ресурсов | Authenticated read / ADMIN or academic.manage write | `200` | `subjects.api.js` |
| `GET` | `/api/v1/subjects/{id}` | Получить ресурс по идентификатору | Authenticated read / ADMIN or academic.manage write | `200` | `subjects.api.js` |
| `GET` | `/api/v1/subjects/{id}/faculties` | Получить связанные факультеты | Authenticated read / ADMIN or academic.manage write | `200` | — |
| `POST` | `/api/v1/subjects` | Создать ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `subjects.api.js` |
| `PUT` | `/api/v1/subjects/{id}` | Изменить ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `subjects.api.js` |
| `DELETE` | `/api/v1/subjects/{id}` | Удалить ресурс | Authenticated read / ADMIN or academic.manage write | `204` | `subjects.api.js` |

## Основные request contracts

**Faculty**

```json
{ "name": "...", "code": "...", "description": "..." }
```

**Group**

```json
{ "name": "...", "code": "...", "facultyId": 1 }
```

**Subject**

```json
{ "name": "...", "description": "..." }
```

## Faculty ↔ Subject

Связь управляется command endpoints:

```text
POST   /faculties/{facultyId}/subjects/{subjectId}
DELETE /faculties/{facultyId}/subjects/{subjectId}
```

Для чтения доступны оба направления: `/faculties/{id}/subjects` и `/subjects/{id}/faculties`.

## Доступ

Чтение академических справочников доступно аутентифицированным пользователям. Запись требует ADMIN или `academic.manage`.
