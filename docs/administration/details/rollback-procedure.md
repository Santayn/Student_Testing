# Rollback

## Ограничение текущего deployment

Images имеют теги:

```text
student-test-backend:latest
student-test-frontend:latest
```

Versioned image registry/rollback policy не настроены.

Поэтому rollback приложения обычно означает:

```text
1. вернуть предыдущий source revision
2. пересобрать images
3. при необходимости восстановить БД и файлы
```

## Когда достаточно rollback кода

Только если новая версия:

- не изменила persistent data несовместимо;
- не изменила schema;
- не записала данные в новом формате.

## Когда нужен restore

Если обновление изменило schema/data semantics, возврат старого JAR/SPA может быть недостаточен.

Тогда нужен согласованный pre-update snapshot:

```text
previous application revision
+
DB backup
+
lecture-files backup
```

## Рекомендуемая последовательность

```text
1. прекратить пользовательские mutations
2. сохранить текущее аварийное состояние для анализа
3. вернуть предыдущий revision
4. восстановить DB snapshot при необходимости
5. восстановить соответствующий lecture-files snapshot
6. rebuild/restart
7. health checks
8. regression checks
```

## Целевое улучшение

Для управляемого rollback нужны:

- versioned Docker tags;
- release manifest;
- migrations с down/compatibility strategy;
- documented release artifacts.
