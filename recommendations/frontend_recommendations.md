# Актуальные рекомендации frontend ↔ backend

Дата актуализации: **30.09.2026**.

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

---

## FE-LECTURE-01. Поддержка семантического DSL-содержимого лекции после расширения backend-контракта

**Приоритет:** 🟡 P2 / product capability  
**Статус:** BACKEND BLOCKED  
**Связанный backend-пункт:** `BE-LECTURE-01`  
**Пометка:** **ТРЕБУЕТ ИЗМЕНЕНИЯ BACKEND / НЕ РЕАЛИЗУЕТСЯ ПОЛНОСТЬЮ НА FRONTEND ПРИ ТЕКУЩЕМ КОНТРАКТЕ**

### Контекст

В frontend уже зафиксирован будущий семантический typography/DSL contract для лекций:

```text
[text]
[lead]
[section]
[subsection]
[heading]
[strong]
[note]
[quote]
[list]
[numbered]
[item]
[code]
[link]
[figure]
[caption]
```

Также зафиксирована единая визуальная система:

```text
Manrope
400 / 500 / 600 / 700
.lecture-content*
```

Однако текущий backend lecture contract не имеет отдельного поля для полноценного структурированного тела лекции. Существующие `title`, `description` и `contentFolderKey` не должны переиспользоваться как контейнер для DSL:

- `description` остаётся кратким описанием;
- `contentFolderKey` остаётся ключом файлового/материального контента;
- тело лекции должно иметь собственный versioned contract.

### Что должен сделать frontend после появления backend-контракта

После реализации `BE-LECTURE-01` frontend должен:

1. расширить lecture editor полем semantic lecture source;
2. отправлять/получать объект:

```json
{
  "content": {
    "format": "STUDENT_TESTING_DSL",
    "schemaVersion": 1,
    "source": "..."
  }
}
```

3. не хранить и не генерировать произвольные inline `style`;
4. не давать преподавателю свободный выбор:
   - `font-family`;
   - arbitrary `font-size`;
   - arbitrary `font-weight`;
   - arbitrary text/background color;
5. преобразовывать DSL в контролируемую структуру/AST;
6. рендерить AST через разрешённые Vue-компоненты/semantic elements;
7. не использовать unsanitized `v-html`;
8. валидировать ссылки и material references;
9. обрабатывать неизвестный `format`/`schemaVersion` как unsupported content, а не пытаться интерпретировать его эвристически;
10. сохранять текущие `description`, materials и `contentFolderKey` как независимые сущности интерфейса.

### Предпочтительная модель rendering

```text
backend content.source
        ↓
DSL parser
        ↓
validated AST
        ↓
controlled Vue markup
        ↓
.lecture-content*
```

Не рекомендуется:

```text
backend HTML
→ direct v-html
```

### Backward compatibility

Лекции без `content` должны продолжать нормально открываться:

```text
content == null
→ обычная лекция без текстового semantic body
```

Frontend migration не должна требовать массовой переработки существующих лекций.

### Acceptance condition

`FE-LECTURE-01` можно закрыть, когда:

1. backend поддерживает `BE-LECTURE-01`;
2. teacher editor сохраняет versioned DSL source;
3. student lecture view отображает его через controlled renderer;
4. raw HTML/inline styles не используются как канонический формат;
5. старые лекции без semantic content продолжают работать;
6. parser/renderer покрыт unit/security regression tests.


# Итог

Frontend-only backlog, не требующий изменения backend, по-прежнему исчерпан.

Остались рекомендации, которые невозможно корректно закрыть без изменения backend:

| ID | Приоритет | Статус | Причина |
|---|---|---|---|
| FE-9 | 🟡 P2 | BACKEND BLOCKED | нет authoritative server deadline в student attempt contract |
| FE-10 | 🟡 P2 | BACKEND BLOCKED | refresh token contract требует JS-доступного token storage |
| FE-LECTURE-01 | 🟡 P2 | BACKEND BLOCKED | текущий Lecture API не хранит versioned semantic body лекции |
| FE-PERF-02 | 🟡 P2 | BACKEND BLOCKED | refresh и `/auth/me` образуют безопасный, но последовательный startup waterfall; нужен backend restore contract с identity |

При текущем правиле проекта эти пункты **сохраняются как документация ограничений**, но не являются задачами текущей доработки frontend.

Следующие улучшения frontend следует формировать уже по новому аудиту качества кода, а не переносить закрытые пункты из старого списка.


---

## FE-PERF-02 — Не ломать auth bootstrap ради параллелизации `refresh` и `/auth/me`

**Приоритет:** 🟡 P2  
**Статус:** BACKEND BLOCKED  
**Связано с:** `BE-PERF-02`

### Наблюдение

Production cold-start на Slow 4G подтвердил последовательный auth bootstrap:

```text
POST /auth/refresh
        ↓
GET /auth/me
        ↓
router.isReady()
        ↓
mount / first protected route
```

При измерении оба auth-запроса занимали примерно по одному сетевому RTT, поэтому истёкший persisted access token добавляет около одного лишнего последовательного RTT к старту приложения.

### Почему frontend не должен просто распараллелить запросы

Текущая зависимость корректна:

```text
refresh
→ получить новый access token
→ /auth/me с новым access token
```

Нельзя заменять её на:

```js
await Promise.all([
  refreshSession(),
  loadCurrentUser(),
])
```

потому что `/auth/me` может уйти со старым/истёкшим access token.

Также не рекомендуется ради ускорения:

- использовать persisted `user` как authoritative security context;
- монтировать защищённый workspace до завершения восстановления актуальной identity;
- ослаблять `ensureAccessToken()` / session epoch protection;
- отправлять защищённые запросы до завершения token refresh.

### Frontend contract после backend-исправления

Если `BE-PERF-02` будет реализован и refresh/restore endpoint начнёт возвращать актуального пользователя вместе с новой token pair, frontend должен:

1. принять token pair;
2. принять актуальный `user` из того же успешного ответа;
3. вызвать `setSessionTokens(...)`;
4. вызвать `setUser(...)`;
5. **не выполнять отдельный `/auth/me`** в этой ветке bootstrap;
6. сохранить текущие session epoch / stale-session / fail-closed гарантии;
7. оставить `/auth/me` для сценария, когда access token ещё жив и identity нужно восстановить отдельно.

### Ожидаемый результат

Worst-case authenticated startup:

```text
сейчас:
refresh → me → router

после BE-PERF-02:
refresh/restore + identity → router
```

То есть убирается один последовательный network round-trip без ослабления security model.

### Acceptance condition

`FE-PERF-02` можно закрыть, когда:

- backend поддерживает `BE-PERF-02`;
- bootstrap не делает отдельный `/auth/me` после successful refresh/restore с identity;
- сценарий с ещё живым access token остаётся корректным;
- guest startup по-прежнему не делает лишних auth-запросов;
- session epoch / refresh single-flight / stale-response regression tests остаются зелёными.


---

## FE-ERR-01 — Использовать machine-readable `code`, не парсить backend message

**Приоритет:** 🟠 P1  
**Статус:** READY / BACKEND COMPATIBLE  
**Связано с:** `BE-ERR-01`

Frontend Stage 3 уже поддерживает code-first обработку:

```text
known code
→ domain-specific UX/message

unknown/missing code
→ status fallback
→ backend message
→ frontend fallback
```

Запрещено определять бизнес-причину по подстрокам в `message`.

Первые поддерживаемые domain codes:

```text
GROUP_HAS_DEPENDENCIES
FACULTY_HAS_DEPENDENCIES
SUBJECT_HAS_DEPENDENCIES
TOPIC_HAS_DEPENDENCIES
```

После реализации `BE-ERR-01` новые бизнес-коды добавляются только через
централизованный registry/contract tests, а не локальными строковыми сравнениями
в отдельных view/composable.
