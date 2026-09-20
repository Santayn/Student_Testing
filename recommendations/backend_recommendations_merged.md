# Актуальные рекомендации и аудит backend

Дата проверки: **20.09.2026**.

Файл актуализирован по фактическому состоянию backend после последнего объединения. Старые пункты не считаются выполненными только потому, что frontend содержит workaround. При проверке учитывались `SecurityConfig`, сервисы, REST-контроллеры, репозитории, `schema.sql`, Docker-конфигурация и текущие DTO.

## Что было очищено при актуализации

Полностью выполненных рекомендаций из предыдущей версии не обнаружено, поэтому целиком удалённых пунктов нет. Однако несколько пунктов были **частично реализованы**, и уже выполненные части из формулировок убраны:

- для student API уже существует отдельный `POST /api/v1/public/learning/test-assignments/{assignmentId}/attempts/start`;
- student API уже возвращает `attemptsRemaining` и `canResume`;
- `TestAttempt` уже хранит итоговый `score`, а `PublicSubmitResponse` его возвращает;
- правильный текст ответа в student submit-response уже не отдаётся (`correctAnswer = null`), но бинарное поле `correct` всё ещё раскрывается;
- student UI использует `/api/v1/public/learning/**`, но общий backend `GET /subjects/{id}` всё ещё не ограничен по объектному доступу.

То есть эти рекомендации ниже описывают только **оставшуюся часть проблемы**.

## Сводка ревизии после merge

| № рекомендации | Что уже реализовано | Что остаётся исправить |
|---|---|---|
| 2 | student question DTO не раскрывает правильные варианты; `correctAnswer` при submit передаётся как `null` | всё ещё раскрывается `correct=true/false`; student results также содержат детальную правильность |
| 3 | student workflow использует отдельный `/api/v1/public/learning/subjects/{id}` | общий `GET /api/v1/subjects/{id}` всё ещё не имеет object-level ограничения для произвольного authenticated пользователя |
| 4 | `/auth/me` / `/users/me` возвращают `personId`, roles и permissions | нет централизованного `accountState` и backend-флага состояния регистрации для UI |
| 5 | есть `POST .../attempts/start`, `attemptsRemaining`, `canResume` | start фактически совмещён с resume; нет отдельного `GET .../attempts/current`; resume использует `testId`, а не assignment scope |
| 13 | `TestAttempt.score` уже вычисляется и submit возвращает score | Result DTO и статистика всё ещё ориентируются на `correct/total`; отсутствуют `maxScore` и `scorePercent` |

Полностью закрытых рекомендаций после merge не обнаружено. Пункты выше оставлены в реестре только в объёме незавершённой работы.

---

# Текущий аудит backend

## 🔴 Критические проблемы

### A1. Матрица прав TEACHER даёт административные полномочия

В `DataLoader` роли `TEACHER` выдаются, среди прочего:

```text
people.write
academic.manage
courses.manage
teaching.manage
tests.manage
questions.manage
```

При этом `SecurityConfig` допускает изменение людей через `people.write`, а изменение факультетов, групп и предметов — через `academic.manage`.

Следствие: стандартный TEACHER может пройти URL-level проверку на операции, которые по смыслу должны быть административными.

**Что исправить:**

- убрать `people.write` и `academic.manage` из стандартного набора TEACHER, если это не является явным бизнес-требованием;
- проверить остальные permission-наборы роли;
- добавить security regression-тесты на TEACHER для изменения `Person`, `Faculty`, `Group`, `Subject`.

**Приоритет: P0.**

### A2. Авторизация одновременно завязана и на роли, и на permissions

Сейчас Spring Security получает в одном наборе `GrantedAuthority` и:

```text
ROLE_TEACHER
TEACHER
```

и:

```text
tests.manage
TESTS.MANAGE
```

Далее `SecurityConfig` для ряда функций разрешает **любой** из этих authorities. Например, доступ к тестам разрешается по `TEACHER` **или** `tests.manage`.

Следствие: удалить у пользователя permission `tests.manage` недостаточно — наличие роли `TEACHER` всё равно может открыть endpoint.

**Рекомендуемая модель:**

```text
permission -> можно использовать функцию
ownership/membership -> можно работать с конкретным объектом
role -> шаблон/набор permissions, но не самостоятельный обход permission
```

Для mutating endpoints желательно перейти на permission-only проверки, оставив роли для формирования стандартных наборов прав и бизнес-контекста.

**Приоритет: P0 / архитектурный.**

### A3. Docker по умолчанию запускает локальный seed-режим

В `docker-compose.yml` по умолчанию используются:

```text
SPRING_PROFILES_ACTIVE=local
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_SQL_INIT_MODE=always
APP_DATA_LOADER_ENABLED=true
```

`DataLoader` создаёт известные тестовые аккаунты, включая:

```text
admin / admin123
teacher / teacher1
student / student1
```

Такую конфигурацию нельзя использовать как production/default deployment.

**Что исправить:**

- production compose/profile должен по умолчанию использовать `validate`, `SQL_INIT_MODE=never`, `DATA_LOADER=false`;
- local/demo профиль должен включаться явно;
- seed-учётные записи не должны появляться в production БД.

**Приоритет: P0 для внешнего deployment.**

---

## 🟠 Высокий приоритет

### A4. Duration теста не контролируется backend

`Test.duration` возвращается клиенту, однако `submitResponse()` и `completeAttempt()` проверяют только состояние попытки. Сервер не сравнивает текущее время с `startedAt + duration`.

Следствие: клиентский таймер можно обойти прямыми API-запросами и продолжать сдавать тест после истечения времени.

**Что исправить:** централизованная серверная проверка deadline при submit/complete и, желательно, автоматический перевод просроченной попытки в завершённое/expired состояние.

**Приоритет: P1.**

### A5. `availableUntilUtc` не проверяется при submit/complete

Доступность `TestAssignment` проверяется при старте попытки, но после старта `submitResponse()` и `completeAttempt()` не вызывают повторную проверку assignment deadline.

Следствие: попытку можно продолжить и отправить после закрытия назначения.

Нужно явно определить бизнес-правило:

- либо дедлайн запрещает любые дальнейшие ответы;
- либо уже начатую попытку разрешено закончить в пределах `duration`;
- либо использовать `min(availableUntilUtc, startedAt + duration)`.

Это правило должно контролироваться backend.

**Приоритет: P1.**

### A6. Student API раскрывает `correct=true/false` по каждому вопросу

Правильный текст ответа студенту уже скрыт, однако `PublicSubmitDetailResponse` всё ещё содержит:

```text
boolean correct
```

`ResultRestController` в student-mode также возвращает `correct`, `questionPoints`, `awardedPoints` и `gradingStatus` для каждого ответа.

При нескольких попытках такая обратная связь позволяет подбирать правильные варианты.

**Что исправить:** разделить teacher/admin и student DTO и выдавать детализацию правильности только когда это разрешено бизнес-правилом (например, после исчерпания попыток/закрытия периода теста).

**Приоритет: P1.**

### A7. Попытки смешиваются между разными TestAssignment одного Test

База ограничивает незавершённую попытку по:

```text
TestAssignmentId + PersonId
```

но `TestService` для `attemptsRemaining`, `hasInProgressAttempt`, resume и лимита использует выборки по:

```text
TestId + PersonId
```

Из-за этого assignment B того же теста может:

- увидеть попытки assignment A;
- уменьшить `attemptsRemaining` из-за попыток другого назначения;
- возобновить незавершённую попытку другого assignment;
- вернуть `actualAssignmentId`, отличный от запрошенного.

**Что исправить:** определить scope попыток. Если лимит относится к назначению, все операции должны использовать `testAssignmentId + personId`. Если лимит глобален для Test, тогда БД, API и документация должны отражать именно глобальную модель и не позволять cross-assignment resume без явного правила.

**Приоритет: P1.**

### A8. Race condition при rotation refresh-token

`refresh()` сначала читает token обычным `findByTokenHash`, затем проверяет `revokedAtUtc`, генерирует новую пару и только после этого помечает старый token отозванным в транзакции.

Два параллельных запроса с одним refresh-token потенциально могут оба увидеть его ещё действующим и выпустить две новые token-пары.

**Что исправить:** pessimistic row lock (`SELECT ... FOR UPDATE`) либо атомарный conditional update/версионирование, после которого только один refresh может успешно ротировать токен.

**Приоритет: P1.**

---

## 🟡 Средний приоритет

### A9. Нет backend-защиты login от brute force

В коде не обнаружено rate limit, backoff или временной блокировки после серии неверных паролей. Минимальная длина пароля — 6 символов.

**Что исправить:** rate limiting на login/refresh, аудит неудачных входов и при необходимости временный lockout/backoff.

### A10. Смена пароля не инвалидирует уже выданные access-token

`changePassword()` отзывает активные refresh-token, но JWT access-token остаются валидными до собственного `exp` (по текущим настройкам 15 либо 120 минут).

**Что исправить:** если требуется немедленная инвалидизация сессий, добавить security/session version в JWT или denylist/revocation strategy.

### A11. Results API использует `findAll()` и фильтрацию в Java

`ResultRestController` перебирает `testAttemptRepository.findAll()` и после этого фильтрует попытки в приложении. Для ответов выполняются дополнительные запросы, что создаёт плохой профиль масштабирования и потенциальный N+1.

**Что исправить:** repository query по конкретному student/teacher scope, pagination и projection/DTO query.

### A12. Файловое хранилище и БД не образуют атомарную операцию

`LectureMaterialService.upload()` сначала пишет файл через `Files.copy`, затем сохраняет метаданные в БД. При rollback БД физический файл может остаться на диске.

При delete порядок обратный: БД-запись удаляется перед удалением файла.

**Что исправить:** компенсирующая очистка при исключении, staging/temp-файлы и after-commit обработка либо отдельный transactional storage workflow.

### A13. Swagger/OpenAPI доступен без авторизации

`/swagger-ui/**` и `/v3/api-docs/**` явно `permitAll()`.

Для local/dev это допустимо, но для production стоит ограничить профилем или авторизацией.

---

# Открытые backend-рекомендации

Статусы:

- **OPEN** — backend-реализация отсутствует;
- **PARTIAL** — часть рекомендации уже реализована, ниже оставлен только незакрытый остаток.

## 1. Атомарное создание теста вместе с назначениями — OPEN

Frontend по-прежнему вынужден оркестрировать создание Test и нескольких TestAssignment отдельными запросами. Единой backend-транзакции/command endpoint для этого не найдено.

Нужно создать бизнес-операцию вида:

```text
POST /tests/with-assignments
```

или эквивалентный command DTO, выполняемый в одной `@Transactional` операции: либо Test и все assignments созданы, либо не создано ничего.

**Приоритет: высокий.**

---

## 2. Не раскрывать студенту правильность каждого ответа — PARTIAL

Уже выполнено:

- student DTO вопроса не содержит правильные варианты;
- `correctAnswer` в submit detail фактически передаётся как `null`.

Осталось:

- убрать/условно скрывать `correct` по каждому вопросу;
- привести к тому же правилу `/api/v1/results/student/**`;
- не раскрывать grading details раньше разрешённого бизнес-момента.

**Связано с аудитом A6. Приоритет: высокий.**

---

## 3. Закрыть общий `GET /subjects/{id}` от произвольного authenticated STUDENT — PARTIAL

Уже выполнено: student workflow использует `/api/v1/public/learning/subjects/{id}` и там проверяется учебный контекст.

Не выполнено: общий `GET /api/v1/subjects/{id}` попадает под `.anyRequest().authenticated()` и сам `SubjectRestController` не выполняет object-level проверку.

Нужно либо оставить общий endpoint только ADMIN/TEACHER, либо добавить проверку видимости предмета текущему пользователю.

**Приоритет: высокий.**

---

## 4. Централизовать состояние готовности аккаунта — PARTIAL

`/auth/me` и `/users/me` уже возвращают `personId`, roles и permissions.

Осталось централизовать:

- `accountState` (`PENDING_PERSON`, `PENDING_ROLE`, `ACTIVE`, `DISABLED` или аналог);
- фактический backend-флаг `publicRegistrationEnabled`, если frontend должен отображать регистрацию;
- единое правило готовности аккаунта вместо повторной логики на клиенте.

**Приоритет: средний.**

---

## 5. Разделить старт и возобновление попытки — PARTIAL

Уже выполнено:

- есть отдельный `POST /public/learning/test-assignments/{assignmentId}/attempts/start`;
- test DTO содержит `attemptsRemaining` и `canResume`.

Осталось:

- `POST start` внутри вызывает `startOrResumeAttemptWithRandomQuestions()` и не отличает новый старт от resume;
- отдельного `GET .../attempts/current` нет;
- resume сейчас ошибочно ищется по `testId + personId`, что создаёт cross-assignment проблему A7.

Рекомендуемый контракт:

```text
GET  /test-assignments/{id}/attempts/current
POST /test-assignments/{id}/attempts/start
```

`POST start` должен либо явно создавать новую попытку, либо возвращать конфликт/состояние, если есть незавершённая; resume должен быть отдельной явной операцией чтения.

**Приоритет: высокий с учётом A7.**

---

## 6. Агрегированные role-aware endpoints для сложных teacher/admin экранов — OPEN

Frontend всё ещё собирает учебный контекст из SubjectMembership, TeachingAssignment, Subject, Group, Lecture и других сущностей отдельными запросами.

Можно добавить специализированные read-model DTO для teacher/admin workspace. Это уменьшит количество запросов и риск смешивания `subjectMembershipId` разных преподавателей.

**Приоритет: низкий / архитектурный.**

---

## 7. Явное делегированное авторство CourseTemplate для ADMIN — OPEN / зависит от требований

`POST /courses/templates` по-прежнему назначает автором `currentPersonId(authentication)`. Request DTO не содержит `ownerPersonId`.

Если ADMIN должен создавать шаблон **от имени преподавателя**, это должно быть отдельным backend-сценарием с проверкой активного teacher SubjectMembership и аудитом actor/owner.

Если такого бизнес-требования нет, текущую безопасную модель менять не нужно, и этот пункт можно будет удалить после фиксации решения в требованиях.

**Приоритет: средний / бизнес-зависимый.**

---

## 8. Проверять активность SubjectMembership при создании TeachingAssignment — OPEN

`TeachingService.createAssignment()` получает SubjectMembership по ID, но не требует одновременно:

```text
role == TEACHER
status == 1
removedAtUtc == null
```

Frontend-фильтрация не заменяет backend-проверку.

**Приоритет: высокий.**

---

## 9. Единый helper активного teacher SubjectMembership для mutation-операций — OPEN

`CurrentUserAccessService.requireSubjectMembershipOwner()` проверяет роль membership и владельца, но не `status`/`removedAtUtc`.

Нужен отдельный helper для **новых/изменяющих** операций, например:

```java
requireActiveTeacherSubjectMembership(...)
```

Read-only/history операции при этом должны уметь отображать закрытые исторические membership.

**Приоритет: высокий.**

---

## 10. Идемпотентное повторное назначение преподавателя на предмет — OPEN

`MembershipService.addSubjectMember()` сейчас возвращает conflict, если существует любая не удалённая запись для той же пары subject/person/role, в том числе приостановленная.

Backend-операция «назначить преподавателя» должна:

1. вернуть уже активную запись;
2. реактивировать status=2, если запись не удалена;
3. создать новую только после настоящего удаления;
4. сохранить DB-защиту от параллельных дублей.

**Приоритет: средний.**

---

## 11. Атомарные batch-операции для массовых административных изменений — OPEN

Специализированных batch endpoints для массового назначения/снятия предметов и связей факультета не обнаружено.

Если бизнес-операция должна быть all-or-nothing, backend должен валидировать весь набор и выполнять изменения одной транзакцией.

**Приоритет: средний.**

---

## 12. Сделать student `GET /lectures/{lectureId}/tests` строго read-only — OPEN

Проблема подтверждена текущим кодом: GET помечен обычным `@Transactional`, и при отсутствии assignment вызывает:

```java
lectureTestLinkService.ensureLectureTestAvailability(...)
```

То есть чтение может создавать/изменять состояние БД.

Нужно:

- сделать GET `@Transactional(readOnly = true)`;
- удалить mutation из read path;
- создание TestAssignment выполнять только явной teacher/admin операцией;
- закрепить regression-тестом отсутствие DB changes после GET.

**Приоритет: высокий (ранее был помечен критическим).**

---

## 13. Использовать серверный score в Result DTO и статистике — PARTIAL

Уже выполнено:

- `TestAttempt` имеет `score`;
- `completeAttempt()` вычисляет `score`;
- `PublicSubmitResponse` возвращает `score`.

Осталось:

- `ResultRestController` не использует `TestAttempt.score` в `ResultAttemptResponse`;
- статистика всё ещё строится по `correct/total`;
- partial grading с `awardedPoints` делает такую статистику неточной.

Нужно отдавать минимум:

```json
{
  "score": 7.5,
  "maxScore": 10,
  "scorePercent": 75.0
}
```

и использовать эти значения как источник истины для результата/лучшей попытки.

**Приоритет: высокий.**

---

## 14. Серверный effective workspace mode для multi-role аккаунтов — OPEN

Backend по-прежнему принимает решения по полному набору ролей пользователя. Явного проверяемого effective mode (`STUDENT`/`TEACHER`/`ADMIN`) не обнаружено.

Если продукт действительно поддерживает переключение рабочего режима одного multi-role аккаунта, backend должен валидировать выбранный режим и использовать его для scope выборок. Это не должно быть способом повысить реальные privileges.

**Приоритет: средний / архитектурный.**

---

## 15. Асинхронный workflow LLM-проверки текстовых ответов — OPEN

В backend присутствует `LocalLlmTextAnswerEvaluationService`, а оценка текстового ответа вызывается синхронно внутри `submitResponse()`.

Для массового использования рекомендуется вынести inference из HTTP transaction path:

```text
Backend transaction
  -> answer + grading=PENDING + outbox
  -> Kafka grading.requested
  -> AI Grading Service / Ollama
  -> Kafka grading.completed
  -> idempotent backend consumer
  -> grading result + score update
```

Ошибка/timeout AI не должна автоматически означать неправильный ответ. Нужны retry/DLQ и состояния `PENDING/FAILED/GRADED` либо эквивалент.

**Приоритет: высокий / архитектурный при массовом использовании.**

---

# Рекомендуемый порядок исправления

1. **A1/A2** — исправить матрицу TEACHER и выбрать единый принцип permission-based endpoint security.
2. **A3** — отделить local/demo Docker от безопасного deployment.
3. **A4/A5** — серверный deadline теста и assignment.
4. **A6 / рекомендация 2** — убрать преждевременную правильность из student DTO.
5. **A7 / рекомендация 5** — исправить scope attempts и resume.
6. **A8** — атомарная rotation refresh-token.
7. **Рекомендации 8–10** — активность и lifecycle SubjectMembership.
8. **Рекомендация 12** — убрать mutation из student GET.
9. **Рекомендация 13** — score-based results.
10. Затем A9–A13 и архитектурные пункты 1, 6, 11, 14, 15.

После каждого security-исправления нужен regression-тест, который проверяет не только ожидаемый `200`, но и запрещённые сценарии `401/403/409` и отсутствие побочных изменений БД.
