# Ключевые связи сущностей

Ниже приведена справочная, а не исчерпывающая ER-модель.

## Учётная запись

```text
User
├── Person
├── UserRole → Role → RolePermission → Permission
├── UserPermission → Permission
└── RefreshToken
```

## Академическая структура

```text
Faculty
├── Group
└── FacultySubject → Subject

Person
├── FacultyMembership → Faculty
├── GroupMembership → Group
└── SubjectMembership → Subject
```

## Нагрузка

```text
SubjectMembership (teacher)
        ↓
TeachingAssignment
├── Subject
├── Group
├── TeachingLoadType
├── CourseVersion
├── TeachingAssignmentEnrollment → student membership
└── LectureAssignment → Lecture
                     └── StudentLectureProgress
```

## Курсы и контент

```text
Subject
  ↓
CourseTemplate
  ↓
CourseVersion
  ↓
Lecture
├── LectureMaterial
├── Topic
└── LectureTestLink → Test
```

В `Lecture` также остаётся legacy-compatible `linkedTestId` наряду с `LectureTestLink`.

## Тестирование

```text
Test
├── TestQuestionSelectionRule
├── TestAssignment
│     └── TestAttempt
│           └── QuestionResponse
│                 └── SelectedOption
└── Question / Topic question bank
      └── QuestionOption
```

Подробная модель: [Technical: testing domain](../../technical/details/testing-domain.md).
