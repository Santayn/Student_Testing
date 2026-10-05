# Системные требования

## Docker-сценарий

Для стандартного локального запуска требуется:

- Docker Engine или Docker Desktop;
- Docker Compose v2, доступный как `docker compose`;
- свободный TCP-порт `80` для frontend;
- свободный TCP-порт `8080` или другой `BACKEND_PORT` для локального доступа к backend;
- интернет при первой сборке образов и загрузке зависимостей.

На host-машине **не требуются** Java, Maven, Node.js, npm, PostgreSQL или Nginx.

## Почему первому build нужен интернет

Docker должен получить базовые образы:

```text
postgres:16-alpine
maven:3.9.9-eclipse-temurin-17
eclipse-temurin:17-jre
node:22-alpine
nginx:1.27-alpine
```

Кроме того:

- Maven загружает backend dependencies;
- `npm ci` загружает frontend dependencies.

После заполнения Docker/Maven/npm cache повторные сборки обычно требуют меньше сетевых обращений.

## Раздельный backend development

Для запуска backend вне Docker необходимы:

- Java 17;
- Maven Wrapper или совместимый Maven;
- доступный PostgreSQL;
- корректно заданные `SPRING_DATASOURCE_*` и `APP_JWT_SECRET`.

На Windows используется:

```powershell
.\mvnw.cmd spring-boot:run
```

Текущий Unix wrapper требует исправления line endings/permissions; см. [Ограничения развёртывания](./deployment-limitations.md).

## Раздельный frontend development

`frontend/package.json` требует:

```text
Node.js ^22.18.0 или >=24.12.0
```

Необходимы Node.js и npm. Установка зависимостей выполняется через:

```bash
npm ci
```

## Ресурсы

В Compose сейчас не заданы жёсткие CPU/RAM limits. Для локальной среды Docker Desktop должен иметь достаточно памяти одновременно для PostgreSQL, JVM, сборки Maven/Node и Nginx.

Для production resource requests/limits должны определяться инфраструктурой отдельно.
