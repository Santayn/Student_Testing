# Технические ограничения и переходные решения

## Назначение

Этот документ фиксирует не пользовательские ограничения, а технические особенности текущей реализации, которые могут влиять на сопровождение, развёртывание или дальнейший рефакторинг.

Приоритеты ниже отражают потенциальное влияние, а не обязательную срочность исправления.

## Критичное для проверки

### Несовпадение имени таблицы Person в `schema.sql`

JPA-модель:

```java
@Table(name = "`Person`")
```

Но `schema.sql` содержит FK для `Tests.AuthorPersonId` на:

```sql
REFERENCES "People" ("Id")
```

Это должно быть проверено на чистой и существующей БД. Если фактическая таблица называется `Person`, SQL необходимо исправить. Иначе инициализация схемы может завершиться ошибкой или зависеть от legacy-таблицы.

## Высокий приоритет

### Нет версионированных миграций

Сейчас schema evolution распределена между:

- Hibernate `ddl-auto=update` в local;
- `schema.sql`;
- JPA-моделью.

Рекомендуемое направление: Flyway/Liquibase, `ddl-auto=validate` в управляемых средах.

### Refresh token доступен JavaScript

Auth store persist'ит refresh token. Это делает долгоживущий credential доступным при XSS.

Более защищённая модель — HttpOnly + Secure + SameSite cookie с соответствующим backend-контрактом и CSRF-моделью. Реализация требует согласованного изменения frontend/backend и не может быть сделана только UI-слоем.

### Backup БД не охватывает lecture files

`pg_dump` сохраняет PostgreSQL, но `lecture-uploads` находится в отдельном volume. Для disaster recovery нужен составной backup.

### Нет authoritative deadline тестовой попытки

`Test.Duration` есть, но текущий lifecycle не фиксирует жёсткий server-side deadline, который автоматически запрещает отправку после истечения времени независимо от клиента.

Если timed tests являются обязательным требованием, deadline должен вычисляться/проверяться backend.

## Средний приоритет

### Переходная связь Lecture ↔ Test

Одновременно используются:

- `Lecture.LinkedTestId`;
- `LectureTestLinks`.

До удаления legacy-поля нужно чётко определить единственный source of truth и выполнить миграцию старых данных.

### Frontend ожидает некоторые специализированные error codes

В отдельных UI-сценариях присутствует логика под специфичные коды вроде dependency conflict, в то время как backend чаще возвращает общие codes. Контракт следует унифицировать и затем зафиксировать в `docs/api/`.

### Dev proxy Vite и backend port

Текущий `vite.config.js` проксирует dev API на `localhost:8081`, в то время как backend default и Docker mapping обычно используют `8080`.

Локальная разработка требует либо совпадающей настройки порта, либо изменения proxy target.

### Maven wrapper line endings

`backend/mvnw` содержит CRLF и может не запускаться напрямую в Linux shell без нормализации line endings/executable bit. Docker build не зависит от wrapper, поскольку использует системный Maven image.

### Local LLM увеличивает submit latency

LLM grading синхронно входит в завершение попытки. При медленной модели пользовательский submit ждёт ответ до timeout.

### Observability ограничена

`traceId` присутствует в ошибках, но отдельной системы structured logs/metrics/tracing не обнаружено. Для production эксплуатации полезны централизованные логи и корреляция trace/request id.

### Demo data loader

Local profile включает `DataLoader`. Это удобно для разработки, но демонстрационные аккаунты/данные не должны появляться в production. Production environment должен явно использовать `APP_DATA_LOADER_ENABLED=false`.

## Архитектурные компромиссы

### Frontend saga для создания теста

Создание Test и нескольких Assignment не атомарно на сервере. Frontend выполняет компенсационное удаление Test при частичном сбое.

Для более сильных гарантий целостности можно добавить server-side command, выполняющий весь use case в одной транзакции.

### Startup auth waterfall

При восстановлении истёкшей сессии frontend может выполнять последовательность refresh, затем `/auth/me`. Это корректно, но увеличивает количество startup requests. Возможная оптимизация — возвращать актуальный identity snapshot вместе с refresh или внедрить другой bootstrap contract.

### Исторический snapshot вопросов

Попытка фиксирует набор question IDs и responses, но строгий immutable snapshot всех текстов/вариантов вопроса как отдельный объект не выделен. Если требования аудита результатов предполагают неизменное историческое представление, понадобится snapshot-модель.

## Что не является ошибкой

- `/api/v1/public/learning/**` требует authentication: `public` означает student-facing API, а не anonymous access.
- `User` без `Person` допустим как промежуточное состояние аккаунта.
- отсутствие удаления некоторых membership/assignment через UI связано с сохранением жизненного цикла и не обязательно означает отсутствие backend-функции.
