# Идентификаторы и таблицы БД

Физические имена таблиц legacy-style и quoted. Нельзя автоматически выводить имя таблицы из имени Java-класса.

| Java Entity | Таблица | ID |
|---|---|---|
| `CourseTemplate` | `CourseTemplates` | Integer |
| `CourseVersion` | `CourseVersions` | Integer |
| `Faculty` | `Faculties` | Integer |
| `FacultyMembership` | `FacultyMemberships` | Integer |
| `Group` | `Groups` | Integer |
| `GroupMembership` | `GroupMemberships` | Integer |
| `Lecture` | `CourseLectures` | Integer |
| `LectureAssignment` | `LectureAssignments` | Integer |
| `LectureMaterial` | `LectureMaterials` | Integer |
| `LectureTestLink` | `LectureTestLinks` | Integer |
| `StudentLectureProgress` | `StudentLectureProgress` | Integer |
| `Person` | `Person` | Integer |
| `Question` | `TestQuestions` | Long |
| `QuestionOption` | `QuestionOptions` | Long |
| `QuestionResponse` | `QuestionResponses` | Long |
| `SelectedOption` | `SelectedOptions` | Long |
| `Permission` | `Permissions` | Integer |
| `Role` | `Roles` | Integer |
| `RolePermission` | `RolePermissions` | EmbeddedId |
| `UserPermission` | `UserPermissions` | EmbeddedId |
| `UserRole` | `UserRoles` | EmbeddedId |
| `FacultySubject` | `FacultySubjects` | Integer |
| `Subject` | `Subjects` | Integer |
| `SubjectMembership` | `SubjectMemberships` | Integer |
| `SubjectMembershipLoadType` | `SubjectMembershipLoadTypes` | Integer |
| `TeachingAssignment` | `TeachingAssignments` | Integer |
| `TeachingAssignmentEnrollment` | `TeachingAssignmentEnrollments` | Integer |
| `TeachingLoadType` | `TeachingLoadTypes` | Integer |
| `Test` | `Tests` | Integer |
| `TestAssignment` | `TestAssignments` | Integer |
| `TestAttempt` | `TestAttempts` | Integer |
| `TestQuestionSelectionRule` | `TestQuestionSelectionRules` | Long |
| `Topic` | `LectureTopics` | Integer |
| `RefreshToken` | `RefreshTokens` | Integer |
| `User` | `Users` | Integer |

Особенно важно: `Person` хранится в таблице `Person`, а не `People`.
