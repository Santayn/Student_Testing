# Permissions

Текущий `DataLoader` создаёт следующие канонические permissions. Имена сохраняются в нижнем регистре.

| Permission | Назначение |
|---|---|
| `users.read` | Просмотр пользователей. |
| `users.write` | Управление пользователями. |
| `roles.manage` | Управление ролями и permissions. |
| `people.read` | Просмотр `Person`. |
| `people.write` | Управление `Person`. |
| `academic.manage` | Управление факультетами, группами, предметами и memberships. |
| `courses.manage` | Управление шаблонами курсов, версиями и лекциями. |
| `teaching.manage` | Управление учебной нагрузкой и enrollments. |
| `tests.manage` | Управление тестами и их назначениями. |
| `questions.manage` | Управление вопросами и вариантами. |
| `tests.take` | Прохождение назначенных тестов. |
| `lectures.read` | Чтение назначенных лекций. |

Security matcher в некоторых местах принимает также uppercase authority aliases, однако в БД каноническими являются lowercase names.

Permissions могут быть назначены через роль и напрямую пользователю. Фактический доступ также может зависеть от object-level ownership, поэтому наличие permission не гарантирует доступ к любому объекту этого типа.
