# Student Testing Frontend

Vue-клиент системы Student Testing.

## Стек

- Vue 3
- Vite
- Vue Router
- Pinia
- Axios
- Vitest + Vue Test Utils

`primevue`, `tailwindcss` и `vite-plugin-vue-devtools` сохранены как подготовка к будущему этапу UI/UX и пока могут не использоваться в production-коде.

## Запуск

Из каталога `frontend`:

```bash
npm ci
npm run dev
```

Production-сборка:

```bash
npm run build
```

Тесты:

```bash
npm run test:unit -- --run
```

При запуске всего проекта через Docker используйте корневой `docker-compose.yml`.

## Структура `src`

```text
src/
├── api/          # HTTP-клиенты backend API
├── components/   # переиспользуемые UI и domain-компоненты
├── composables/  # общая реактивная логика экранов
├── config/       # frontend feature flags
├── router/       # routes, role constants и guards
├── stores/       # Pinia stores
├── utils/        # небольшие чистые функции и flow helpers
└── views/        # страницы приложения
```

## Роли и доступ

Рабочая часть приложения поддерживает роли:

- `STUDENT`
- `TEACHER`
- `ADMIN`

Роль `USER` сама по себе не даёт доступ к рабочему пространству. Для рабочего доступа также требуется привязанный `personId`.

Student-only страницы прохождения обучения используют `/api/v1/public/learning/**`. Teacher/Admin экраны используют соответствующие административные и преподавательские API.

Если одной учётной записи назначено несколько рабочих ролей, frontend использует единый активный режим (`STUDENT`, `TEACHER` или `ADMIN`). Режим можно переключить в шапке приложения; общие страницы предметов и результатов используют именно выбранный режим, а не самостоятельно выбирают роль по приоритету.

## Регистрация

Публичная регистрация управляется frontend-переменной:

```text
VITE_PUBLIC_REGISTRATION_ENABLED
```

В Docker она синхронизируется с backend-переменной `APP_PUBLIC_REGISTRATION_ENABLED`.

Новый аккаунт без завершённой привязки роли/Person остаётся на `/account-pending`.

## Backend-рекомендации

Frontend-only workaround'ы, которые архитектурно лучше перенести или дополнительно защитить на backend, фиксируются в:

```text
../recommendations/backend_recommendations.md
```

Backend не следует менять в рамках frontend-этапов без отдельного решения.
