# Авторизация API

## Два уровня контроля доступа

Backend использует два слоя:

```text
SecurityConfig
→ проверка роли / authority для namespace и HTTP method

CurrentUserAccessService / controller checks
→ проверка принадлежности конкретного Subject, Test, Lecture, CourseVersion и т. п.
```

Наличие роли `TEACHER` или permission `tests.manage` не означает автоматический доступ ко всем тестам системы.

## Основные authorities

Используются как роли (`ROLE_ADMIN`, `ROLE_TEACHER`) так и permissions (`users.read`, `academic.manage`, `tests.manage`, `questions.manage`, `courses.manage`, `teaching.manage`). SecurityConfig принимает некоторые имена и в uppercase-варианте для совместимости.

## Крупные зоны доступа

| Namespace | Request-level правило |
|---|---|
| `/status/**` | anonymous |
| `/auth/login`, `/register`, `/refresh` | anonymous |
| `/public/learning/**` | authenticated |
| `/admin/database-backups/**` | strict ADMIN |
| `/roles/**` | ADMIN или `roles.manage` |
| `/questions/**`, `/topics/**` | teacher/admin или `questions.manage` |
| `/tests/**` | teacher/admin или `tests.manage`; low-level attempts дополнительно ограничены administrative authority |
| `/courses/**`, `/lectures/**` | teacher/admin или `courses.manage` |
| `/results/teacher/**` | teacher/admin или `tests.manage` |
| `/results/student/**` | authenticated, затем own-data checks |
| academic write | ADMIN или `academic.manage` |
| membership write | administrative authority |

## Object-level ownership

Примеры дополнительных проверок:

- преподаватель должен владеть предметным membership/контекстом;
- тест должен принадлежать текущему преподавателю;
- лекция/версия курса должна относиться к доступному преподавателю контексту;
- student result API ограничивает данные текущим `Person` пользователя;
- student learning API проверяет enrollment/назначение и доступность контента.

## Важный нюанс `ADMIN_AUTHORITIES`

В группу administrative authorities включён `roles.manage`, поэтому на request-matcher уровне этот permission открывает несколько endpoint-классов шире, чем можно ожидать по названию. Object-level проверки местами дополнительно сужают доступ, но семантику `roles.manage` стоит пересмотреть при будущем hardening.
