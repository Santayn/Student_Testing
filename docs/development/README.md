# Разработка Student Testing

Раздел описывает практический процесс разработки и расширения Student Testing. Он не дублирует техническую документацию, а отвечает на вопрос: **какие части проекта должен изменить разработчик, чтобы новая функция была завершённой и согласованной с существующей архитектурой**.

Главный принцип проекта — изменение обычно проходит через несколько слоёв. Новая возможность редко ограничивается одним `View`, `Controller` или `Service`: необходимо проверить API-контракт, безопасность, тесты, конфигурацию и документацию.

## [Обзор разработки](./details/development-overview.md)

Стек разработки, границы frontend/backend и базовая модель прохождения изменения через проект.

## [Структура репозитория](./details/repository-structure.md)

Назначение основных каталогов, точек входа и конфигурационных файлов.

## [Локальная среда разработки](./details/local-development-environment.md)

Инструменты для frontend/backend разработки и отличия от Docker-запуска.

## [Рабочий процесс изменения](./details/development-workflow.md)

Канонический путь: определить домен → изменить код → проверить безопасность и контракт → добавить тесты → выполнить quality gates → обновить документацию.

## Backend

- [Backend development](./details/backend-development.md)
- [Backend conventions](./details/backend-layer-conventions.md)
- [Добавление backend endpoint](./details/adding-backend-endpoint.md)
- [Изменение базы данных](./details/database-changes.md)
- [Security и permissions](./details/security-and-permission-changes.md)

## Frontend

- [Frontend development](./details/frontend-development.md)
- [Добавление новой страницы](./details/adding-frontend-page.md)
- [Composables и state](./details/composables-and-state.md)
- [UI development](./details/ui-development.md)
- [Routing и navigation](./details/routing-and-navigation.md)

## Сквозные изменения

- [API contract changes](./details/api-contract-changes.md)
- [Configuration и feature flags](./details/configuration-and-feature-flags.md)
- [Error handling development](./details/error-handling-development.md)
- [Writing tests](./details/writing-tests.md)
- [Quality gates](./details/quality-gates.md)
- [Documentation updates](./details/documentation-updates.md)

## Процесс команды

- [Contribution workflow](./details/contribution-workflow.md)
- [Review checklist](./details/review-checklist.md)
- [Development limitations](./details/development-limitations.md)
