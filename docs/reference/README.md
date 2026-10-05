# Справочник Student Testing

Этот раздел предназначен для быстрого поиска терминов, кодов, статусов, ролей, permissions, лимитов и инфраструктурных значений проекта. Подробное объяснение архитектуры, API и эксплуатации находится в соответствующих разделах документации.

## [Термины и сокращения](./details/glossary.md)

Краткие определения основных сущностей и перегруженных терминов: `User`, `Person`, `Assignment`, `Result`, `CourseLecture` и других.

## [Роли](./details/roles.md)

Системные роли backend, рабочие роли frontend и приоритет выбора рабочего пространства.

## [Permissions](./details/permissions.md)

Канонические permissions, создаваемые текущим DataLoader, и их назначение.

## [Матрица ролей и permissions](./details/roles-and-permissions-matrix.md)

Базовое seed-распределение permissions между `ADMIN`, `TEACHER` и `STUDENT`.

## [Статусы и числовые коды](./details/statuses-and-codes.md)

Статусы memberships, тестовых и учебных назначений, попыток и прогресса.

## [Типы вопросов](./details/question-types.md)

Четыре типа вопросов и особенности их представления.

## [Scope назначения теста](./details/test-assignment-scopes.md)

Значения `TestAssignment.scope` и допустимые target-поля.

## [Значения authentication](./details/authentication-values.md)

Режимы времени жизни access/refresh tokens и связанные значения.

## [Сущности](./details/entities.md)

Каталог JPA-сущностей по предметным областям.

## [Связи сущностей](./details/entity-relationships.md)

Краткая карта ключевых отношений между пользователями, учебным процессом, контентом и тестированием.

## [Идентификаторы и таблицы БД](./details/identifiers-and-table-names.md)

Java Entity, физическое имя таблицы и тип идентификатора.

## [Коды ошибок API](./details/api-error-codes.md)

Текущие backend error codes и frontend-reserved коды.

## [HTTP-статусы](./details/http-statuses.md)

Краткая таблица HTTP-статусов, используемых API.

## [Дата и время](./details/date-and-time-formats.md)

`Instant`, `LocalDate`, `LocalTime` и wire-форматы.

## [Лимиты и валидация](./details/limits-and-validation.md)

Основные ограничения длины строк, числовые диапазоны и upload limits.

## [Файлы и форматы](./details/file-formats.md)

DOCX import, материалы лекций, SQL backup/restore и бинарные ответы.

## [Переменные окружения](./details/environment-variables.md)

Справочник backend, Docker, frontend build-time и Nginx runtime variables.

## [Порты, пути и сервисы](./details/ports-paths-and-services.md)

REST prefix, Swagger, health endpoints, Docker containers, volumes, ports и storage paths.

## [Неоднозначности справочника](./details/reference-caveats.md)

Значения, которые текущая реализация не формализует достаточно строго и которые нельзя трактовать как глобальные enum.
