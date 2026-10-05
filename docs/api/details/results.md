# API результатов

Results API имеет отдельную read-модель для teacher/admin и для student.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/results/teacher/subjects` | Получить предметы для просмотра результатов преподавателем | TEACHER / ADMIN + tests.manage/object access | `200` | `results.api.js` |
| `GET` | `/api/v1/results/student/subjects` | Получить предметы с результатами студента | Authenticated + own data | `200` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/lectures` | Получить лекции для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/tests` | Получить тесты для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/groups` | Получить группы для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/students` | Получить студентов для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/data` | Получить результаты преподавателя/администратора | TEACHER / ADMIN + tests.manage/object access | `200` | `results.api.js` |
| `GET` | `/api/v1/results/student/data` | Получить собственные результаты студента | Authenticated + own data | `200` | `results.api.js` |

## Teacher/Admin flow

Frontend строит каскад фильтров:

```text
subjects → lectures → tests → groups → students → data
```

Соответствующие GET endpoints принимают context query parameters и object-level access checks.

## Student flow

Student получает список доступных ему предметов и `student/data`. Backend сам связывает запрос с текущим Person; произвольный просмотр результатов другого студента через student namespace не предусмотрен.

## ResultDataResponse

```text
stats
selectedTestName
selectedGroupName
selectedStudentName
attemptCount
attempts[]
```

Attempt содержит stats и `results[]` по вопросам.

## Correct answer

`ResultItemResponse.correctAnswer` условно сериализуется и для student-mode удаляется/не раскрывается. Это принципиальное отличие student result contract от teacher/admin read model.
