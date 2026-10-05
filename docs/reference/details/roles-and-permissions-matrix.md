# Матрица ролей и permissions

Таблица отражает **базовое seed-распределение DataLoader**, а не неизменяемый enum. Администратор может изменить role-permission associations.

| Permission | ADMIN | TEACHER | STUDENT |
|---|:---:|:---:|:---:|
| `users.read` | ✓ |  |  |
| `users.write` | ✓ |  |  |
| `roles.manage` | ✓ |  |  |
| `people.read` | ✓ | ✓ |  |
| `people.write` | ✓ | ✓ |  |
| `academic.manage` | ✓ | ✓ |  |
| `courses.manage` | ✓ | ✓ |  |
| `teaching.manage` | ✓ | ✓ |  |
| `tests.manage` | ✓ | ✓ |  |
| `questions.manage` | ✓ | ✓ |  |
| `tests.take` | ✓ |  | ✓ |
| `lectures.read` | ✓ | ✓ | ✓ |

Роль `USER` создаётся DataLoader, но отдельный набор permissions для неё не назначается.

Даже при наличии permission преподавательские операции дополнительно ограничиваются проверками владения/контекста через `CurrentUserAccessService`.
