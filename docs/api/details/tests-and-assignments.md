# Тесты, правила отбора и назначения

Management API тестирования предназначен для преподавателей/администраторов. Он не должен использоваться студентом вместо Student Learning API.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/tests` | Получить список ресурсов | TEACHER / ADMIN + tests.manage/ownership | `200` | `tests.api.js` |
| `GET` | `/api/v1/tests/{id}` | Получить ресурс по идентификатору | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `POST` | `/api/v1/tests` | Создать ресурс | TEACHER / ADMIN + tests.manage/ownership | `200` | `tests.api.js` |
| `PUT` | `/api/v1/tests/{id}` | Изменить ресурс | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `DELETE` | `/api/v1/tests/{id}` | Удалить ресурс | TEACHER / ADMIN + tests.manage/ownership | `204` | `tests.api.js` |
| `GET` | `/api/v1/tests/{testId}/selection-rules` | Получить правила отбора вопросов | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `PUT` | `/api/v1/tests/{testId}/selection-rules` | Заменить правила отбора вопросов | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `GET` | `/api/v1/tests/assignments` | Получить учебные/тестовые назначения | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `GET` | `/api/v1/tests/assignments/{assignmentId}` | Получить назначение | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `POST` | `/api/v1/tests/{testId}/assignments` | Создать назначение теста | TEACHER / ADMIN + tests.manage/ownership | `200` | `tests.api.js` |
| `PUT` | `/api/v1/tests/assignments/{assignmentId}` | Изменить назначение | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `PUT` | `/api/v1/tests/assignments/{assignmentId}/status` | Изменить статус назначения | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `GET` | `/api/v1/tests/attempts` | Получить попытки тестов | TEACHER / ADMIN + tests.manage/ownership | `200` | — |
| `GET` | `/api/v1/tests/attempts/{attemptId}` | Получить попытку | Administrative authority | `200` | — |
| `POST` | `/api/v1/tests/assignments/{assignmentId}/attempts` | Начать или продолжить попытку теста | Administrative authority | `200` | — |
| `GET` | `/api/v1/tests/attempts/{attemptId}/responses` | Получить ответы попытки | Administrative authority | `200` | — |
| `POST` | `/api/v1/tests/attempts/{attemptId}/responses` | Сохранить ответ попытки | Administrative authority | `200` | — |
| `GET` | `/api/v1/tests/responses/{responseId}/selected-options` | Получить выбранные варианты ответа | Administrative authority | `200` | — |
| `POST` | `/api/v1/tests/attempts/{attemptId}/complete` | Завершить попытку | Administrative authority | `200` | — |

## Test request

```json
{
  "title": "Тест",
  "description": "...",
  "duration": "00:30:00",
  "attemptsAllowed": 2,
  "questionCount": 20,
  "selectionRules": []
}
```

`attemptsAllowed` и `questionCount` должны быть положительными.

## SelectionRule

Rule задаёт `courseLectureId` или `topicId`, общее количество вопросов и количество каждого типа. Набор правил можно полностью заменить через `PUT /tests/{testId}/selection-rules`.

## Assignment

```json
{
  "scope": 4,
  "courseVersionId": null,
  "courseLectureId": null,
  "teachingAssignmentId": 15,
  "availableFromUtc": "2026-10-05T10:00:00Z",
  "availableUntilUtc": "2026-10-12T10:00:00Z",
  "status": 2
}
```

Assignment отделяет сам тест от контекста, кому и когда он назначен.

## Low-level attempt API

Маршруты `/tests/attempts/**`, `/tests/responses/**` и `POST /tests/assignments/{id}/attempts` защищены administrative authority. Это не student API.

Student должен начинать попытку только через:

```text
POST /public/learning/test-assignments/{assignmentId}/attempts/start
```

## Ownership

Teacher queries требуют subject/test ownership. Например список тестов для teacher без `subjectId` отклоняется, а конкретные Test/Assignment операции проверяют принадлежность через `CurrentUserAccessService`.
