# Неоднозначности и оговорки справочника

Этот файл перечисляет значения, которые нельзя безопасно воспринимать как единый глобальный enum.

## Membership `role`

Backend использует числовое поле `role` в разных membership-контекстах, однако его значение не совпадает с системными `STUDENT/TEACHER/ADMIN`.

В seed-коде одновременно существуют:

```text
MEMBERSHIP_ROLE_TEACHER = 1
MEMBERSHIP_ROLE_STUDENT = 1
```

То есть `role=1` в `SubjectMembership` и `GroupMembership` имеет контекстно разный смысл. Не создавайте глобальный справочник `1 = teacher` или `1 = student`.

## Статусы разных сущностей

`status=1` также не глобален:

```text
TestAssignment     1 = Черновик
TeachingAssignment 1 = Активно
```

Всегда используйте таблицу статусов конкретной сущности.

## Неполностью формализованные статусы

Для следующих полей код задаёт диапазон, но не хранит единый канонический набор подписей:

- `TeachingAssignmentEnrollment.status` (`1..4`);
- `LectureAssignment.status` (`1..4`);
- `StudentLectureProgress.status=3`;
- `StudentLectureProgress.completionSource` (`1..3`).

Документация намеренно не придумывает значения, которых нет в текущем контракте.

## Frontend error codes

Frontend знает несколько более предметных error codes, которых backend сейчас не гарантирует. Используйте [api-error-codes.md](./api-error-codes.md) и различайте реальные backend codes и frontend-reserved values.

## `public/learning`

Путь содержит `public`, но требует authentication. Это student-facing API, а не anonymous API.

## `Result`

В интерфейсе и API есть результаты, однако отдельной JPA Entity `Result` нет.

## `CourseLectureId`

Это ID сущности `Lecture`, физически хранящейся в `CourseLectures`; отдельного класса `CourseLecture` нет.

## `Lecture.linkedTestId`

Это legacy/single-test compatibility field. Актуальная множественная связь лекции с тестами представлена `LectureTestLink`.

## Схема БД

Текущая схема развивается через сочетание JPA/Hibernate и `schema.sql`, а не через версионированные migrations. Поэтому справочник таблиц описывает текущий snapshot, а не стабильную migration contract history.
