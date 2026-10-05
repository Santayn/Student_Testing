# Scope назначения теста

`TestAssignment.scope` определяет контекст назначения теста.

| Scope | Значение | Допустимый target |
|---:|---|---|
| `1` | Global | Все target ID должны быть `null`. |
| `2` | CourseVersion | Только `courseVersionId`. |
| `3` | Lecture | Только `courseLectureId` (`Lecture.id`). |
| `4` | TeachingAssignment / group context | Только `teachingAssignmentId`. |

Backend проверяет, что для scope задан ровно подходящий target и что соответствующая сущность существует.

Также для одного теста запрещено создавать дублирующее назначение с тем же scope и target.
