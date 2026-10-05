# Frontend build и Nginx

## Multi-stage image

Frontend Dockerfile использует два этапа:

```text
node:22-alpine
    ↓ npm ci
    ↓ npm run build
Vue dist
    ↓
nginx:1.27-alpine
```

Runtime image не содержит Node.js development server.

## Build-time frontend variables

Docker build принимает:

```text
APP_PUBLIC_REGISTRATION_ENABLED
VITE_API_TIMEOUT_MS
VITE_SUBMIT_TIMEOUT_MS
VITE_FILE_TRANSFER_TIMEOUT_MS
```

Они попадают в bundle на этапе `npm run build`.

Изменение этих параметров требует пересборки frontend image.

## Nginx responsibilities

Nginx:

- обслуживает `/usr/share/nginx/html`;
- делает Vue Router history fallback;
- проксирует `/api/**`;
- устанавливает security headers;
- ограничивает `client_max_body_size` значением `200m`;
- задаёт proxy timeouts;
- кэширует hashed assets.

## SPA fallback

```nginx
try_files $uri $uri/ /index.html;
```

позволяет открывать frontend routes напрямую, не получая Nginx `404`.

## Cache policy

Assets:

```text
1 year
Cache-Control: public, immutable
```

`index.html`:

```text
no-store, no-cache, must-revalidate
```

Так новая версия HTML должна подхватываться сразу, а versioned assets можно кэшировать длительно.

## API proxy

Runtime script подставляет `BACKEND_HOST`, `BACKEND_PORT` и proxy timeout values в Nginx template.

Compose использует:

```text
BACKEND_HOST=backend
BACKEND_PORT=8080
```

## Health

Frontend image имеет healthcheck:

```text
GET http://127.0.0.1/health
```

Он проверяет Nginx, но не backend/database readiness.
