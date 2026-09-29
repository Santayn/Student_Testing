# Frontend recommendations — update after Stage 27

## FE-NEW-05 — Technical IDs in ordinary UI

**Status:** CLOSED by Stage 27.

Новый UI contract:

```text
ordinary UI:
semantic label -> semantic fallback

admin/support/recovery:
technical ID allowed when it materially helps identify an entity
```

Stage 27 не меняет backend contracts, auth, cache, mutations или routing identifiers.

## Remaining backend-blocked frontend items

Без изменений остаются ранее зафиксированные ограничения:

- authoritative attempt timer требует server-provided deadline;
- HttpOnly refresh-session contract требует backend changes;
- полный elimination student-context fan-out требует aggregated backend endpoint.
