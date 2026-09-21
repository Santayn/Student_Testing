# Аудит и рекомендации frontend ↔ backend

Дата проверки: **21.09.2026**.

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
| FE-9 | 🟡 P2 | BACKEND BLOCKED | Надёжный таймер теста невозможно построить по текущему student DTO | backend contract + frontend после него |
| FE-10 | 🟡 P2 | BACKEND BLOCKED | Безопасное хранение refresh-сессии с HttpOnly cookie не поддерживается текущим auth API | backend auth contract + frontend после него |

P0-проблем, требующих немедленной остановки frontend, в статическом аудите не обнаружено. FE-4/FE-6 закрыты на шаге timeout-hardening, FE-5 — на шаге draft recovery, FE-7 — на шаге workspace route isolation, FE-8 — на шаге stale-context hardening; остаточная неопределённость неидемпотентного submit зафиксирована как backend-blocked.

---

# Закрытые frontend-проблемы

## FE-8. Route/context-driven loaders защищены от stale response — DONE

Реализовано на frontend без изменения backend:

- `SubjectLecturesView`, `LectureDetailsView` и `SubjectDetailsView` используют `createLatestRequestGuard()` и коммитят state/error/loading только для последнего актуального запроса;
- route ID захватывается до `await`, поэтому запрос не перечитывает уже изменившийся `route.params`;
- `SubjectDetailsView` дополнительно перезагружает данные при смене effective `isStudentMode`, потому что shared route использует разные API-flow для student и teacher/admin;
- `FacultySubjectsView` защищает загрузку назначенных предметов от старого ответа после быстрой смены факультета;
- base loading и faculty-context loading разделены, поэтому завершение одного запроса больше не сбрасывает индикатор второго;
- add/remove предметов факультета используют `targetFacultyId`, захваченный до batch-operation, и не перечитывают изменяемый `facultyId` внутри длительной mutation;
- выбор факультета блокируется на время batch mutation как дополнительный технический interlock;
- `TeacherWorkloadView` загружает период во временные структуры (`assignments`, группы, lecture assignments, lecture catalogs) и атомарно коммитит их только если request token всё ещё актуален;
- период (`studyCourse`, `semester`, `academicYear`) и membership snapshot захватываются до сетевых запросов;
- payload массового назначения лекций и status-update захватывается до `await`, чтобы длительная операция не читала изменившийся reactive context;
- повторный аудит `views` с `watch + async loader` не выявил оставшихся экранов без latest-request guard.

Добавлен regression/contract test `StaleContextHardening.spec.js`.

## FE-7. Workspace mode теперь ограничивает role-specific маршруты — DONE

Реализовано на frontend без изменения backend:

- существующая `meta.roles` сохранена как проверка того, какие роли вообще есть у аккаунта;
- для role-specific маршрутов добавлена отдельная `meta.workspaceRoles`;
- `authGuard` теперь требует, чтобы текущий `authStore.workspaceRole` входил в разрешённые workspace-роли маршрута;
- `/teacher/**` доступен только в `TEACHER`/`ADMIN` mode в рамках уже существующего account-role контракта;
- `/admin/**` доступен только в `ADMIN` mode;
- student-only `/public/learning/**` flow и `/tests/:testId` доступны только в `STUDENT` mode;
- shared routes (`/subjects`, `/subject-details`, `/results`, `/profile`, `/`) не получили дополнительного mode-lock и продолжают выбирать API-flow внутри view;
- `LectureDetailsView` больше не использует raw account roles для доступности прохождения теста и ориентируется на effective `isStudentMode`.

Добавлены regression-тесты, которые проверяют multi-role аккаунты и запрещают переход в route другого активного workspace без переключения режима.

## FE-4. Operation-specific timeout и защита от слепого повторного submit — DONE

Реализовано на frontend:

- обычные JSON-запросы сохраняют короткий timeout;
- `submitAttempt` использует отдельный увеличенный timeout;
- upload/download/import больше не наследуют короткий JSON-timeout;
- frontend-Nginx имеет согласованные увеличенные proxy timeout;
- при timeout/network failure во время финального submit текущий экран блокирует слепую повторную отправку, потому что исход операции неизвестен.

Остаточный риск нельзя устранить только frontend-кодом: надёжная reconciliation/idempotency семантика для финального submit остаётся в `backend_recommendations.md`.

## FE-6. Timeout больших файлов — DONE

Файловые операции переведены на отдельный timeout-класс (`0` по умолчанию на Axios-уровне), а proxy idle timeouts frontend-Nginx увеличены и вынесены в runtime-конфигурацию.

## FE-5. Восстановление незавершённых ответов после reload — DONE

Реализовано на frontend без изменения backend:

- добавлено отдельное `sessionStorage`-хранилище `student-test-draft:v1`;
- draft привязан к `testId + assignmentId + attemptId`;
- перед восстановлением проверяется полный fingerprint набора вопросов: ID, тип, текст и допустимые варианты;
- при несовпадении attempt/context/question contract draft удаляется и не применяется;
- single/multiple/text ответы восстанавливаются только в пределах допустимых текущих значений;
- matching-ответы сохраняются как `right value -> prompt ordinal`, поэтому повторное перемешивание `matchingOptions` backend-ом не ломает восстановление;
- изменения ответов сохраняются синхронным watcher-ом best-effort, чтобы минимизировать потерю последнего ввода перед reload;
- после подтверждённого успешного submit draft удаляется;
- при неизвестном исходе submit draft сохраняется, потому что frontend не может достоверно определить состояние попытки без backend reconciliation API.

Добавлены regression-тесты для совместимого resume, другого `attemptId`, изменения question contract, shuffled matching options, autosave и очистки после успешного submit.


---

# Подтверждённые frontend-проблемы

На текущем этапе подтверждённых технических frontend-проблем, которые можно надёжно исправить без изменения backend-контракта, не осталось. Оставшиеся пункты FE-9/FE-10 требуют изменений backend и поэтому не имплементируются сейчас.

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

1. **FE-8** — распространить latest-request pattern на оставшиеся context-driven loaders.
2. **FE-9/FE-10** — не имплементировать до появления соответствующего backend-контракта.

---

# Regression tests, которые стоит добавить вместе с исправлениями

Для FE-4/FE-6 regression-проверки уже добавлены в `ApiContracts.spec.js` и `TestViewRace.spec.js`. Для FE-5 добавлены `TestAttemptDraft.spec.js` и `TestViewDraft.spec.js`.

Для FE-7 добавлены `WorkspaceRouteGuard.spec.js` и `LectureDetailsWorkspaceMode.spec.js`, а `StudentLearningRouterContract.spec.js` расширен проверками `workspaceRoles`.

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
