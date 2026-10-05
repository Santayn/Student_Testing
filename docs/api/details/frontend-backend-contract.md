# Контракт frontend ↔ backend

## Результат статической сверки

| Метрика | Значение |
|---|---:|
| HTTP-вызовов в `frontend/src/api` | 114 |
| Сопоставлено с backend | 114 |
| Клиентских маршрутов без backend | 0 |
| Backend endpoint'ов без прямого frontend wrapper | 45 |

Таким образом, все текущие frontend API calls имеют соответствующий Spring route.

## API-модули frontend

Frontend централизует HTTP-доступ в `src/api/`: `auth`, `users`, `roles`, `faculties`, `groups`, `subjects`, `memberships`, `courses`, `lectures`, `teaching`, `topics`, `questions`, `tests`, `learning`, `results`, `databaseBackups`.

`http.js` отвечает за base URL, Bearer token, preflight refresh, response retry и нормализацию ошибок.

## Backend endpoint'ы без прямого wrapper текущего frontend

- `GET /api/v1/courses/templates/{templateId}` — Получить шаблон курса
- `GET /api/v1/courses/versions/{versionId}` — Получить версию курса
- `GET /api/v1/lectures/{id}` — Получить ресурс по идентификатору
- `PUT /api/v1/lectures/{id}/linked-test` — Изменить legacy-связь лекции с тестом
- `GET /api/v1/status` — Проверить состояние backend
- `GET /api/v1/status/readiness` — Проверить готовность backend и БД
- `GET /api/v1/memberships/faculties` — Получить membership факультетов
- `GET /api/v1/memberships/faculties/{facultyId}` — Получить участников факультета
- `GET /api/v1/memberships/faculties/memberships/{membershipId}` — Получить membership факультета
- `POST /api/v1/memberships/faculties/{facultyId}` — Добавить человека на факультет
- `PUT /api/v1/memberships/faculties/memberships/{membershipId}/status` — Изменить статус membership факультета
- `PUT /api/v1/memberships/faculties/memberships/{membershipId}` — Изменить membership факультета
- `GET /api/v1/memberships/groups/{groupId}` — Получить membership учебных групп
- `GET /api/v1/memberships/groups/memberships/{membershipId}` — Получить membership группы
- `PUT /api/v1/memberships/groups/memberships/{membershipId}` — Изменить membership группы
- `GET /api/v1/memberships/subjects/{subjectId}` — Получить участников предмета
- `GET /api/v1/public/learning/tests/{testId}` — Получить публичные метаданные теста без старта попытки
- `GET /api/v1/questions/{questionId}` — Получить ресурс по идентификатору
- `GET /api/v1/subjects/{id}/faculties` — Получить связанные факультеты
- `GET /api/v1/teaching/load-types/{loadTypeId}` — Получить тип нагрузки
- `PUT /api/v1/teaching/subject-load-types/{subjectLoadTypeId}/status` — Изменить статус типа нагрузки membership
- `PUT /api/v1/teaching/subject-load-types/{subjectLoadTypeId}` — Изменить связь типа нагрузки
- `PUT /api/v1/teaching/assignments/{assignmentId}/status` — Изменить статус назначения
- `POST /api/v1/teaching/assignments/{assignmentId}/enrollments` — Добавить enrollment
- `PUT /api/v1/teaching/enrollments/{enrollmentId}/status` — Изменить статус enrollment
- `PUT /api/v1/teaching/lecture-assignments/{lectureAssignmentId}` — Изменить назначение лекции
- `GET /api/v1/teaching/lecture-progress` — Получить прогресс лекций
- `POST /api/v1/teaching/lecture-assignments/{lectureAssignmentId}/progress` — Обновить прогресс лекции
- `GET /api/v1/tests/{id}` — Получить ресурс по идентификатору
- `PUT /api/v1/tests/{id}` — Изменить ресурс
- `GET /api/v1/tests/{testId}/selection-rules` — Получить правила отбора вопросов
- `PUT /api/v1/tests/{testId}/selection-rules` — Заменить правила отбора вопросов
- `GET /api/v1/tests/assignments` — Получить учебные/тестовые назначения
- `GET /api/v1/tests/assignments/{assignmentId}` — Получить назначение
- `PUT /api/v1/tests/assignments/{assignmentId}` — Изменить назначение
- `PUT /api/v1/tests/assignments/{assignmentId}/status` — Изменить статус назначения
- `GET /api/v1/tests/attempts` — Получить попытки тестов
- `GET /api/v1/tests/attempts/{attemptId}` — Получить попытку
- `POST /api/v1/tests/assignments/{assignmentId}/attempts` — Начать или продолжить попытку теста
- `GET /api/v1/tests/attempts/{attemptId}/responses` — Получить ответы попытки
- `POST /api/v1/tests/attempts/{attemptId}/responses` — Сохранить ответ попытки
- `GET /api/v1/tests/responses/{responseId}/selected-options` — Получить выбранные варианты ответа
- `POST /api/v1/tests/attempts/{attemptId}/complete` — Завершить попытку
- `GET /api/v1/users/{id}` — Получить пользователя
- `PUT /api/v1/users/{id}/permissions` — Заменить прямые permissions пользователя

Эти endpoints не следует автоматически удалять. Среди них есть служебные health routes, расширенные read operations и low-level administrative test-attempt API.

## Принцип изменения контракта

При изменении backend route нужно одновременно проверять:

1. соответствующий `frontend/src/api/*.js`;
2. composables/views, использующие API-модуль;
3. security matcher и object-level checks;
4. frontend API contract tests;
5. OpenAPI description;
6. этот раздел документации.
