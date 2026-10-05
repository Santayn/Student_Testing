# Каталог сущностей

В проекте 35 JPA Entity.

## Authentication и security

- `User` — учётная запись.
- `Person` — персональные данные человека.
- `Role` — системная роль.
- `Permission` — право.
- `UserRole` — связь User ↔ Role.
- `UserPermission` — прямое право User.
- `RolePermission` — permission роли.
- `RefreshToken` — refresh-session state.

## Академическая структура

- `Faculty`
- `Group`
- `Subject`
- `FacultySubject`
- `FacultyMembership`
- `GroupMembership`
- `SubjectMembership`

## Учебная нагрузка

- `TeachingLoadType`
- `SubjectMembershipLoadType`
- `TeachingAssignment`
- `TeachingAssignmentEnrollment`
- `LectureAssignment`
- `StudentLectureProgress`

## Курсы и лекции

- `CourseTemplate`
- `CourseVersion`
- `Lecture`
- `LectureMaterial`
- `LectureTestLink`
- `Topic`

## Тестирование

- `Test`
- `TestAssignment`
- `TestAttempt`
- `TestQuestionSelectionRule`
- `Question`
- `QuestionOption`
- `QuestionResponse`
- `SelectedOption`

`Result` не является отдельной Entity; результаты вычисляются из тестового домена и учебного контекста.
