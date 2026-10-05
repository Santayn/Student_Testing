# Сеть и порты

## Docker Compose

| Сервис | Container port | Host mapping |
|---|---:|---|
| frontend | 80 | `0.0.0.0:80` |
| backend | 8080 | `127.0.0.1:${BACKEND_PORT:-8080}` |
| postgres | 5432 | не публикуется |

## Frontend

Пользователь открывает:

```text
http://localhost/
```

Frontend container публикуется на всех host interfaces через `80:80`. Текущий Compose не содержит переменной для изменения host frontend port.

## Backend

Backend публикуется только на loopback:

```text
127.0.0.1:8080
```

Это удобно для локального Swagger/диагностики и одновременно не делает backend непосредственно доступным на внешних сетевых интерфейсах host.

Изменить host-порт можно:

```dotenv
BACKEND_PORT=8081
```

Внутри Docker network backend всё равно работает на `8080`.

## PostgreSQL

Порт PostgreSQL не опубликован. Backend использует внутреннее имя Docker service:

```text
jdbc:postgresql://postgres:5432/student_test
```

Для подключения внешнего SQL-клиента к локальному PostgreSQL потребуется временно добавить port mapping или использовать `docker compose exec`.

## API proxy

Nginx принимает:

```text
/api/**
```

и проксирует запрос без смены path на:

```text
http://backend:8080/api/**
```

Поэтому production frontend использует относительный API base:

```text
/api/v1
```

без знания адреса backend container.

## CORS

В штатном browser-flow frontend и API имеют один origin, поскольку API проходит через Nginx. CORS важен главным образом при прямом доступе к backend или раздельном development server.
