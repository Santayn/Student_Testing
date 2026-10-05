# Ручной запуск Docker Compose

## Подготовка `.env`

Скопируйте шаблон:

```bash
cp .env.example .env
```

На Windows файл можно скопировать обычными средствами проводника или PowerShell.

Минимально необходимо задать:

```dotenv
POSTGRES_PASSWORD=<случайный надёжный пароль>
APP_JWT_SECRET=<секрет не короче 32 символов>
```

Не публикуйте `.env`: он исключён из Git и Docker build context.

## Сборка и запуск

Из корня проекта:

```bash
docker compose up -d --build
```

Compose создаёт или использует существующие:

- Docker images backend/frontend;
- внутреннюю сеть проекта;
- `postgres-data` volume;
- `lecture-uploads` volume.

## Зависимости запуска

Backend не запускается до successful PostgreSQL healthcheck. Frontend ожидает healthy backend.

```text
postgres → backend → frontend
```

## Проверка контейнеров

```bash
docker compose ps
```

Логи всех сервисов:

```bash
docker compose logs
```

Логи конкретного сервиса:

```bash
docker compose logs backend
docker compose logs frontend
docker compose logs postgres
```

Для наблюдения в реальном времени:

```bash
docker compose logs -f backend
```

## Адреса

```text
Frontend: http://localhost/
Backend:  http://localhost:8080/
Swagger:  http://localhost:8080/swagger-ui.html
```

PostgreSQL по умолчанию не публикуется на host.

## Повторный запуск

Если images уже собраны и пересборка не нужна:

```bash
docker compose up -d
```

Если изменён backend/frontend source, Dockerfile или frontend build-time параметры, используйте `--build`.
