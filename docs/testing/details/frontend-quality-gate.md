# Frontend quality gate

Каноническая агрегирующая команда frontend:

```bash
npm run quality
```

Она последовательно выполняет:

```text
npm run lint
   ↓
npm run quality:static
   ↓
npm run test:unit -- --run
   ↓
npm run build
```

## Что подтверждает успешный gate

1. ESLint не обнаружил нарушений правил.
2. Custom static quality не обнаружил запрещённых конструкций, broken internal imports и unreachable production modules.
3. Vitest suite завершился успешно.
4. Production bundle собирается Vite.

## Чего gate не подтверждает

- backend tests;
- реальный backend API;
- PostgreSQL;
- browser E2E;
- Docker Compose startup;
- visual regression;
- production deployment.

## Требования

`package.json` требует Node:

```text
^22.18.0 || >=24.12.0
```

Перед первым запуском quality gate требуется:

```bash
npm ci
```

## Состояние текущего audit snapshot

Custom static-quality script был запущен отдельно и завершился успешно:

```text
Static quality check passed: 201 production JS/Vue modules are reachable and clean.
```

Полный `npm run quality` в среде аудита не был подтверждён, поскольку архив содержит неполный `node_modules`, а внешняя установка зависимостей в среде анализа недоступна. Поэтому документация не утверждает, что текущий snapshot прошёл весь Vitest suite.
