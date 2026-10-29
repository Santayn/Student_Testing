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
├── backend/
│   ├── Dockerfile          # backend image
│   ├── .dockerignore
│   └── ...                 # backend source
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

## Быстрый запуск в Windows

Секреты не входят в ZIP. Откройте PowerShell в корне проекта и один раз
запустите безопасный стартовый скрипт:

```powershell
.\start-local.ps1
```

Либо запустите двойным щелчком:

```text
start-local.cmd
```

Скрипт создаёт минимальный локальный `.env` с новыми случайными секретами, если файла ещё
нет. Если `.env` уже существует, скрипт сохраняет локальные параметры машины и при необходимости
обновляет только отсутствующие/некорректные секреты. После этого обычный
`docker compose up -d --build` также будет работать.

## Ручная настройка

Для проекта, полученного через Git, а не из готового ZIP:

```bash
cp .env.example .env
# Обязательно замените POSTGRES_PASSWORD и APP_JWT_SECRET в .env.
docker compose up -d --build
```

После запуска (при стандартном `BACKEND_PORT=8080`):

```text
Frontend: http://localhost/
Backend:  http://localhost:8080/
Postgres: доступен только внутри Docker-сети проекта
```

Если host-порт `8080` занят, достаточно добавить в корневой `.env`, например:

```env
BACKEND_PORT=8081
```

Этот же `BACKEND_PORT` автоматически использует Vite dev proxy при отдельном запуске frontend через `npm run dev`.

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

Локальный `.env` исключён из Git. Не публикуйте его; на сервере задайте отдельные значения
`POSTGRES_PASSWORD` и `APP_JWT_SECRET`. Корневой `.env.example` является единственным шаблоном
переменных проекта; отдельные frontend/backend `.env.example` не используются.

Vue Router history mode поддерживается через:

```nginx
try_files $uri $uri/ /index.html;
```

## ИИ: ключи API

Проверка свободных ответов, подсказка критериев, распознавание сканов лекций и презентации работают через внешний ИИ-сервис. Без ключа проект запускается как обычно — кнопки ИИ просто скрыты. Выгрузка текста лекции в DOCX/PDF ключа не требует.

Нужен **один ключ** — `APP_AI_API_KEY`. Сейчас используется Groq; ключ выдаёт руководитель проекта. Добавьте в корневой `.env` (он в `.gitignore`, **не коммитьте ключ** ни в `.env.example`, ни в код, ни в документацию):

```env
APP_AI_PROVIDER=openai
APP_AI_BASE_URL=https://api.groq.com/openai/v1
APP_AI_API_KEY=<ключ Groq>
APP_AI_MODEL_GRADING=qwen/qwen3.8-27b
APP_AI_MODEL_LECTURE=qwen/qwen3.8-27b
APP_AI_MODEL_PRESENTATION=openai/gpt-oss-120b
APP_AI_IMAGE_GENERATION_ENABLED=false
APP_AI_PRESENTATION_MAX_SOURCE_CHARS=20000
```

Бесплатный тариф Groq принимает до 8000 токенов в минуту на модель (примерно 20 000 символов русского текста), поэтому объём материалов для презентации ограничен. При массовой проверке ответов (группа сдаёт тест одновременно) запросы упираются в этот лимит и выполняются с повторами — проверка растягивается на минуты; для реальной эксплуатации нужен платный тариф Groq (Dev Tier) или другой провайдер.

Затем `docker compose up -d --build backend`. Проверка: под преподавателем `GET /api/v1/ai/status` возвращает `"enabled": true`.

| Что | Нужен ключ? | Где взять |
|---|---|---|
| Тексты: проверка, критерии, разметка сканов, план презентации | да, `APP_AI_API_KEY` | Groq (выдаёт руководитель) или другой провайдер — см. [ИИ](docs/technical/details/ai.md#провайдеры) |
| Распознавание текста со сканов | нет | Tesseract встроен в Docker-образ бэкенда |
| Поиск картинок (Wikimedia Commons, Openverse) | нет | — |
| Генерация картинок | ключ провайдера с `/images/generations` (у Groq нет — выключено) | — |
| Yandex AI Studio (альтернатива) | `APP_AI_API_KEY` + `APP_AI_FOLDER_ID` | Yandex Cloud |
| GigaChat (альтернатива) | `APP_AI_API_KEY` (ключ авторизации) | developers.sber.ru |

Мобильному приложению ключи не нужны — оно ходит в ИИ только через бэкенд. Все переменные: [переменные окружения](docs/deployment/details/environment-variables.md). Сценарии ручной проверки: [тест-кейсы ИИ](docs/testing/details/ai-test-cases.md).

## Мобильное приложение

Нативный клиент для Android и iOS (Kotlin Multiplatform + Compose Multiplatform) лежит в папке `mobile/`.
Он повторяет функциональность фронтенда для всех ролей. Запуск, архитектура и тесты описаны в [mobile/README.md](mobile/README.md).
