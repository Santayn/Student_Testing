# REST API Student Testing

Этот раздел описывает **контракт между frontend и backend Student Testing**: маршруты REST API, форматы запросов и ответов, правила аутентификации и авторизации, числовые контрактные значения, загрузку файлов, ошибки и текущее состояние OpenAPI/Swagger.

Все прикладные маршруты backend находятся под префиксом `/api/v1`. Полный машинно-генерируемый список доступен через OpenAPI, а этот раздел объясняет семантику контрактов и ограничения, которые из одной схемы OpenAPI неочевидны.

## Общие сведения

### [Обзор API](./details/api-overview.md)

Границы REST API, статистика маршрутов и принципы разделения management API и student learning API.

### [Полный индекс endpoint'ов](./details/endpoint-index.md)

Все REST endpoint'ы текущего backend с HTTP-методом, уровнем доступа, handler'ом и использованием во frontend.

### [Общие соглашения](./details/conventions.md)

Base URL, JSON, даты и время, HTTP-коды, списковые ответы, бинарные ответы и правила изменения ресурсов.

### [Контрактные числовые значения](./details/contract-values.md)

Типы вопросов, статусы memberships, тестовых и учебных назначений и другие числовые поля API.

## Безопасность

### [Аутентификация](./details/authentication.md)

Login/register/refresh/revoke, Bearer access token, refresh token и сведения о текущей сессии.

### [Авторизация](./details/authorization.md)

Роли, permissions, request-level security и object-level ownership checks.

## Предметные API

### [System и health](./details/system-and-health.md)

Status и readiness endpoints.

### [Пользователи, роли и permissions](./details/users-roles-and-permissions.md)

User, Person, роли, permissions, активность аккаунта и привязка профиля.

### [Академическая структура](./details/academic-structure.md)

Факультеты, группы, предметы и связи факультет–предмет.

### [Memberships](./details/memberships.md)

Membership факультетов, групп и предметов, их статусы и ограничения доступа.

### [Курсы](./details/courses.md)

Шаблоны и версии курсов, публикация и ownership.

### [Лекции и материалы](./details/lectures-and-materials.md)

CRUD лекций, связи с тестами, upload/download учебных материалов.

### [Учебная нагрузка](./details/teaching.md)

Load types, teaching assignments, enrollments, lecture assignments и прогресс.

### [Темы и вопросы](./details/topics-and-questions.md)

Темы, банк вопросов, варианты ответа, matching и DOCX-импорт.

### [Тесты и назначения](./details/tests-and-assignments.md)

Тесты, правила отбора вопросов, назначения и низкоуровневый administrative attempt API.

### [Student Learning API](./details/student-learning.md)

Безопасный контракт студента для предметов, лекций, тестов, попыток и отправки ответов.

### [Результаты](./details/results.md)

Read API результатов для преподавателя/администратора и студента.

### [Резервные копии](./details/database-backups.md)

Создание SQL backup и восстановление БД.

## Сквозные контракты

### [Файлы, multipart и binary](./details/files-and-multipart.md)

Upload/download материалов, импорт DOCX и backup/restore файловые операции.

### [Ошибки](./details/errors.md)

Единый `ErrorResponse`, HTTP-коды и несоответствия между backend и frontend error codes.

### [Контракт frontend ↔ backend](./details/frontend-backend-contract.md)

Сверка клиентских API-модулей с backend routes и backend endpoint'ы, не используемые текущим frontend.

### [OpenAPI и Swagger](./details/openapi-and-swagger.md)

Доступ к Swagger UI, сильные стороны и текущие пробелы генерируемой схемы.

## Ограничения

### [Текущие ограничения API](./details/api-limitations.md)

Контрактные дублирования, непоследовательные статусы, отсутствие пагинации и другие найденные аудитом ограничения.
