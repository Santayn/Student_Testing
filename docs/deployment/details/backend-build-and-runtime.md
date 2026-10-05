# Backend build и runtime

## Multi-stage build

Backend image собирается из `backend/Dockerfile` с build context `./backend`.

### Build stage

```text
maven:3.9.9-eclipse-temurin-17
```

Последовательность:

```text
COPY pom.xml
mvn dependency:go-offline
COPY src
mvn -DskipTests package
```

Тесты при image build не выполняются.

### Runtime stage

```text
eclipse-temurin:17-jre
```

В image дополнительно устанавливаются:

```text
curl
postgresql-client
```

`curl` используется Docker healthcheck, а PostgreSQL client нужен `DatabaseBackupService`.

## Непривилегированный пользователь

Image создаёт system user/group:

```text
studenttest
```

и запускает Java process через:

```text
USER studenttest
```

Это уменьшает последствия возможного компрометации приложения относительно запуска от root.

## Storage directory

Container подготавливает:

```text
/app/uploads/lecture-materials
```

Compose монтирует туда `lecture-uploads` volume.

## JVM

Entry point:

```text
java $JAVA_OPTS -jar /app/app.jar
```

Поэтому JVM flags можно передать через `JAVA_OPTS`, если переменная будет задана container environment.

## Port

Приложение слушает `8080` внутри container.

## Readiness

Dockerfile healthcheck обращается к:

```text
http://127.0.0.1:8080/api/v1/status/readiness
```

и ожидает успешный HTTP response.

## Build ≠ тестирование

Поскольку используется `-DskipTests`, успешный `docker build` подтверждает сборку приложения, но не заменяет test suite. Полный процесс будет описываться в `docs/testing/`.
