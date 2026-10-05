# API курсов

Управляет шаблонами курсов и их версиями. Версия курса — отдельный snapshot-like контекст для лекций и учебной нагрузки.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/courses/templates` | Получить шаблоны курсов | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |
| `GET` | `/api/v1/courses/templates/{templateId}` | Получить шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `200` | — |
| `POST` | `/api/v1/courses/templates` | Создать шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |
| `PUT` | `/api/v1/courses/templates/{templateId}` | Изменить шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |
| `DELETE` | `/api/v1/courses/templates/{templateId}` | Удалить шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `204` | `courses.api.js` |
| `PUT` | `/api/v1/courses/versions/{versionId}/publish` | Опубликовать версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |
| `PUT` | `/api/v1/courses/versions/{versionId}/unpublish` | Снять публикацию версии курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |
| `GET` | `/api/v1/courses/templates/{templateId}/versions` | Получить версии шаблона | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |
| `GET` | `/api/v1/courses/versions/{versionId}` | Получить версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | — |
| `POST` | `/api/v1/courses/templates/{templateId}/versions` | Создать версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |
| `PUT` | `/api/v1/courses/versions/{versionId}` | Изменить версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `courses.api.js` |

## CourseTemplate request

```json
{
  "subjectId": 5,
  "name": "Основной курс",
  "publicVisible": true
}
```

## CourseVersion request

```json
{
  "versionNumber": 2,
  "title": "2026",
  "description": "...",
  "published": false,
  "changeNotes": "..."
}
```

Update версии не принимает `published`; публикация и снятие публикации вынесены в отдельные command endpoints.

## Ownership

Преподаватель работает только с course context, который связан с доступным ему subject/author context. ADMIN имеет более широкий доступ.
