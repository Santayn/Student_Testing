# Обзор REST API

## Базовый контракт

Основной REST API опубликован под префиксом:

```text
/api/v1/**
```

Frontend использует относительный base URL `/api/v1`, а production Nginx проксирует его в backend. Поэтому браузер обычно работает с API как с same-origin ресурсом.

## Размер текущего API

| Показатель | Значение |
|---|---:|
| Всего endpoint'ов | 159 |
| GET | 74 |
| POST | 38 |
| PUT | 38 |
| DELETE | 9 |
| Frontend HTTP-вызовов в `src/api` | 114 |

Все 114 маршрутов, объявленных текущими frontend API-модулями, имеют соответствующий backend endpoint. Статический аудит не обнаружил клиентских обращений к отсутствующим backend route.

## Основные зоны API

```text
/api/v1/auth/**                  аутентификация
/api/v1/users/**                 User / Person
/api/v1/roles/**                 роли и permissions
/api/v1/faculties/**             факультеты
/api/v1/groups/**                группы
/api/v1/subjects/**              предметы
/api/v1/memberships/**           академические membership-связи
/api/v1/courses/**               шаблоны и версии курсов
/api/v1/lectures/**              лекции и материалы
/api/v1/teaching/**              учебная нагрузка
/api/v1/topics/**                темы
/api/v1/questions/**             банк вопросов
/api/v1/tests/**                 management API тестирования
/api/v1/public/learning/**       student learning API
/api/v1/results/**               результаты
/api/v1/admin/database-backups   backup/restore
/api/v1/status/**                health/readiness
```

## Management API и Student Learning API

Тестовый домен намеренно разделён на две поверхности.

```text
/tests/**
→ управление тестами, назначениями и административный low-level attempt API

/public/learning/**
→ безопасная поверхность студента
```

Название `public/learning` не означает anonymous access: весь этот namespace требует аутентифицированного пользователя. `public` здесь отражает безопасную внешнюю модель учебного контракта, в которой не раскрываются правильные ответы и внутренние administrative fields.

## Источник истины

Для API-контракта источниками истины являются backend controller'ы, request/response DTO и `SecurityConfig`. Frontend API-модули показывают, какая часть этого контракта реально используется текущим Vue-клиентом.
