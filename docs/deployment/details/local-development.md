# Локальная разработка

## Общая схема

При разработке frontend/backend можно запускать отдельно от production Nginx:

```text
Vite dev server
      ↓ /api proxy
Spring Boot
      ↓
PostgreSQL
```

PostgreSQL при желании можно оставить в Docker, но для этого его порт необходимо отдельно опубликовать или использовать другой доступный PostgreSQL instance.

## Backend вне Docker

Backend требует обязательные datasource-параметры:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
APP_JWT_SECRET
```

На Windows из `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

Backend по умолчанию использует:

```text
SERVER_PORT=8080
```

### Unix/macOS

README содержит команду:

```bash
./mvnw spring-boot:run
```

но текущий `backend/mvnw` в поставляемом архиве имеет CRLF line endings и не сохраняет executable bit. До исправления wrapper необходимо нормализовать или использовать установленный Maven.

## Frontend вне Docker

Из `frontend/`:

```bash
npm ci
npm run dev
```

Vite автоматически выбирает development port и проксирует запросы `/api`.

### Текущий конфликт портов

`vite.config.js` сейчас содержит:

```text
target: http://localhost:8081
```

а backend default:

```text
8080
```

Поэтому при неизменённой конфигурации необходимо либо:

- запустить backend с `SERVER_PORT=8081`, либо
- исправить Vite proxy target на `8080`.

Канонический Docker-сценарий этой проблемы не имеет: Nginx проксирует API на Docker service `backend:8080`.

## CORS

При same-origin production proxy CORS обычно не участвует. При раздельных dev servers backend должен разрешать origin Vite-сервера через `APP_CORS_ALLOWED_ORIGINS`.

Текущий local default допускает localhost/127.0.0.1 с произвольным портом.
