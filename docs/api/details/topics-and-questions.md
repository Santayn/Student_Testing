# Темы и банк вопросов

Topic и Question API обслуживают преподавательский банк вопросов, варианты ответа, matching пары и DOCX import.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/topics` | Получить список ресурсов | TEACHER / ADMIN + questions.manage/ownership | `200` | `topics.api.js` |
| `GET` | `/api/v1/topics/{id}` | Получить ресурс по идентификатору | TEACHER / ADMIN + questions.manage/ownership | `200` | `topics.api.js` |
| `POST` | `/api/v1/topics` | Создать ресурс | TEACHER / ADMIN + questions.manage/ownership | `201` | `topics.api.js` |
| `PUT` | `/api/v1/topics/{id}` | Изменить ресурс | TEACHER / ADMIN + questions.manage/ownership | `200` | `topics.api.js` |
| `DELETE` | `/api/v1/topics/{id}` | Удалить ресурс | TEACHER / ADMIN + questions.manage/ownership | `204` | `topics.api.js` |
| `GET` | `/api/v1/questions` | Получить банк вопросов | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `GET` | `/api/v1/questions/{questionId}` | Получить ресурс по идентификатору | TEACHER / ADMIN + questions.manage/ownership | `200` | — |
| `GET` | `/api/v1/questions/{questionId}/options` | Получить варианты ответа вопроса | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `POST` | `/api/v1/questions/import` | Импортировать вопросы из DOCX | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `POST` | `/api/v1/questions` | Создать ресурс | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `PUT` | `/api/v1/questions/{questionId}` | Изменить ресурс | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `GET` | `/api/v1/questions/options/{optionId}` | Получить вариант ответа | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `POST` | `/api/v1/questions/{questionId}/options` | Добавить вариант ответа | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `PUT` | `/api/v1/questions/options/{optionId}` | Изменить вариант ответа | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |
| `PUT` | `/api/v1/questions/{questionId}/active` | Изменить активность ресурса | TEACHER / ADMIN + questions.manage/ownership | `200` | `questions.api.js` |

## Topic request

```text
subjectId
courseLectureId
subjectMembershipId (required)
ordinal (>0)
name
description
```

`subjectMembershipId` используется как основной teacher ownership context.

## Question request

```text
testId
courseLectureId
topicId
type
question
points
ordinal
correctAnswer
matchingPairs[]
```

Question update дополнительно принимает `active`.

## Option request

```json
{ "text": "Ответ", "ordinal": 1, "correct": true }
```

Флаг `correct` присутствует только в management API и не передаётся в student learning DTO.

## Matching

Для `type = 3` request может содержать пары `{ left, right, ordinal }`.

## DOCX import

```http
POST /api/v1/questions/import
Content-Type: multipart/form-data
```

Endpoint импортирует вопросы и варианты и возвращает количество импортированных объектов плюс созданные Question DTO.

## Удаление вопросов

Обычного DELETE Question в текущем API нет. Жизненный цикл управляется `PUT /questions/{id}/active`.
