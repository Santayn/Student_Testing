# Документация Student Testing

Документация проекта сгруппирована по назначению. Каждый раздел содержит краткий `README.md` с навигацией, а подробные материалы находятся во вложенной папке `details/`.

## [Описание проекта](./project-description/README.md)

Назначение Student Testing, пользовательские роли, основные функциональные возможности, организация учебного процесса и текущие ограничения системы.

## [Руководство пользователя](./user-guide/README.md)

Пошаговые сценарии работы с системой: общие действия, руководство студента, преподавателя и администратора.

## [Техническая документация](./technical/README.md)

Архитектура frontend/backend, модель данных, безопасность, предметные домены, жизненный цикл запросов, файловое хранилище, runtime и технические ограничения.

## [Развёртывание](./deployment/README.md)

Системные требования, Docker Compose, быстрый запуск, переменные окружения, первый старт, volumes, healthchecks, обновление и ограничения production-развёртывания.

## [REST API](./api/README.md)

Полный контракт frontend/backend: endpoint'ы, auth/security, request/response conventions, student learning API, ошибки, файлы и OpenAPI.

## [Тестирование](./testing/README.md)

Frontend/backend test strategy, quality gates, security/concurrency regression, API smoke, pre-release checklist, CI/E2E roadmap и текущие ограничения покрытия.

## [Разработка](./development/README.md)

Руководство разработчика: структура репозитория, backend/frontend conventions, добавление endpoint'ов и страниц, database/security changes, quality gates, contribution workflow и review checklist.

## [Эксплуатация и администрирование](./administration/README.md)

Поддержка работающей системы: запуск/health/logs, административный доступ, сессии, backup/restore, volumes, обновление, rollback и disaster recovery.

## [Справочник](./reference/README.md)

Быстрые таблицы терминов, ролей, permissions, статусов, кодов, сущностей, ошибок, лимитов, env-переменных, портов и инфраструктурных значений.

## Комплект документации

Основной план документации проекта завершён: описание продукта, руководство пользователя, техническая документация, развёртывание, API, тестирование, разработка, эксплуатация и справочные материалы находятся под единым `docs/`.
