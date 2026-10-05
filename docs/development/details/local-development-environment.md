# Локальная среда разработки

## Два режима

Разработчик может использовать Docker Compose или раздельно запускать frontend/backend.

Docker-путь подробно описан в [deployment](../../deployment/README.md).

## Frontend локально

`package.json` требует:

```text
Node.js ^22.18.0 или >=24.12.0
npm
```

Команды:

```bash
cd frontend
npm ci
npm run dev
```

Полная проверка:

```bash
npm run quality
```

### Текущий proxy-нюанс

Vite проксирует `/api` на `http://localhost:8081`, а backend по умолчанию работает на `8080`. До синхронизации конфигурации разработчику нужно либо запускать backend на 8081, либо менять proxy.

## Backend локально

Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Требуется Java 17. Runtime профиль также требует PostgreSQL.

### Unix/macOS

Текущий `backend/mvnw` в snapshot имеет CRLF и в ZIP теряет executable bit. Поэтому `./mvnw` нельзя считать надёжным onboarding-сценарием до исправления `.gitattributes`/wrapper.

## IDE

IDE должна:

- использовать Java 17;
- уважать Prettier для frontend;
- не преобразовывать shell scripts в CRLF;
- не добавлять IDE-generated файлы в Git.

## Environment

Backend env — runtime. `VITE_*` — build/dev-time. Наличие параметра в `application.yml` не означает, что Docker Compose уже пробрасывает его в container.
