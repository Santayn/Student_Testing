# Composables и state

## Глобальный state

Pinia используется для действительно глобального состояния: auth/session/workspace и theme.

Не переносить состояние каждой формы или таблицы в global store.

## Feature state

Предпочтительное место — composable.

Хорошая декомпозиция:

```text
useXxxData
→ query/filter/loading

useXxxMutations
→ create/edit/delete/form
```

## Async lifecycle

Для контекстных запросов использовать существующие механизмы:

- `AbortController`;
- abortable request guard;
- latest-request semantics;
- session epoch для auth-sensitive flows.

Старый response не должен перезаписать новый Subject/Group/User context.

## Cache

Memory cache должен учитывать security context и инвалидироваться после mutation/logout.

## Draft

`sessionStorage` — временный UX-state, а не server source of truth.

## Forms

Перед новой form infrastructure проверить существующие `useOverlayForm`, form error lifecycle, focus helpers, unsaved changes и API error presentation.
