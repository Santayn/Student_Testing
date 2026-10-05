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

### Vite proxy и корневой `.env`

Vite использует корневой `.env` (`envDir` указывает на корень проекта). `BACKEND_PORT` является единым host-port override для Docker backend и target dev proxy. При отсутствии значения используется `8080`. Отдельный `frontend/.env` не нужен.

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

Корневой `.env` хранит только локальные overrides/секреты, а `.env.example` документирует доступные параметры всего проекта. `VITE_*` остаются публичными build/dev-time значениями; backend `APP_*`/`SPRING_*` — runtime. Compose явно пробрасывает поддерживаемые backend overrides.
