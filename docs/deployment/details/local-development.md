# Локальная разработка

## Каноническая конфигурация

Корневые `.env` и `.env.example` являются единственным env-источником проекта. Отдельные env-файлы внутри `frontend/` или `backend/` не используются.

Реальный `.env` рекомендуется хранить минимальным. Пример машины, где `8080` занят:

```env
POSTGRES_PASSWORD=<local-secret>
APP_JWT_SECRET=<local-secret>
BACKEND_PORT=8081
```

## Рекомендуемый режим frontend-разработки

Backend и PostgreSQL можно оставить в Docker:

```bash
docker compose up -d postgres backend
```

Frontend запустить отдельно:

```bash
cd frontend
npm ci
npm run dev
```

Vite читает `../.env` через `envDir` и направляет `/api` на:

```text
http://127.0.0.1:${BACKEND_PORT:-8080}
```

Таким образом Docker mapping и Vite proxy используют один `BACKEND_PORT`. Внутренний Docker proxy Nginx при этом не меняется и продолжает использовать `backend:8080`.

## Backend вне Docker

При прямом запуске Spring Boot сам по себе не загружает dotenv-файл как shell environment. Необходимые значения нужно экспортировать в процесс/IDE либо использовать собственную run configuration. Минимально требуются datasource-параметры и `APP_JWT_SECRET`.

Windows из `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend default port — `8080`. `BACKEND_PORT` является настройкой host mapping/Vite proxy и не заменяет `SERVER_PORT` для прямого Spring Boot процесса.

## Unix/macOS

Текущий `backend/mvnw` в snapshot имеет известную CRLF/executable проблему. До исправления wrapper требуется нормализация или установленный Maven.

## CORS

При same-origin Docker/Nginx proxy CORS обычно не участвует. При отдельном Vite dev server backend local default разрешает localhost/127.0.0.1 с произвольным портом.
