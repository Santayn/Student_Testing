# Ограничения развёртывания

Этот документ фиксирует ограничения текущей версии проекта, обнаруженные при deployment-аудите. Они не означают, что локальный Docker-сценарий неработоспособен, но важны при переносе системы за пределы локальной/demo среды.

## Критичные для production

### Нет законченного clean production bootstrap

При отключённых:

```text
APP_DATA_LOADER_ENABLED=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_SQL_INIT_MODE=never
```

проект не содержит versioned migrations, system seed и initial-admin provisioning, достаточных для полностью пустой PostgreSQL.

### DataLoader смешивает system и demo seed

Он создаёт как обязательные роли/permissions, так и известные демонстрационные аккаунты. Эти задачи должны быть разделены.

### Несовпадение `People` / `Person`

`schema.sql` содержит внешний ключ `Tests.AuthorPersonId → People.Id`, тогда как текущая JPA entity использует таблицу `Person`.

## Существенные ограничения

### Нет Flyway/Liquibase

Schema развивается через Hibernate `ddl-auto` и общий `schema.sql`. Это не даёт строгой версии миграций и предсказуемого upgrade path.

### Backend Maven Wrapper в Unix

Поставляемый `backend/mvnw` имеет CRLF line endings, а ZIP не сохраняет executable permission. `./mvnw spring-boot:run` на Linux/macOS в текущем виде требует исправления.

### SQL backup не включает lecture files

Встроенный backup сохраняет PostgreSQL, но не `lecture-uploads`.

### Local LLM и Docker host

Backend разрешает LLM endpoint только на `localhost`, `127.0.0.1` или `::1`. Внутри Docker `127.0.0.1` относится к самому backend container, а не к host. Обычная схема «Ollama на host + backend в Docker» поэтому без изменения реализации не работает.

## Конфигурационные ограничения

### Frontend host port фиксирован

Compose содержит:

```text
80:80
```

и не предоставляет `FRONTEND_PORT`.

### Frontend env частично build-time

Изменение `VITE_*` или `APP_PUBLIC_REGISTRATION_ENABLED` для production frontend требует rebuild, не только restart. Для локального `npm run dev` Vite читает корневой `.env` при запуске.

### PostgreSQL client version не зафиксирована

Backend runtime устанавливает общий `postgresql-client`, тогда как server image фиксирован на PostgreSQL 16.

## Infrastructure limitations

Текущий Compose не предоставляет готовые:

- HTTPS/TLS termination;
- secrets manager;
- CPU/RAM limits;
- централизованную log rotation policy;
- HA PostgreSQL;
- горизонтальное масштабирование backend;
- rolling deployment/rollback;
- immutable versioned image release policy.

## Вывод

Текущая конфигурация хорошо подходит для локального single-host запуска и демонстрации. Перед публичным production-развёртыванием перечисленные критичные пункты должны быть устранены или компенсированы внешней инфраструктурой и эксплуатационными процедурами.
