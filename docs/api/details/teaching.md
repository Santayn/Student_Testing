# Учебная нагрузка и назначения

Teaching API связывает teacher subject membership, группу, тип нагрузки, версию курса, enrollment студентов и назначения лекций.

## Endpoint'ы

| Метод | Маршрут | Назначение | Доступ | HTTP | Frontend |
|---|---|---|---|---:|---|
| `GET` | `/api/v1/teaching/load-types` | Получить типы учебной нагрузки | Authenticated + domain access checks | `200` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/load-types/{loadTypeId}` | Получить тип нагрузки | Authenticated + domain access checks | `200` | — |
| `POST` | `/api/v1/teaching/load-types` | Создать тип нагрузки | ADMIN-authority write | `200` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/load-types/{loadTypeId}` | Изменить тип нагрузки | ADMIN-authority write | `200` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/subject-load-types` | Получить типы нагрузки membership предмета | Authenticated + domain access checks | `200` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/subject-memberships/{subjectMembershipId}/load-types` | Назначить тип нагрузки предметному membership | TEACHER / ADMIN + teaching.manage/object access | `200` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/subject-load-types/{subjectLoadTypeId}/status` | Изменить статус типа нагрузки membership | TEACHER / ADMIN + teaching.manage/object access | `200` | — |
| `PUT` | `/api/v1/teaching/subject-load-types/{subjectLoadTypeId}` | Изменить связь типа нагрузки | TEACHER / ADMIN + teaching.manage/object access | `200` | — |
| `GET` | `/api/v1/teaching/assignments` | Получить учебные/тестовые назначения | Authenticated + domain access checks | `200` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/assignments/{id}` | Получить назначение | Authenticated + domain access checks | `200` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/assignments` | Создать учебное назначение | ADMIN-authority write | `200` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/assignments/{assignmentId}` | Изменить назначение | ADMIN-authority write | `200` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/assignments/{assignmentId}/status` | Изменить статус назначения | ADMIN-authority write | `200` | — |
| `GET` | `/api/v1/teaching/enrollments` | Получить enrollment студентов | Authenticated + domain access checks | `200` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/assignments/{assignmentId}/enrollments` | Добавить enrollment | TEACHER / ADMIN + teaching.manage/object access | `200` | — |
| `PUT` | `/api/v1/teaching/enrollments/{enrollmentId}/status` | Изменить статус enrollment | TEACHER / ADMIN + teaching.manage/object access | `200` | — |
| `GET` | `/api/v1/teaching/lecture-assignments` | Получить назначения лекций | Authenticated + domain access checks | `200` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/assignments/{assignmentId}/lecture-assignments` | Назначить лекцию | TEACHER / ADMIN + teaching.manage/object access | `200` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/lecture-assignments/{lectureAssignmentId}` | Изменить назначение лекции | TEACHER / ADMIN + teaching.manage/object access | `200` | — |
| `PUT` | `/api/v1/teaching/lecture-assignments/{lectureAssignmentId}/status` | Изменить статус назначения лекции | TEACHER / ADMIN + teaching.manage/object access | `200` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/lecture-progress` | Получить прогресс лекций | Authenticated + domain access checks | `200` | — |
| `POST` | `/api/v1/teaching/lecture-assignments/{lectureAssignmentId}/progress` | Обновить прогресс лекции | TEACHER / ADMIN + teaching.manage/object access | `200` | — |

## TeachingAssignment request

```json
{
  "subjectMembershipId": 12,
  "groupId": 4,
  "loadTypeId": 1,
  "courseVersionId": 8,
  "semester": 1,
  "studyCourse": 2,
  "academicYear": 2026,
  "hoursPerWeek": 2.0,
  "status": 1,
  "notes": "..."
}
```

Semester ограничен `1..2`, `status` — `1..4`.

## Enrollment

```json
{
  "groupMembershipId": 55,
  "status": 1
}
```

Enrollment фиксирует участие конкретного group membership в teaching assignment.

## LectureAssignment

Поддерживает доступность/дедлайн, обязательность, минимальный прогресс и status. Response также содержит snapshot fields: courseVersion, group, teacher, semester и academic year.

## Progress

Progress request хранит процент, status/completionSource и опциональные position/timeSpent values.

## Access

Создание/изменение базовых teaching assignments и load types относится к administrative authority. Часть lecture/enrollment операций разрешена teacher/admin с `teaching.manage` и последующими object-level checks.
