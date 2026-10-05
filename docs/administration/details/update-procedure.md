# Обновление системы

## Базовый механизм

Текущий deployment обновляется пересборкой images:

```bash
docker compose up -d --build
```

Named volumes при этом сохраняются.

## Безопасная последовательность

### До обновления

1. зафиксировать текущую версию исходников/commit;
2. создать SQL backup;
3. сохранить `lecture-uploads`;
4. сохранить `.env` в защищённом месте;
5. проверить свободное место;
6. прочитать изменения схемы/configuration.

### Обновление

```bash
docker compose up -d --build
```

### После обновления

```bash
docker compose ps
```

проверить:

```text
PostgreSQL healthy
Backend readiness 200
Frontend /health = ok
```

Далее выполнить минимальный regression/smoke:

- login ADMIN;
- login STUDENT;
- открыть предмет/лекцию;
- проверить teacher workspace;
- проверить доступ к материалу;
- при изменениях тестирования — выполнить test attempt flow.

## Почему backup обязателен

Нет versioned database migrations и automated rollback. `ddl-auto=update`/`schema.sql` могут изменить схему так, что предыдущая версия backend больше не будет с ней совместима.

## Build-time frontend settings

Изменения `VITE_*` требуют rebuild frontend, а не только restart.
