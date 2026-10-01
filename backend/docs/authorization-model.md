# Authorization model

The backend uses three separate authorization layers. They are intentionally not interchangeable.

## 1. Global account role

A global role is an account type and a default permission bundle. A role name is not a substitute for a functional permission on ordinary management endpoints.

Examples:

- `TEACHER` receives the standard teacher permission set from seed/configuration, but `ROLE_TEACHER` alone does not authorize course, lecture, test, question, people, or teaching-management APIs.
- `ADMIN` receives the standard administrator permission set, but `ROLE_ADMIN` alone does not bypass ordinary `users.write`, `roles.manage`, `academic.manage`, `courses.manage`, `teaching.manage`, `tests.manage`, or `questions.manage` gates.

The `ADMIN` role is not a general domain-scope bypass. Global academic scope is represented explicitly by the `academic.manage` permission, so two accounts with the same functional permissions receive the same ownership/membership scope regardless of their global role name. The `ADMIN` role is used directly only for deliberately strict administrator-only operations such as database backup/restore and raw administrative attempt/support operations.

## 2. Functional permission

Permissions answer whether the account may use a function at all. URL-level security is permission-first for ordinary management APIs.

Examples:

- `roles.manage` — role and permission administration only;
- `users.read` / `users.write` — user account administration;
- `people.read` / `people.write` — person records;
- `academic.manage` — faculties, groups, subjects, and membership mutations;
- `courses.manage` — course templates, versions, and lectures;
- `teaching.manage` — teacher workload resources owned by the caller;
- `tests.manage` — tests and teacher result views;
- `questions.manage` — question bank and topics.

Granting one permission must not implicitly grant unrelated administrator functions.

## 3. Domain ownership and membership

Passing a functional permission does not grant access to every object. Controllers/services additionally enforce ownership and membership scope.

Examples:

- teacher `SubjectMembership.role = TEACHER` is a domain fact and controls which subject resources the person owns;
- student `GroupMembership.role = STUDENT` is a domain fact and controls the student's learning context;
- test/course/lecture/question ownership checks restrict a permission holder to objects they own or are assigned to;
- a custom account role may receive `teaching.manage`, but it still must own the relevant teacher subject membership.

This separation allows custom permission bundles without treating a global role name as an alternative authorization path.


## 4. Frontend capability model

The frontend mirrors the same functional model for routing and navigation, but it is never the security boundary.

- ordinary teacher/admin workspaces declare `meta.permissions` and are shown only when the account has the corresponding functional permission;
- a custom account role with a granted permission can therefore reach the same UI capability as the backend contract allows;
- removing a permission hides the corresponding ordinary management navigation even when the account still has a broad global role name;
- deliberately strict administrator-only operations (currently database backup/restore) may still require the `ADMIN` role explicitly;
- backend `SecurityConfig` and service ownership/membership checks remain authoritative if a client bypasses the UI.
