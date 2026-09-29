# Актуальные рекомендации frontend ↔ backend

Дата актуализации: **29.09.2026**.

## Правило проекта

Backend считается **неизменяемой константой** и в рамках текущей доработки проекта не изменяется.

Frontend дорабатывается максимально полно в пределах уже существующего backend-контракта. Если корректное решение технически требует изменения backend, рекомендация **не удаляется**, а сохраняется с пометкой:

> **ТРЕБУЕТ ИЗМЕНЕНИЯ BACKEND / НЕ РЕАЛИЗУЕТСЯ НА FRONTEND ПРИ ТЕКУЩЕМ КОНТРАКТЕ**

Такие пункты не входят в текущий frontend backlog и не должны имитироваться небезопасными или архитектурно слабыми клиентскими обходами.

---

# Состояние frontend-рекомендаций

Повторная сверка рекомендаций с текущим кодом frontend показала, что все ранее зафиксированные проблемы, которые можно было надёжно исправить **только на frontend**, закрыты.

Подтверждено наличие следующих механизмов:

- operation-specific timeout для обычных запросов, submit и файловых операций;
- защита от слепого повторного submit после неизвестного сетевого исхода;
- восстановление черновика незавершённой тестовой попытки через `sessionStorage`;
- привязка draft к `testId + assignmentId + attemptId` и fingerprint вопросов;
- workspace route isolation через `meta.workspaceRoles`;
- использование effective workspace mode вместо raw account roles там, где это требуется;
- защита route/context-driven загрузчиков через `createLatestRequestGuard()`;
- защита от stale responses и stale reactive context;
- regression/contract tests для перечисленных механизмов.

Соответствующие ранее закрытые пункты FE-4, FE-5, FE-6, FE-7 и FE-8 удалены из активного списка рекомендаций как выполненные.

На текущий момент **подтверждённых незакрытых frontend-only рекомендаций из прежнего файла нет**.

---

# Рекомендации, требующие изменения backend

## FE-9. Надёжный таймер теста нельзя реализовать по текущему student contract

**Приоритет:** 🟡 P2  
**Статус:** BACKEND BLOCKED  
**Пометка:** **ТРЕБУЕТ ИЗМЕНЕНИЯ BACKEND / НЕ РЕАЛИЗУЕТСЯ НА FRONTEND ПРИ ТЕКУЩЕМ КОНТРАКТЕ**

Backend возвращает в `PublicTestResponse`:

```text
duration
```

но `PublicTestAttemptLoadResponse` не содержит серверных временных данных, достаточных для authoritative countdown:

```text
startedAtUtc
effectiveDeadlineUtc
serverNowUtc
```

Добавление только локального frontend-countdown не решает задачу корректно:

- reload сбрасывает локальную точку старта;
- часы клиента могут отличаться от серверных;
- клиентский таймер можно обойти;
- frontend не может доказать истечение серверного времени;
- текущий backend-контракт не предоставляет authoritative deadline.

### Что требуется от backend в будущем

1. backend вычисляет authoritative deadline попытки;
2. start/resume DTO возвращает минимум `effectiveDeadlineUtc`;
3. backend проверяет deadline при submit;
4. после этого frontend отображает countdown и использует deadline только как UX-представление серверного ограничения.

### Что делать на frontend сейчас

**Ничего не имитировать.**

Не рекомендуется строить локальный таймер и выдавать его за реальное ограничение времени теста. Это создаст ложное ощущение корректности и добавит дублирующую бизнес-логику на клиенте.

---

## FE-10. HttpOnly refresh-session требует изменения auth-контракта backend

**Приоритет:** 🟡 P2 / security hardening  
**Статус:** BACKEND BLOCKED  
**Пометка:** **ТРЕБУЕТ ИЗМЕНЕНИЯ BACKEND / НЕ РЕАЛИЗУЕТСЯ НА FRONTEND ПРИ ТЕКУЩЕМ КОНТРАКТЕ**

Текущий auth API:

- возвращает refresh token в JSON;
- ожидает refresh token в body `/auth/refresh`;
- ожидает refresh token в body `/auth/revoke`.

Поэтому frontend вынужден иметь refresh token в JavaScript-доступном состоянии.

Frontend не способен самостоятельно превратить такой токен в `HttpOnly` cookie, потому что `HttpOnly` задаётся сервером через `Set-Cookie`.

### Что требуется от backend в будущем

Целевой web-contract:

```text
refresh token -> Secure + HttpOnly + SameSite cookie
access token  -> короткоживущий, предпочтительно in-memory
refresh       -> работает с cookie-сессией
revoke/logout -> инвалидирует cookie-сессию
```

При таком контракте отдельно потребуется определить CSRF/CORS/credentials policy.

### Что делать на frontend сейчас

Текущий auth flow не перестраивать искусственно.

Перенос refresh token между `localStorage`, `sessionStorage` или другим JS-доступным storage может уменьшить persistence, но **не является полноценной защитой от XSS token theft**.

---

# Итог

Активный frontend backlog из данного файла исчерпан.

Остались только две рекомендации, которые невозможно корректно закрыть без изменения backend:

| ID | Приоритет | Статус | Причина |
|---|---|---|---|
| FE-9 | 🟡 P2 | BACKEND BLOCKED | нет authoritative server deadline в student attempt contract |
| FE-10 | 🟡 P2 | BACKEND BLOCKED | refresh token contract требует JS-доступного token storage |

При текущем правиле проекта эти пункты **сохраняются как документация ограничений**, но не являются задачами текущей доработки frontend.

Следующие улучшения frontend следует формировать уже по новому аудиту качества кода, а не переносить закрытые пункты из старого списка.
