# Migration To TestPlatformBackend

## Goal

`TestPlatformBackend` is the source of truth for the current database model and the target application behavior. The Spring Boot code under `src` still uses the older model, so migration must be done in vertical slices instead of direct table renames.

## Current State

The Spring Boot application is centered around:

- `User` with embedded profile fields and a single `Role`
- separate `Student` and `Teacher` entities
- simple join tables such as `group_student`, `teacher_group`, `teacher_subject`, `test_group`, `test_lecture`
- old test result storage in `student_answer`

The new .NET model is centered around:

- `Person` as the physical person profile
- `Users`, `Roles`, `Permissions`, `UserRoles`, `RolePermissions`, `UserPermissions`, `RefreshTokens`
- membership tables with history and statuses: `FacultyMemberships`, `GroupMemberships`, `SubjectMemberships`
- teaching workload: `TeachingLoadTypes`, `SubjectMembershipLoadTypes`, `TeachingAssignments`, `TeachingAssignmentEnrollments`
- course content: `CourseTemplates`, `CourseVersions`, `CourseLectures`
- assigned tests and attempts: `Tests`, `TestQuestions`, `QuestionOptions`, `TestAssignments`, `TestAttempts`, `QuestionResponses`, `SelectedOptions`
- assigned lectures and progress: `LectureAssignments`, `StudentLectureProgress`

## Important Compatibility Notes

EF Core migrations create PostgreSQL identifiers such as `"Users"`, `"PasswordHash"`, `"PersonId"`. Hibernate must either quote table and column names explicitly or enable globally quoted identifiers for entities that target the new schema.

The old Spring Boot `ddl-auto: update` setting is unsafe for migration work. When the Java code is pointed at the new database, old JPA entities can silently create legacy tables again. Before validating against the new schema, switch to `ddl-auto: validate` or isolate migrated entities behind a separate persistence configuration.

The Java build currently passes with JDK 21. It fails on JDK 25 with the current Lombok version. Use JDK 21 for migration work unless Lombok and compiler settings are updated.

## Migration Slices

### 1. Auth And RBAC

Target tables:

- `Person`
- `Users`
- `Roles`
- `Permissions`
- `UserRoles`
- `RolePermissions`
- `UserPermissions`
- `RefreshTokens`

Required Java changes:

- replace `app_user` mapping with `Users`
- replace `app_role` mapping with `Roles`
- replace one-user-one-role logic with many-to-many user roles
- move `firstName`, `lastName`, `phoneNumber` from `User` to `Person`
- add `isActive`, `personId`, `passwordHash`
- align JWT claims with .NET: subject/user id, login/name, roles, permissions, person id
- add refresh token flow: `login`, `register`, `refresh`, `revoke`, `change-password`, `me`
- keep temporary compatibility methods only while old controllers still compile

### 2. Academic Structure

Replace:

- `Student` and `Teacher` identity usage with `Person`
- `Group_Student` with `GroupMemberships`
- `Teacher_Group` with `TeachingAssignments` and `TeachingAssignmentEnrollments`
- `Teacher_Subject` with `SubjectMemberships` and `SubjectMembershipLoadTypes`
- `Subject_Faculty` with the new subject/faculty relationship model available through memberships and assignments

This slice should update `StudentService`, `TeacherService`, `GroupService`, `SubjectService`, and all DTOs that expose student/teacher ids.

### 3. Course And Lecture Content

Replace:

- `Lecture` with `CourseLecture`
- direct subject-to-lecture access with `CourseTemplate -> CourseVersion -> CourseLecture`
- lecture visibility and assignment logic with `LectureAssignments`
- progress tracking with `StudentLectureProgress`

This slice should update public subject lecture endpoints and teacher lecture management endpoints.

### 4. Tests And Results

Replace:

- `Test_Group` and `Test_Lecture` with `TestAssignments`
- `Question`, `Question_In_Test`, `Question_Test_Answer` with `TestQuestions`, `QuestionOptions`, `QuestionResponses`, `SelectedOptions`
- `AnswerResult` / `student_answer` with `TestAttempts` and response tables

This slice should update test creation, loading, submission, and teacher results endpoints.

## First Implementation Step

Start with auth/RBAC because it is the entry point for all other flows and has the smallest external surface in `TestPlatformBackend`.

Recommended first code change:

1. Add JPA mappings for `Person`, `Users`, `Roles`, `Permissions`, `RefreshTokens`.
2. Update `UserRegisterService`, `JwtService`, `AuthRestController`, `UserRoleService`.
3. Keep `/api/v1/auth/*` routes temporarily, but add `/api/auth/*` aliases to match .NET.
4. Keep read-only compatibility methods on `User` for old DTOs while the rest of the app is migrated.
5. Run with JDK 21 and `mvnw test`.

## Definition Of Done For Full Migration

- Java app starts against the new database with `ddl-auto: validate`.
- No legacy tables are created by Hibernate.
- Static frontend calls the migrated API contract.
- Auth supports access and refresh tokens.
- Students and teachers are represented through `Person` plus memberships.
- Tests store attempts and answers in the new normalized tables.
- Build and tests pass on a documented JDK version.
