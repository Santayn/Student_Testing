# Обзор эксплуатации

## Назначение раздела

Administration-документация описывает поддержку работающей Student Testing после развёртывания. Основной текущий сценарий — один Docker host с тремя сервисами:

```text
Browser
   ↓
frontend (Nginx)
   ↓
backend (Spring Boot)
   ↓
PostgreSQL
```

Система также хранит материалы лекций отдельно от базы данных.

## Что относится к эксплуатации

Оператор отвечает за:

1. доступность Docker host;
2. корректный `.env` и сохранность секретов;
3. состояние `postgres`, `backend` и `frontend`;
4. сохранность `postgres-data` и `lecture-uploads`;
5. резервное копирование БД и файлов;
6. обновления и rollback;
7. контроль административного доступа;
8. анализ логов и health endpoints;
9. проведение восстановления в контролируемое окно.

## Что не автоматизировано

В текущей версии нет отдельной платформы эксплуатации:

- orchestrator/cluster;
- централизованных логов;
- метрик и alerting;
- scheduler резервных копий;
- versioned DB migrations;
- maintenance mode;
- automated rollback;
- admin bootstrap CLI;
- password recovery workflow.

Поэтому текущую operational-модель следует рассматривать как **single-host local/demo или малую управляемую инсталляцию**, а не как готовую высокодоступную production-платформу.

## Связанные разделы

- запуск и переменные среды: [Deployment](../../deployment/README.md);
- пользовательские действия ADMIN: [User Guide](../../user-guide/details/admin/README.md);
- устройство runtime и security: [Technical](../../technical/README.md);
- контракты административных API: [API](../../api/README.md).
