# Volumes и хранение данных

## Named volumes

Compose объявляет:

```yaml
volumes:
  postgres-data:
  lecture-uploads:
```

### `postgres-data`

Содержит физические данные PostgreSQL.

### `lecture-uploads`

Содержит загруженные материалы лекций.

## Безопасные операции

Контейнер можно пересоздать:

```bash
docker compose up -d --build
```

без удаления named volumes.

Обычный:

```bash
docker compose down
```

также сохраняет volumes.

## Опасная операция

```bash
docker compose down -v
```

удаляет volumes и должна рассматриваться как destructive reset.

## Контроль диска

Приложение не имеет встроенного мониторинга свободного места. Оператор должен контролировать capacity host самостоятельно.

Особенно растут:

```text
PostgreSQL data
lecture materials
Docker images/build cache
container logs
```

## Права доступа

Backend runtime работает от непривилегированного пользователя внутри container. При переходе от named volume к bind mount нужно отдельно обеспечить корректные права записи в `APP_STORAGE_LECTURE_MATERIALS_DIR`.

## Перенос на другой host

Недостаточно перенести исходники. Нужно перенести:

```text
DB backup
lecture-files backup
.env/configuration
версию приложения
```

и только после этого запускать новый stack.
