# Структура репозитория

## Верхний уровень

```text
Student_Testing/
├── backend/                 Spring Boot приложение
├── frontend/                Vue SPA
├── docs/                    документация
├── recommendations/         аудиты и рекомендации
├── docker-compose.yml       локальный stack
├── Dockerfile               backend image
├── start-local.ps1
├── start-local.cmd
└── .env.example
```

## Backend

```text
backend/src/main/java/org/santayn/testing/
├── config/
├── models/
├── repository/
├── security/
├── service/
└── web/
    ├── advice/
    ├── controller/rest/
    └── dto/
```

`models/` дополнительно разделён по предметным областям. `repository/` и `service/` в основном плоские.

## Frontend

```text
frontend/src/
├── api/
├── components/
│   ├── ui/
│   └── ...
├── composables/
├── config/
├── navigation/
├── router/
├── stores/
├── utils/
├── views/
└── __tests__/
```

## Документация как часть репозитория

При изменении поведения нужно проверять:

- `project-description/` — возможности;
- `user-guide/` — действия пользователя;
- `technical/` — архитектуру;
- `deployment/` — runtime/config;
- `api/` — HTTP-контракт;
- `testing/` — проверки;
- `development/` — developer workflow.

## Текущий naming debt

`pom.xml` и главный backend-класс сохраняют starter-наименования `com.example`, `demo`, `DemoApplication`. Это не ломает runtime, но ухудшает onboarding.
