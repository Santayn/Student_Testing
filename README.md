# Student Test: Vue frontend Docker integration

Текущий корневой `docker-compose.yml` уже имеет backend service:

```yaml
backend:
  ...
  environment:
    SERVER_PORT: 8080
```

Поэтому frontend использует:

```yaml
BACKEND_HOST: backend
BACKEND_PORT: "8080"
```

Итоговая структура:

```text
project-root/
├── docker-compose.yml
├── Dockerfile              # существующий backend Dockerfile
├── backend/                # backend source
└── frontend/
    ├── package.json
    ├── package-lock.json
    ├── vite.config.js
    ├── src/
    ├── Dockerfile
    ├── .dockerignore
    └── docker/
        ├── nginx.conf.template
        └── 40-render-backend.sh
```

Запуск:

```bash
cp .env.example .env
# Обязательно замените POSTGRES_PASSWORD и APP_JWT_SECRET в .env.
docker compose up -d --build
```

После запуска:

```text
Frontend: http://localhost/
Backend:  http://localhost:8080/
Postgres: localhost:5432 (доступен только с этого компьютера)
```

Nginx frontend автоматически проксирует:

```text
/api/** -> http://backend:8080/api/**
```

Поэтому Vue/Axios должен продолжать работать с:

```text
/api/v1
```

без отдельного production URL backend. Секреты запуска хранятся в локальном
`.env`, который исключён из Git.

Vue Router history mode поддерживается через:

```nginx
try_files $uri $uri/ /index.html;
```
