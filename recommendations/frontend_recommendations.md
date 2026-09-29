# Актуальные рекомендации frontend ↔ backend

Дата актуализации: **29.09.2026**.  
Статус после Stage 28: **authoritative registry**.

## Правило проекта

Backend считается **неизменяемой константой** в рамках текущей frontend-доработки.

Frontend исправляется максимально полно в пределах существующего backend-контракта. Если корректное решение технически требует изменения backend, пункт сохраняется с пометкой:

> **BACKEND BLOCKED / НЕ РЕАЛИЗУЕТСЯ НА FRONTEND ПРИ ТЕКУЩЕМ КОНТРАКТЕ**

Такие пункты не должны имитироваться небезопасными или архитектурно слабыми клиентскими обходами.

---

# 1. Текущее состояние frontend

После Stage 24–27 и финального re-audit:

```text
frontend-only P0: 0
frontend-only P1: 0
frontend-only P2: 0
```

Закрыты и подтверждены:

- startup/bootstrap fallback;
- root runtime error boundary;
- ESLint + Vue lint quality gate;
- custom static-quality gate;
- duplicate assignment read в student learning context;
- сокращение cold-load critical path;
- semantic UI fallbacks вместо технических `#id`;
- stale/race/session/workspace protections;
- test-attempt draft recovery;
- API-contract hardening;
- accessibility/responsive foundation.

Активного standalone frontend-only backlog сейчас нет.

---

# 2. FE-9 — Authoritative timer тестовой попытки

**Приоритет:** P2  
**Статус:** BACKEND BLOCKED

Backend возвращает `duration`, но student attempt contract не предоставляет серверных временных данных, достаточных для authoritative countdown:

```text
startedAtUtc
effectiveDeadlineUtc
serverNowUtc
```

Frontend не может корректно восстановить оставшееся время после reload и не должен сам становиться источником бизнес-истины по deadline.

## Что требуется от backend

1. backend вычисляет authoritative deadline;
2. start/resume DTO возвращает минимум `effectiveDeadlineUtc`;
3. backend проверяет deadline при submit/complete;
4. frontend отображает countdown только как UX-представление серверного ограничения.

## Что делать frontend сейчас

Не имитировать серверный deadline локальным таймером.

---

# 3. FE-10 — HttpOnly refresh-session

**Приоритет:** P2 / security hardening  
**Статус:** BACKEND BLOCKED

Текущий auth API:

- возвращает refresh token в JSON;
- ожидает refresh token в body `/auth/refresh`;
- ожидает refresh token в body `/auth/revoke`.

Frontend поэтому вынужден держать refresh token в JavaScript-доступном состоянии.

Frontend не может самостоятельно превратить такой token в `HttpOnly` cookie.

## Что требуется от backend

Целевой web-contract:

```text
refresh token -> Secure + HttpOnly + SameSite cookie
access token  -> короткоживущий, предпочтительно in-memory
refresh       -> cookie-based session
revoke/logout -> инвалидирует cookie-session
```

Также потребуется определить CORS/credentials/CSRF policy.

## Что делать frontend сейчас

Не перестраивать auth-flow искусственно и не считать перенос между `localStorage`/`sessionStorage` полноценной XSS-защитой.

---

# 4. FE-PERF-01 — Student learning context fan-out

**Приоритет:** P2 performance  
**Статус:** PARTIAL / BACKEND BLOCKED  
**Связанный backend item:** `BE-PERF-01`

## Что уже закрыто на frontend в Stage 26

- enrolled teaching assignment не перечитывается по ID, если он уже пришёл из group assignment query;
- group reads, assignment-list reads и enrollment reads запускаются одной async-фазой;
- fallback `getAssignment(id)` сохранён только для отсутствующих assignment;
- существующие TTL/cache/invalidation semantics сохранены;
- не добавлены новые stale-prone cache layers.

## Что остаётся

Frontend всё ещё реконструирует контекст через несколько REST-ресурсов:

```text
group memberships
→ groups
→ faculties
→ teaching assignments
→ enrollments
→ subject memberships
→ subjects
```

Request count остаётся зависимым от числа связей.

## Почему не нужно продолжать frontend-only оптимизацию

Не рекомендуется:

- загружать широкие `getAll()` каталоги ради сокращения request count;
- увеличивать TTL access-related сущностей;
- вводить большой normalized entity cache только для компенсации backend granularity.

## Что требуется от backend

Нужен агрегированный student learning context endpoint с server-side object-level filtering.

После появления endpoint frontend должен:

1. перейти на aggregate contract;
2. временно оставить старый traversal только как migration fallback при необходимости;
3. затем удалить устаревший graph traversal;
4. сохранить session/workspace invalidation semantics.

---

# 5. Закрытые пункты, важные для истории

## FE-NEW-05 — Technical IDs in ordinary UI

**Статус:** CLOSED by Stage 27.

Зафиксированный UI contract:

```text
ordinary UI:
semantic label -> semantic fallback

admin/support/recovery:
technical ID допустим, если он реально нужен для идентификации
```

Stage 27 не менял backend contracts, auth, cache, mutations или routing IDs.

---

# 6. Итоговый активный frontend registry

| ID | Приоритет | Статус | Причина |
|---|---|---|---|
| FE-9 | P2 | BACKEND BLOCKED | нет authoritative server deadline |
| FE-10 | P2 | BACKEND BLOCKED | refresh-token contract требует JS-accessible token |
| FE-PERF-01 | P2 | PARTIAL / BACKEND BLOCKED | остаточный REST fan-out требует aggregate backend endpoint |

## Правило дальнейшей работы

Новый frontend Stage открывать только если появляется:

- реальный bug/regression;
- новая feature requirement;
- измеренный performance symptom;
- изменение backend-контракта;
- новое архитектурное требование.

Новый рефакторинг «на всякий случай» не нужен.
