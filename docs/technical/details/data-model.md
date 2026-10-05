# Модель данных

## Общий принцип

Модель данных Student Testing разделяет четыре крупных области:

1. учётные записи и безопасность;
2. академическую структуру;
3. учебный контент и нагрузку;
4. тестирование и результаты.

В backend модели представлены JPA-сущностями в `org.santayn.testing.models`.

## Учётная запись и человек

Ключевое разделение:

```mermaid
flowchart LR
    U[User] -->|optional PersonId| P[Person]
    U --> UR[UserRole]
    UR --> R[Role]
    R --> RP[RolePermission]
    RP --> PERM[Permission]
    U --> UP[UserPermission]
    UP --> PERM
    U --> RT[RefreshToken]
```

`User` отвечает за authentication/security:

- login;
- password hash;
- active state;
- roles;
- direct permissions;
- optional `PersonId`.

`Person` отвечает за сведения о человеке в академической модели:

- имя и фамилия;
- дата рождения;
- email;
- телефон.

Это позволяет существовать учётной записи, которая ещё не привязана к академическому профилю.

## Академическая структура

```mermaid
flowchart TD
    F[Faculty] --> FS[FacultySubject]
    FS --> S[Subject]
    F --> G[Group]
    G --> GM[GroupMembership]
    GM --> P[Person]
    S --> SM[SubjectMembership]
    SM --> P
```

`GroupMembership` и `SubjectMembership` являются самостоятельными сущностями, а не только join-таблицами. Они содержат статус, время назначения/удаления и примечания.

## Учебная нагрузка

```mermaid
flowchart TD
    SM[SubjectMembership] --> TA[TeachingAssignment]
    G[Group] --> TA
    LT[TeachingLoadType] --> TA
    CV[CourseVersion] --> TA
    TA --> TAE[TeachingAssignmentEnrollment]
    GM[GroupMembership] --> TAE
```

`TeachingAssignment` описывает назначение преподавателя/предмета на группу в конкретном учебном периоде с типом нагрузки и часами.

`TeachingAssignmentEnrollment` связывает конкретный membership студента с учебным назначением.

## Курсы и лекции

```mermaid
flowchart TD
    S[Subject] --> CT[CourseTemplate]
    CT --> CV[CourseVersion]
    CV --> L[Lecture]
    S --> L
    SM[SubjectMembership] --> L
    L --> LM[LectureMaterial]
    L --> LTL[LectureTestLink]
    LTL --> T[Test]
    S --> TP[Topic]
```

`CourseTemplate` задаёт логический шаблон курса. `CourseVersion` хранит версию с номером, описанием изменений и состоянием публикации. Лекции могут относиться к конкретной версии.

## Вопросы, тесты и результаты

```mermaid
flowchart TD
    TP[Topic] --> Q[Question]
    L[Lecture] --> Q
    T[Test] --> QR[TestQuestionSelectionRule]
    TP --> QR
    L --> QR
    T --> A[TestAssignment]
    A --> AT[TestAttempt]
    AT --> RESP[QuestionResponse]
    Q --> RESP
    RESP --> SO[SelectedOption]
    Q --> OPT[QuestionOption]
    OPT --> SO
```

### `Test`

Хранит описание теста, количество вопросов, число разрешённых попыток, продолжительность и автора.

### `TestQuestionSelectionRule`

Определяет, из какого контекста и в каком количестве выбирать вопросы, включая количество вопросов каждого типа.

### `TestAssignment`

Связывает тест с учебным контекстом и временным окном доступности. Может быть связан с версией курса, лекцией и/или `TeachingAssignment`.

### `TestAttempt`

Конкретная попытка Person по TestAssignment. Содержит ordinal, статус, время начала/завершения и итоговый score.

### `QuestionResponse`

Ответ на конкретный вопрос внутри попытки. В нём сохраняются текст ответа, корректность и начисленные баллы.

### `SelectedOption`

Фиксирует выбранные варианты ответа для option-based вопросов.

## Важная семантика модели

- `Test` не равен `TestAssignment`: тест описывает содержание и правила, assignment — кому и когда он доступен.
- `TestAttempt` не равен результату как отдельной сущности: итоговый результат собирается из попытки и `QuestionResponse`.
- `User` не равен `Person`.
- Membership-сущности сохраняют контекст назначения и позволяют моделировать жизненный цикл связи.
- Бинарное содержимое материалов лекций не хранится в БД; в БД находятся метаданные и путь.
