# Лекции и материалы

Lecture API управляет метаданными лекции, связями с тестами и файлами учебных материалов.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/lectures` | Получить список ресурсов | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |
| `GET` | `/api/v1/lectures/{id}` | Получить ресурс по идентификатору | TEACHER / ADMIN + courses.manage/ownership | `200` | — |
| `POST` | `/api/v1/lectures` | Создать ресурс | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |
| `PUT` | `/api/v1/lectures/{id}` | Изменить ресурс | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |
| `PUT` | `/api/v1/lectures/{id}/linked-test` | Изменить legacy-связь лекции с тестом | TEACHER / ADMIN + courses.manage/ownership | `200` | — |
| `DELETE` | `/api/v1/lectures/{id}` | Удалить ресурс | TEACHER / ADMIN + courses.manage/ownership | `204` | `lectures.api.js` |
| `GET` | `/api/v1/lectures/{id}/tests` | Получить тесты, связанные с лекцией | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |
| `PUT` | `/api/v1/lectures/{id}/tests` | Заменить набор связанных тестов | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |
| `GET` | `/api/v1/lectures/{lectureId}/materials` | Получить материалы лекции | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |
| `POST` | `/api/v1/lectures/{lectureId}/materials` | Загрузить материалы лекции | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |
| `DELETE` | `/api/v1/lectures/{lectureId}/materials/{materialId}` | Удалить материал лекции | TEACHER / ADMIN + courses.manage/ownership | `204` | `lectures.api.js` |
| `GET` | `/api/v1/lectures/{lectureId}/materials/{materialId}/download` | Скачать материал лекции | TEACHER / ADMIN + courses.manage/ownership | `200` | `lectures.api.js` |

## Lecture request

```text
subjectId
subjectMembershipId
courseVersionId
ordinal
title
description
contentFolderKey
linkedTestId
publicVisible
```

Для преподавателя backend требует owned placement: обычно `subjectMembershipId` или `courseVersionId`. Простого произвольного `subjectId` недостаточно.

## Связи с тестами

Существуют два механизма:

- legacy поле `linkedTestId` и `PUT /lectures/{id}/linked-test`;
- множественная связь через `GET/PUT /lectures/{id}/tests`.

Новая клиентская логика использует множественную связь, но legacy контракт пока сохранён.

## Материалы

Upload использует `multipart/form-data`, field `files`. Download возвращает binary body с `Content-Disposition: attachment`. Метаданные ответа включают `fileName`, `contentType`, `sizeBytes`, `uploadedAtUtc`.
