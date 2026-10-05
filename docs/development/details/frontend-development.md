# Frontend development

## Целевая цепочка

```text
View
 ↓
Composable
 ↓
API module
 ↓
http.js
 ↓
Backend
```

UI:

```text
View / feature component
 ↓
@/components/ui
 ↓
PrimeVue
```

## View

Route-level View отвечает за layout и orchestration. Новый сложный View не должен становиться контейнером всей HTTP/business logic.

## Composable

Composable хранит feature-state:

- loading/error;
- filters;
- forms;
- API orchestration;
- cancellation/latest request;
- mutation lifecycle.

## API module

Все HTTP-вызовы должны идти через `src/api/`. Прямой Axios в business View/component нарушает текущую архитектурную границу.

## JavaScript

Frontend полностью JavaScript; TypeScript отсутствует. Поэтому API shapes и invariants особенно важно защищать tests и docs.

## Dead code

`quality:static` требует, чтобы production `.js/.vue` modules были достижимы от `main.js`. Новый файл должен реально подключаться к приложению или barrel export.
