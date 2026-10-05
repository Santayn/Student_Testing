# Быстрый запуск в Windows

## Рекомендуемый способ

Из корня проекта можно запустить:

```text
start-local.cmd
```

двойным щелчком или выполнить в PowerShell:

```powershell
.\start-local.ps1
```

Скрипт предназначен для локальной Docker-среды.

## Что делает скрипт

1. Переходит в корень проекта.
2. Проверяет наличие `.env`.
3. Проверяет `POSTGRES_PASSWORD` и `APP_JWT_SECRET`.
4. При необходимости создаёт случайные секреты.
5. Проверяет наличие команды `docker`.
6. Выполняет:

```text
docker compose up -d --build
```

7. Выводит адрес приложения:

```text
http://localhost/
```

## Генерация секретов

Если `.env` отсутствует, скрипт создаёт:

- `POSTGRES_PASSWORD` из 24 криптографически случайных байт;
- `APP_JWT_SECRET` из 48 криптографически случайных байт.

Значения записываются в hexadecimal-представлении.

## Что происходит с некорректным `.env`

Если существующий файл не содержит:

- `POSTGRES_PASSWORD` длиной хотя бы 12 символов;
- `APP_JWT_SECRET` длиной хотя бы 32 символа,

старый файл копируется в:

```text
.env.backup-YYYYMMDD-HHMMSS
```

после чего скрипт заменяет или добавляет только некорректные секреты. Остальные локальные параметры, включая `BACKEND_PORT`, сохраняются.

Если `.env` отсутствует, создаётся минимальный файл только с локальными секретами. Остальные значения берутся из defaults проекта.

## Запуск без пересборки

После успешной первоначальной сборки можно выполнить:

```powershell
.\start-local.ps1 -NoBuild
```

Скрипт вызовет `docker compose up -d` без `--build`.

Используйте этот режим только если исходный код и build-time параметры frontend не менялись.

## Проверка результата

```powershell
docker compose ps
```

Ожидаются три сервиса:

```text
student-test-postgres
student-test-backend
student-test-frontend
```

После выхода frontend в healthy-состояние приложение доступно по адресу:

```text
http://localhost/
```

Backend для локальной диагностики доступен через `BACKEND_PORT`:

```text
http://localhost:${BACKEND_PORT:-8080}/
```

Swagger:

```text
http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html
```
