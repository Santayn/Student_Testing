# Запуск и остановка

## Обычный запуск

Из корня проекта:

```powershell
./start-local.ps1
```

или:

```powershell
./start-local.cmd
```

Скрипт проверяет `.env`, при необходимости создаёт секреты и запускает:

```text
docker compose up -d --build
```

Ручной эквивалент:

```bash
docker compose up -d --build
```

## Проверка после запуска

```bash
docker compose ps
```

Ожидаемое состояние:

```text
postgres  → running / healthy
backend   → running / healthy
frontend  → running
```

Далее проверить health endpoints согласно [service-health.md](./service-health.md).

## Мягкая остановка

```bash
docker compose down
```

Контейнеры и compose network удаляются, named volumes сохраняются.

## Остановка без удаления контейнеров

```bash
docker compose stop
```

Повторный запуск:

```bash
docker compose start
```

## Опасная операция

```bash
docker compose down -v
```

`-v` удаляет named volumes. Для текущего проекта это означает потерю:

```text
postgres-data
lecture-uploads
```

Перед такой операцией обязательны проверенные резервные копии БД и файлов лекций.

## Перезапуск отдельного сервиса

Например backend:

```bash
docker compose restart backend
```

Перезапуск не пересобирает image. После изменения кода или build-time конфигурации нужен rebuild.
