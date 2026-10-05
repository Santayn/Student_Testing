# Student Learning API

Безопасная API-поверхность, через которую студент читает доступный контент, начинает/продолжает попытку и отправляет тест. Несмотря на namespace `public`, endpoints требуют authentication.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/public/learning/subjects/{subjectId}` | Получить доступный студенту предмет | Authenticated + student context | `200` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/subjects/{subjectId}/lectures` | Получить лекции предмета студента | Authenticated + student context | `200` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}` | Получить лекцию студента | Authenticated + student context | `200` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/tests` | Получить тесты лекции и их доступность | Authenticated + student context | `200` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/materials` | Получить материалы лекции | Authenticated + student context | `200` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/materials/{materialId}/download` | Скачать материал лекции | Authenticated + student context | `200` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/tests/{testId}` | Получить публичные метаданные теста без старта попытки | Authenticated + student context | `200` | — |
| `POST` | `/api/v1/public/learning/test-assignments/{assignmentId}/attempts/start` | Начать или продолжить попытку теста | Authenticated + student context | `200` | `learning.api.js` |
| `POST` | `/api/v1/public/learning/attempts/{attemptId}/submit` | Отправить и завершить попытку | Authenticated + student context | `200` | `learning.api.js` |

## Получение теста не расходует попытку

```http
GET /api/v1/public/learning/tests/{testId}
```

возвращает только metadata и не создаёт TestAttempt.

## Start/resume

```http
POST /api/v1/public/learning/test-assignments/{assignmentId}/attempts/start
```

Backend проверяет assignment, enrollment, доступность, лимит попыток и наличие уже активной попытки. При существующей незавершённой попытке возвращается она же.

Ответ содержит:

```text
attemptId
assignmentId
test
questions[]
```

## Student Question DTO

```json
{
  "id": 101,
  "type": 1,
  "text": "Вопрос",
  "question": "Вопрос",
  "points": 1,
  "ordinal": 1,
  "options": [
    { "id": 1, "text": "A", "ordinal": 1 }
  ],
  "matchingPrompts": [],
  "matchingOptions": []
}
```

Правильность option и management `correctAnswer` не выдаются студенту.

## Submit

```json
{
  "questionIds": [101, 102],
  "answers": [null, "текст"],
  "selectedOptionIds": [[1], []]
}
```

Массивы образуют позиционно связанный набор ответов.

Ответ содержит итоговый `score`, `correctCount`, `totalCount` и details.

## Дополнительное сокрытие данных

Student result contract на backend уже скрывает чувствительные поля. Frontend дополнительно санитизирует result DTO перед передачей UI-компонентам.

## Контрактные дублирования

`PublicQuestionResponse` сейчас содержит одновременно `text` и `question` с одинаковым содержимым. `PublicSubmitDetailResponse.correctAnswer` присутствует в DTO, но backend передаёт `null`.
