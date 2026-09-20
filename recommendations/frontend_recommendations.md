# Аудит и рекомендации frontend ↔ backend

Дата проверки: **20.09.2026**.

## Границы аудита

Backend в рамках этой работы считается **неизменяемым контрактом**. Backend-код не исправляется.

В этот аудит включены только технические проблемы frontend и его взаимодействия с текущим backend:

- REST-контракты и DTO;
- авторизация и маршрутизация;
- состояние тестовой попытки;
- конкурентные/устаревшие async-запросы;
- обработка timeout/network error;
- сохранение клиентского состояния;
- техническая устойчивость интеграции.

**UI/UX, визуальное оформление, расположение элементов и удобство интерфейса намеренно не рассматриваются.**

Если надёжное исправление требует изменения backend-контракта, такой пункт помечен `BACKEND BLOCKED` и продублирован/уточнён в `backend_recommendations.md`. Реализовывать backend-часть сейчас не следует.

---

# Итоговая сводка

| ID | Приоритет | Статус | Проблема | Где исправлять |
|---|---|---|---|---|
| FE-4 | 🔴 P1 | CONFIRMED | Общий Axios timeout 15s опасен для submit теста с LLM и создаёт неопределённое состояние | frontend; надёжное решение частично backend-blocked |
| FE-5 | 🟠 P1/P2 | CONFIRMED | Незавершённые ответы теста полностью теряются после reload | frontend |
| FE-6 | 🟠 P2 | CONFIRMED | Один timeout 15s применяется к загрузке/скачиванию больших материалов | frontend / frontend nginx |
| FE-7 | 🟠 P2 | CONFIRMED | Router проверяет все роли аккаунта, а не выбранный `workspaceRole` | frontend |
| FE-8 | 🟠 P2 | CONFIRMED | Несколько route/context-driven view не используют latest-request guard | frontend |
| FE-9 | 🟡 P2 | BACKEND BLOCKED | Надёжный таймер теста невозможно построить по текущему student DTO | backend contract + frontend после него |
| FE-10 | 🟡 P2 | BACKEND BLOCKED | Безопасное хранение refresh-сессии с HttpOnly cookie не поддерживается текущим auth API | backend auth contract + frontend после него |

P0-проблем, требующих немедленной остановки frontend, в статическом аудите не обнаружено.

---

# Подтверждённые frontend-проблемы

## FE-4. Глобальный Axios timeout 15 секунд опасен для submit теста

**Приоритет: P1.**

`frontend/src/api/http.js`:

```js
const clientConfig = {
  ...
  timeout: 15000,
}
```

Этот timeout применяется ко всем операциям, включая:

```text
POST /public/learning/attempts/{attemptId}/submit
```

Backend выполняет оценивание текстовых ответов синхронно в submit path. При включённой local LLM конфигурация backend допускает timeout модели до:

```text
12 секунд на один inference
```

Несколько текстовых вопросов могут последовательно занять больше 15 секунд.

### Почему это опаснее обычного timeout

Submit не является простым read-запросом.

Возможен сценарий:

1. frontend отправил submit;
2. backend продолжает обработку;
3. через 15 секунд Axios завершает запрос как `ECONNABORTED`;
4. frontend показывает «сервер слишком долго отвечает»;
5. backend спустя несколько секунд успешно завершает attempt;
6. пользователь повторяет submit;
7. backend уже отвечает «attempt is not in progress».

То есть frontend не знает, завершилась операция или нет.

### Что можно исправить сейчас на frontend

1. Убрать единый timeout как правило для всех операций.
2. Ввести operation-specific timeout, например отдельный большой timeout для `submitAttempt`.
3. На timeout/network error после submit **не делать автоматический повтор неидемпотентного POST**.
4. Сохранять `pendingSubmit` context в `sessionStorage` до отправки.
5. После неоднозначной ошибки пытаться сверить attempt по `/results/student/data?testId=...` и `attemptId` до разрешения повторной сдачи.

### Ограничение

Это только mitigation. Полностью надёжный submit при долгом grading требует backend idempotency/status/async grading. Соответствующая backend-рекомендация дополнена отдельно.

---

## FE-5. Незавершённые ответы полностью теряются после reload

**Приоритет: P1/P2.**

Текущий `TestView` хранит ответы только в reactive memory:

```text
singleAnswers
multipleAnswers
textAnswers
matchingAnswers
```

`sessionStorage` используется только для **уже завершённой** попытки (`completedTestSession.js`).

При reload:

1. backend resume возвращает тот же attempt и тот же набор вопросов;
2. backend student start DTO не возвращает draft-ответы;
3. `initializeAnswers()` очищает все поля;
4. всё введённое до reload теряется.

Для длинных тестов это реальная потеря пользовательских данных.

### Что исправить только на frontend

Добавить отдельное хранилище draft-сессии, привязанное минимум к:

```text
attemptId
testId
assignmentId
questionIds
```

Сохранять изменения best-effort в `sessionStorage`.

После resume восстанавливать draft **только если**:

- backend вернул тот же `attemptId`;
- assignment/test context совпадает;
- question set совместим.

Для matching-вопросов нельзя полагаться только на индекс элемента: backend повторно перемешивает `matchingOptions`. Draft нужно хранить по стабильному значению пары/правой части, а при restore заново сопоставлять с текущим порядком.

Очищать draft после подтверждённого успешного submit.

Backend менять не требуется для локального recovery.

---

## FE-6. 15-секундный timeout конфликтует с разрешённым размером файлов

**Приоритет: P2.**

Backend разрешает:

```text
max-file-size: 50MB
max-request-size: 200MB
```

Frontend Nginx разрешает:

```text
client_max_body_size 200m
proxy_send_timeout 60s
proxy_read_timeout 60s
```

но Axios обрывает любую операцию через 15 секунд.

Это касается:

```text
lecturesApi.uploadMaterials()
lecturesApi.downloadMaterial()
learningApi.downloadMaterial()
questionsApi.importFile()
```

При реальном размере 50–200 MB или медленном соединении клиентский timeout становится самым жёстким ограничением.

### Что исправить

Ввести разные timeout-классы:

```text
обычные JSON request     -> 15s
file upload/download     -> >= proxy timeout или 0 с явной отменой пользователем
long command/submit      -> отдельный лимит
```

Лучше вынести значения в конфигурацию (`VITE_*_TIMEOUT_MS`) вместо одного hardcode.

При изменении timeout необходимо согласовать его с `frontend/docker/nginx.conf.template`, иначе браузер и proxy будут расходиться.

---

## FE-7. Workspace mode не является ограничением маршрутизации

**Приоритет: P2.**

В store уже существует единый выбранный режим:

```text
workspaceRole
isStudentMode
isTeacherMode
isAdminMode
```

и `WorkspaceMode.spec.js` прямо проверяет идею «one consistent mode for a multi-role account».

Однако `authGuard` проверяет route только через полный набор account roles:

```js
authStore.hasAnyRole(...roles)
```

Следствие для аккаунта `STUDENT + TEACHER`:

- выбран `STUDENT` mode;
- sidebar показывает student context;
- ручной переход на `/teacher/...` всё равно проходит route guard, потому что account содержит `TEACHER`.

Это не повышает backend privileges, но нарушает техническую изоляцию рабочего контекста и может запускать API-flow другой роли.

### Дополнительный пример

`LectureDetailsView.vue` использует:

```js
authStore.isStudent || authStore.isAdmin
```

вместо effective mode.

### Что исправить

Разделить два понятия:

```text
account roles       -> какие режимы вообще доступны
workspaceRole       -> какой frontend-flow разрешён сейчас
```

Для role-specific routes добавить проверку effective workspace mode. Общие маршруты (`/subjects`, `/results`, `/profile`) могут остаться shared и выбирать поведение внутри view.

Backend менять не требуется для frontend mode isolation.

Сильная server-side изоляция effective mode остаётся отдельной backend-рекомендацией №14.

---

## FE-8. Route/context-driven loaders не везде защищены от stale response

**Приоритет: P2.**

В проекте уже есть хороший utility:

```text
createLatestRequestGuard()
```

Он используется в `CourseTemplatesView`, `QuestionsView`, `TestEditorView`, `TopicLibraryView`, `LectureManagementView`, `ResultsView`, `TeachingTemplatesView`.

Но аналогичная защита отсутствует как минимум в:

```text
views/tests/TestView.vue                 <- самый опасный случай, выделен отдельно FE-2
views/lectures/SubjectLecturesView.vue
views/lectures/LectureDetailsView.vue
views/subjects/SubjectDetailsView.vue
views/admin/FacultySubjectsView.vue
views/teacher/TeacherWorkloadView.vue
```

При быстром изменении route/filter старый ответ способен перезаписать новый context. Даже если это не приводит к security bypass, возможны:

- данные старой лекции под новым URL;
- тесты старой лекции в новом контексте;
- прежний предмет после смены subjectId;
- прежняя выборка после смены фильтра;
- преждевременный `loading=false` из `finally` старого запроса.

### Что исправить

Для всех запросов, результат которых зависит от изменяемого route/filter/context:

```text
begin request
capture context
await
check request is current
only then commit state
```

Для mutable admin/teacher экранов additionally захватывать ID контекста непосредственно перед mutation и не читать `ref.value` повторно внутри long-running batch.

---

# Backend-blocked frontend work

## FE-9. Надёжный таймер теста нельзя реализовать по текущему student contract

**Статус: BACKEND BLOCKED.**

Backend возвращает в `PublicTestResponse`:

```text
duration
```

но `PublicTestAttemptLoadResponse` не содержит:

```text
startedAtUtc
effectiveDeadlineUtc
serverNowUtc
```

Frontend сейчас вообще не использует `duration`, но добавление только локального countdown не решит задачу корректно:

- reload сбросит локальную точку старта;
- часы клиента могут отличаться;
- клиентский таймер обходится;
- backend сам сейчас не контролирует duration/deadline.

### Правильный порядок

1. backend A4/A5 должен вычислять и проверять authoritative deadline;
2. start/resume DTO должен отдавать `effectiveDeadlineUtc` (желательно также `startedAtUtc`);
3. после этого frontend может отображать countdown и локально блокировать отправку после deadline.

До изменения backend не рекомендуется выдавать клиентский timer за реальное security-ограничение.

---

## FE-10. HttpOnly refresh-session требует изменения auth-контракта backend

**Статус: BACKEND BLOCKED / security hardening.**

`authStore` persist-ит:

```text
accessToken
refreshToken
expiration timestamps
user
activeWorkspaceRole
```

Текущий auth API возвращает refresh token в JSON и ожидает его снова в JSON body `/auth/refresh` и `/auth/revoke`. Поэтому frontend обязан держать refresh token в JavaScript-доступном состоянии.

Frontend может уменьшить срок локального хранения, но не может самостоятельно сделать refresh token `HttpOnly`.

Для полноценной XSS-resistant refresh-сессии backend должен перейти на контракт вида:

```text
refresh token -> Secure + HttpOnly + SameSite cookie
access token  -> короткоживущий, предпочтительно in-memory
refresh endpoint -> работает с cookie
```

После появления такого backend-контракта frontend auth-store и interceptors нужно будет упростить/перестроить.

До этого не следует считать перенос `localStorage -> sessionStorage` полноценным исправлением XSS token theft: это только уменьшение persistence.

---

# Что проверено и не считается ошибкой

## Student results без `subjectId`

Раннее предположение о том, что `/results/student/data` без фильтров всегда возвращает пустой список, **не подтвердилось**.

В backend `testIdsForCurrentStudent(null, null)` действительно возвращает пустой set, но при отсутствии `subjectId/testId` одновременно:

```text
filterByTestContext = false
```

поэтому этот set не применяется как фильтр. В результате backend возвращает завершённые попытки текущего студента по всем тестам.

Frontend-исправление здесь не требуется.

## Роли/permissions внутри JWT

Раннее предположение о рассинхронизации `/auth/me` и JWT-authorities также **не подтвердилось**.

`JwtAuthenticationFilter` на каждом API-запросе заново вызывает `UserRegisterService.loadUserByUsername()` и создаёт Spring Authentication с актуальными authorities из БД. Claims `roles/permissions` внутри старого JWT не являются источником endpoint-authorities.

Поэтому отдельный forced refresh после изменения роли только ради обновления backend authorities не нужен.

## Student response sanitization на frontend

Frontend уже удаляет из student-facing state:

```text
correctAnswer
correct
gradingStatus
gradingNote
questionPoints
awardedPoints
```

после получения result DTO.

Это полезно для локального state/session storage, но **не устраняет backend leak**, потому что исходный JSON уже пришёл в браузер. Backend-проблема A6 остаётся отдельной.

## Test creation rollback

Frontend уже имеет best-effort rollback через `createTestWithAssignments()`:

```text
create Test
-> create assignments sequentially
-> on error delete Test
```

Это разумный workaround при неизменяемом backend. Настоящая атомарность всё равно требует backend recommendation №1, поэтому повторно требовать ещё один frontend rollback слой не нужно.

---

# Рекомендуемый порядок frontend-исправлений

1. **FE-4** — разделить timeout по типам операций и добавить recovery для неоднозначного submit.
2. **FE-5** — сохранять draft незавершённой попытки по `attemptId`.
3. **FE-7** — привести route guards к effective `workspaceRole` для role-specific workspace.
4. **FE-8** — распространить latest-request pattern на оставшиеся context-driven loaders.
5. **FE-6** — согласовать file-transfer timeouts с Nginx и backend limits.
6. **FE-9/FE-10** — не имплементировать до появления соответствующего backend-контракта.

---

# Regression tests, которые стоит добавить вместе с исправлениями

Минимальный набор:

```text
ApiContracts.spec.js
- long-operation API methods use explicit timeout config

TestDraftSession.spec.js
- draft survives reload for same attemptId
- draft is discarded for another attemptId
- matching draft restores by stable value, not shuffled index
- draft clears after confirmed submit

WorkspaceRouteGuard.spec.js
- STUDENT+TEACHER in STUDENT mode cannot open teacher-only route
- the same account in TEACHER mode can open it
- shared routes remain available

LongOperationTimeout.spec.js
- submit/file operations do not inherit the generic 15s timeout
- timeout after submit does not trigger blind duplicate POST
```

---

# Ограничения проверки

Это **статический технический аудит** исходного кода и фактического Java backend-контракта.

Полный frontend build/unit suite в текущей среде не был запущен: присутствующий `node_modules` неполный (`vite` executable/package files отсутствуют), а повторная установка зависимостей не завершилась в доступной среде. Поэтому найденные проблемы подтверждены по control flow и контрактам, но после внесения исправлений необходимо отдельно выполнить:

```text
npm ci
npm run build
npm run test:unit -- --run
```

перед merge.
