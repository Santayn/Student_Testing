# Развёртывание Student Testing

Этот раздел описывает **установку, конфигурацию, запуск и обновление Student Testing**. Основным поддерживаемым сценарием текущего проекта является single-host запуск через Docker Compose. Для разработки также доступны раздельный запуск backend и frontend.

Раздел не дублирует внутреннюю архитектуру системы, каталог REST API или правила разработки. Они находятся соответственно в `technical`, `api` и `development`.

## Основные сценарии

### [Обзор развёртывания](./details/deployment-overview.md)

Какие способы запуска существуют, какой из них считать основным и чем отличаются локальная, development и production-подобная среды.

### [Системные требования](./details/system-requirements.md)

Что требуется на машине для Docker-запуска и какие дополнительные инструменты нужны при раздельной разработке frontend/backend.

### [Быстрый запуск в Windows](./details/quick-start-windows.md)

Запуск через `start-local.cmd` или `start-local.ps1`, автоматическое создание `.env` и проверка результата.

### [Ручной запуск Docker Compose](./details/docker-compose-start.md)

Подготовка `.env`, сборка образов, запуск контейнеров и базовая диагностика.

### [Локальная разработка без полного Docker-стека](./details/local-development.md)

Раздельный запуск backend и frontend, требования к Java/Node.js и текущие ограничения локальной конфигурации.

## Конфигурация и сеть

### [Переменные окружения](./details/environment-variables.md)

Обязательные секреты, параметры PostgreSQL, JPA, регистрации, таймаутов и различие между runtime- и build-time-переменными.

### [Сеть и порты](./details/networking-and-ports.md)

Какие порты публикуются на host, как Nginx проксирует API и почему PostgreSQL остаётся внутри Docker-сети.

## Данные и первый запуск

### [Инициализация базы данных](./details/database-initialization.md)

`citext`, Hibernate `ddl-auto`, `schema.sql`, DataLoader и фактический порядок инициализации чистой БД.

### [Первый запуск и bootstrap](./details/first-start-and-bootstrap.md)

Как появляются базовые роли, permissions и демонстрационные данные, и почему production bootstrap пока требует доработки.

### [Хранилище и Docker volumes](./details/storage-and-volumes.md)

Где находятся PostgreSQL-данные и материалы лекций, а также какие команды безопасны для данных.

### [Особенности резервного копирования](./details/backup-considerations.md)

Что сохраняет встроенный SQL backup и почему для полного восстановления отдельно требуется volume материалов лекций.

## Сборка и эксплуатация

### [Frontend build и Nginx](./details/frontend-build-and-nginx.md)

Multi-stage frontend image, build-time параметры Vite, Nginx, SPA fallback, proxy и кэширование.

### [Backend build и runtime](./details/backend-build-and-runtime.md)

Сборка Spring Boot JAR, runtime Java image, непривилегированный пользователь, `pg_dump`/`psql` и readiness.

### [Healthchecks](./details/healthchecks.md)

Проверка PostgreSQL, backend readiness и frontend Nginx health endpoint.

### [Обновление и пересборка](./details/update-and-rebuild.md)

Когда достаточно restart, а когда требуется rebuild frontend/backend image.

### [Остановка и сброс среды](./details/shutdown-and-reset.md)

Безопасная остановка, повторный запуск и полное удаление persistent volumes.

## Production

### [Production-развёртывание](./details/production-deployment.md)

Целевая схема внешнего развёртывания, HTTPS, секреты, БД, DataLoader и требования к production bootstrap.

### [Ограничения развёртывания](./details/deployment-limitations.md)

Текущие риски и ограничения: отсутствие миграций и чистого production bootstrap, demo seed, `mvnw`, Vite proxy, LLM в Docker и другие найденные аудитом моменты.
