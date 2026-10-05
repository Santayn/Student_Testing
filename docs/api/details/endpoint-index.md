# Полный индекс endpoint'ов

Таблица построена по фактическим Spring controller mappings текущей версии проекта. Колонка **Frontend** показывает прямое наличие соответствующего маршрута в `frontend/src/api/*.js`; отсутствие frontend-модуля не означает, что endpoint не используется другими клиентами или не нужен backend.

| Метод | Маршрут | Назначение | Доступ | HTTP | Backend handler | Frontend |
|---|---|---|---|---:|---|---|
| `POST` | `/api/v1/admin/database-backups` | Создать и скачать SQL-резервную копию БД | ADMIN | `200` | `AdminDatabaseBackupRestController.createBackup` | `databaseBackups.api.js` |
| `POST` | `/api/v1/admin/database-backups/restore` | Восстановить БД из SQL-файла | ADMIN | `200` | `AdminDatabaseBackupRestController.restoreBackup` | `databaseBackups.api.js` |
| `POST` | `/api/v1/auth/login` | Выполнить вход и получить пару access/refresh token | Anonymous | `200` | `AuthRestController.login` | `auth.api.js` |
| `POST` | `/api/v1/auth/register` | Зарегистрировать учётную запись | Anonymous | `201` | `AuthRestController.register` | `auth.api.js` |
| `POST` | `/api/v1/auth/refresh` | Обновить пару токенов | Anonymous | `200` | `AuthRestController.refresh` | `auth.api.js` |
| `POST` | `/api/v1/auth/revoke` | Отозвать refresh token | Authenticated | `204` | `AuthRestController.revoke` | `auth.api.js` |
| `POST` | `/api/v1/auth/change-password` | Изменить пароль текущего пользователя | Authenticated | `204` | `AuthRestController.changePassword` | `auth.api.js` |
| `GET` | `/api/v1/auth/me` | Получить сведения о текущем пользователе | Authenticated | `200` | `AuthRestController.me` | `auth.api.js` |
| `GET` | `/api/v1/courses/templates` | Получить шаблоны курсов | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.templates` | `courses.api.js` |
| `GET` | `/api/v1/courses/templates/{templateId}` | Получить шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.template` | — |
| `POST` | `/api/v1/courses/templates` | Создать шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.createTemplate` | `courses.api.js` |
| `PUT` | `/api/v1/courses/templates/{templateId}` | Изменить шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.updateTemplate` | `courses.api.js` |
| `DELETE` | `/api/v1/courses/templates/{templateId}` | Удалить шаблон курса | TEACHER / ADMIN + courses.manage/ownership | `204` | `CourseRestController.deleteTemplate` | `courses.api.js` |
| `PUT` | `/api/v1/courses/versions/{versionId}/publish` | Опубликовать версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.publishVersion` | `courses.api.js` |
| `PUT` | `/api/v1/courses/versions/{versionId}/unpublish` | Снять публикацию версии курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.unpublishVersion` | `courses.api.js` |
| `GET` | `/api/v1/courses/templates/{templateId}/versions` | Получить версии шаблона | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.versions` | `courses.api.js` |
| `GET` | `/api/v1/courses/versions/{versionId}` | Получить версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.version` | — |
| `POST` | `/api/v1/courses/templates/{templateId}/versions` | Создать версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.createVersion` | `courses.api.js` |
| `PUT` | `/api/v1/courses/versions/{versionId}` | Изменить версию курса | TEACHER / ADMIN + courses.manage/ownership | `200` | `CourseRestController.updateVersion` | `courses.api.js` |
| `GET` | `/api/v1/faculties` | Получить список ресурсов | Authenticated read / ADMIN or academic.manage write | `200` | `FacultyRestController.all` | `faculties.api.js` |
| `GET` | `/api/v1/faculties/{id}` | Получить ресурс по идентификатору | Authenticated read / ADMIN or academic.manage write | `200` | `FacultyRestController.one` | `faculties.api.js` |
| `GET` | `/api/v1/faculties/{id}/subjects` | Получить связанные предметы | Authenticated read / ADMIN or academic.manage write | `200` | `FacultyRestController.subjects` | `faculties.api.js` |
| `POST` | `/api/v1/faculties` | Создать ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `FacultyRestController.create` | `faculties.api.js` |
| `PUT` | `/api/v1/faculties/{id}` | Изменить ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `FacultyRestController.update` | `faculties.api.js` |
| `DELETE` | `/api/v1/faculties/{id}` | Удалить ресурс | Authenticated read / ADMIN or academic.manage write | `204` | `FacultyRestController.delete` | `faculties.api.js` |
| `POST` | `/api/v1/faculties/{facultyId}/subjects/{subjectId}` | Связать факультет и предмет | Authenticated read / ADMIN or academic.manage write | `204` | `FacultyRestController.linkSubject` | `faculties.api.js` |
| `DELETE` | `/api/v1/faculties/{facultyId}/subjects/{subjectId}` | Удалить связь факультета и предмета | Authenticated read / ADMIN or academic.manage write | `204` | `FacultyRestController.unlinkSubject` | `faculties.api.js` |
| `GET` | `/api/v1/groups` | Получить список ресурсов | Authenticated read / ADMIN or academic.manage write | `200` | `GroupRestController.all` | `groups.api.js` |
| `GET` | `/api/v1/groups/{id}` | Получить ресурс по идентификатору | Authenticated read / ADMIN or academic.manage write | `200` | `GroupRestController.one` | `groups.api.js` |
| `POST` | `/api/v1/groups` | Создать ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `GroupRestController.create` | `groups.api.js` |
| `PUT` | `/api/v1/groups/{id}` | Изменить ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `GroupRestController.update` | `groups.api.js` |
| `DELETE` | `/api/v1/groups/{id}` | Удалить ресурс | Authenticated read / ADMIN or academic.manage write | `204` | `GroupRestController.delete` | `groups.api.js` |
| `GET` | `/api/v1/lectures/{id}/tests` | Получить тесты, связанные с лекцией | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureContentRestController.linkedTests` | `lectures.api.js` |
| `PUT` | `/api/v1/lectures/{id}/tests` | Заменить набор связанных тестов | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureContentRestController.replaceLinkedTests` | `lectures.api.js` |
| `GET` | `/api/v1/lectures/{lectureId}/materials` | Получить материалы лекции | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureContentRestController.materials` | `lectures.api.js` |
| `POST` | `/api/v1/lectures/{lectureId}/materials` | Загрузить материалы лекции | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureContentRestController.uploadMaterials` | `lectures.api.js` |
| `DELETE` | `/api/v1/lectures/{lectureId}/materials/{materialId}` | Удалить материал лекции | TEACHER / ADMIN + courses.manage/ownership | `204` | `LectureContentRestController.deleteMaterial` | `lectures.api.js` |
| `GET` | `/api/v1/lectures/{lectureId}/materials/{materialId}/download` | Скачать материал лекции | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureContentRestController.downloadMaterial` | `lectures.api.js` |
| `GET` | `/api/v1/lectures` | Получить список ресурсов | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureRestController.all` | `lectures.api.js` |
| `GET` | `/api/v1/lectures/{id}` | Получить ресурс по идентификатору | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureRestController.one` | — |
| `POST` | `/api/v1/lectures` | Создать ресурс | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureRestController.create` | `lectures.api.js` |
| `PUT` | `/api/v1/lectures/{id}` | Изменить ресурс | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureRestController.update` | `lectures.api.js` |
| `PUT` | `/api/v1/lectures/{id}/linked-test` | Изменить legacy-связь лекции с тестом | TEACHER / ADMIN + courses.manage/ownership | `200` | `LectureRestController.updateLinkedTest` | — |
| `DELETE` | `/api/v1/lectures/{id}` | Удалить ресурс | TEACHER / ADMIN + courses.manage/ownership | `204` | `LectureRestController.delete` | `lectures.api.js` |
| `GET` | `/api/v1/status` | Проверить состояние backend | Anonymous | `200` | `MainRestController.status` | — |
| `GET` | `/api/v1/status/readiness` | Проверить готовность backend и БД | Anonymous | `200` | `MainRestController.readiness` | — |
| `GET` | `/api/v1/memberships/faculties` | Получить membership факультетов | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.facultyMembers` | — |
| `GET` | `/api/v1/memberships/faculties/{facultyId}` | Получить участников факультета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.facultyMembersByFaculty` | — |
| `GET` | `/api/v1/memberships/faculties/memberships/{membershipId}` | Получить membership факультета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.facultyMembership` | — |
| `POST` | `/api/v1/memberships/faculties/{facultyId}` | Добавить человека на факультет | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.addFacultyMember` | — |
| `PUT` | `/api/v1/memberships/faculties/memberships/{membershipId}/status` | Изменить статус membership факультета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.updateFacultyMembershipStatus` | — |
| `PUT` | `/api/v1/memberships/faculties/memberships/{membershipId}` | Изменить membership факультета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.updateFacultyMembership` | — |
| `GET` | `/api/v1/memberships/groups` | Получить membership учебных групп | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.groupMembers` | `memberships.api.js` |
| `POST` | `/api/v1/memberships/groups/{groupId}` | Добавить человека в группу | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.addGroupMember` | `memberships.api.js` |
| `GET` | `/api/v1/memberships/groups/{groupId}` | Получить membership учебных групп | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.groupMembers` | — |
| `GET` | `/api/v1/memberships/groups/memberships/{membershipId}` | Получить membership группы | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.groupMembership` | — |
| `PUT` | `/api/v1/memberships/groups/memberships/{membershipId}/status` | Изменить статус membership группы | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.updateGroupMembershipStatus` | `memberships.api.js` |
| `PUT` | `/api/v1/memberships/groups/memberships/{membershipId}` | Изменить membership группы | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.updateGroupMembership` | — |
| `GET` | `/api/v1/memberships/subjects` | Получить membership предметов | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.subjectMembers` | `memberships.api.js` |
| `GET` | `/api/v1/memberships/subjects/{subjectId}` | Получить участников предмета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.subjectMembersBySubject` | — |
| `GET` | `/api/v1/memberships/subjects/memberships/{membershipId}` | Получить membership предмета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.subjectMembership` | `memberships.api.js` |
| `POST` | `/api/v1/memberships/subjects/{subjectId}` | Добавить человека к предмету | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.addSubjectMember` | `memberships.api.js` |
| `PUT` | `/api/v1/memberships/subjects/memberships/{membershipId}/status` | Изменить статус membership предмета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.updateSubjectMembershipStatus` | `memberships.api.js` |
| `PUT` | `/api/v1/memberships/subjects/memberships/{membershipId}` | Изменить membership предмета | Authenticated read / ADMIN-authority write | `200` | `MembershipRestController.updateSubjectMembership` | `memberships.api.js` |
| `GET` | `/api/v1/public/learning/subjects/{subjectId}` | Получить доступный студенту предмет | Authenticated + student context | `200` | `PublicLearningRestController.subject` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/subjects/{subjectId}/lectures` | Получить лекции предмета студента | Authenticated + student context | `200` | `PublicLearningRestController.subjectLectures` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}` | Получить лекцию студента | Authenticated + student context | `200` | `PublicLearningRestController.lecture` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/tests` | Получить тесты лекции и их доступность | Authenticated + student context | `200` | `PublicLearningRestController.lectureTests` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/materials` | Получить материалы лекции | Authenticated + student context | `200` | `PublicLearningRestController.lectureMaterials` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/lectures/{lectureId}/materials/{materialId}/download` | Скачать материал лекции | Authenticated + student context | `200` | `PublicLearningRestController.downloadLectureMaterial` | `learning.api.js` |
| `GET` | `/api/v1/public/learning/tests/{testId}` | Получить публичные метаданные теста без старта попытки | Authenticated + student context | `200` | `PublicLearningRestController.test` | — |
| `POST` | `/api/v1/public/learning/test-assignments/{assignmentId}/attempts/start` | Начать или продолжить попытку теста | Authenticated + student context | `200` | `PublicLearningRestController.startAttempt` | `learning.api.js` |
| `POST` | `/api/v1/public/learning/attempts/{attemptId}/submit` | Отправить и завершить попытку | Authenticated + student context | `200` | `PublicLearningRestController.submitAttempt` | `learning.api.js` |
| `GET` | `/api/v1/questions` | Получить банк вопросов | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.questions` | `questions.api.js` |
| `GET` | `/api/v1/questions/{questionId}` | Получить ресурс по идентификатору | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.one` | — |
| `GET` | `/api/v1/questions/{questionId}/options` | Получить варианты ответа вопроса | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.options` | `questions.api.js` |
| `POST` | `/api/v1/questions/import` | Импортировать вопросы из DOCX | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.importDocx` | `questions.api.js` |
| `POST` | `/api/v1/questions` | Создать ресурс | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.create` | `questions.api.js` |
| `PUT` | `/api/v1/questions/{questionId}` | Изменить ресурс | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.update` | `questions.api.js` |
| `GET` | `/api/v1/questions/options/{optionId}` | Получить вариант ответа | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.option` | `questions.api.js` |
| `POST` | `/api/v1/questions/{questionId}/options` | Добавить вариант ответа | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.addOption` | `questions.api.js` |
| `PUT` | `/api/v1/questions/options/{optionId}` | Изменить вариант ответа | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.updateOption` | `questions.api.js` |
| `PUT` | `/api/v1/questions/{questionId}/active` | Изменить активность ресурса | TEACHER / ADMIN + questions.manage/ownership | `200` | `QuestionUploadRestController.setActive` | `questions.api.js` |
| `GET` | `/api/v1/results/teacher/subjects` | Получить предметы для просмотра результатов преподавателем | TEACHER / ADMIN + tests.manage/object access | `200` | `ResultRestController.teacherSubjects` | `results.api.js` |
| `GET` | `/api/v1/results/student/subjects` | Получить предметы с результатами студента | Authenticated + own data | `200` | `ResultRestController.studentSubjects` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/lectures` | Получить лекции для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `ResultRestController.lectures` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/tests` | Получить тесты для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `ResultRestController.tests` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/groups` | Получить группы для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `ResultRestController.groups` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/students` | Получить студентов для фильтра результатов | TEACHER / ADMIN + tests.manage/object access | `200` | `ResultRestController.students` | `results.api.js` |
| `GET` | `/api/v1/results/teacher/data` | Получить результаты преподавателя/администратора | TEACHER / ADMIN + tests.manage/object access | `200` | `ResultRestController.data` | `results.api.js` |
| `GET` | `/api/v1/results/student/data` | Получить собственные результаты студента | Authenticated + own data | `200` | `ResultRestController.studentData` | `results.api.js` |
| `GET` | `/api/v1/roles` | Получить список ресурсов | ADMIN / roles.manage | `200` | `RoleRestController.all` | `roles.api.js` |
| `POST` | `/api/v1/roles` | Создать ресурс | ADMIN / roles.manage | `200` | `RoleRestController.create` | `roles.api.js` |
| `GET` | `/api/v1/roles/permissions` | Получить permissions | ADMIN / roles.manage | `200` | `RoleRestController.permissions` | `roles.api.js` |
| `POST` | `/api/v1/roles/permissions` | Создать permission | ADMIN / roles.manage | `200` | `RoleRestController.createPermission` | `roles.api.js` |
| `PUT` | `/api/v1/roles/{id}/permissions` | Заменить permissions роли | ADMIN / roles.manage | `200` | `RoleRestController.setRolePermissions` | `roles.api.js` |
| `GET` | `/api/v1/subjects` | Получить список ресурсов | Authenticated read / ADMIN or academic.manage write | `200` | `SubjectRestController.all` | `subjects.api.js` |
| `GET` | `/api/v1/subjects/{id}` | Получить ресурс по идентификатору | Authenticated read / ADMIN or academic.manage write | `200` | `SubjectRestController.one` | `subjects.api.js` |
| `GET` | `/api/v1/subjects/{id}/faculties` | Получить связанные факультеты | Authenticated read / ADMIN or academic.manage write | `200` | `SubjectRestController.faculties` | — |
| `POST` | `/api/v1/subjects` | Создать ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `SubjectRestController.create` | `subjects.api.js` |
| `PUT` | `/api/v1/subjects/{id}` | Изменить ресурс | Authenticated read / ADMIN or academic.manage write | `200` | `SubjectRestController.update` | `subjects.api.js` |
| `DELETE` | `/api/v1/subjects/{id}` | Удалить ресурс | Authenticated read / ADMIN or academic.manage write | `204` | `SubjectRestController.delete` | `subjects.api.js` |
| `GET` | `/api/v1/teaching/load-types` | Получить типы учебной нагрузки | Authenticated + domain access checks | `200` | `TeachingRestController.loadTypes` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/load-types/{loadTypeId}` | Получить тип нагрузки | Authenticated + domain access checks | `200` | `TeachingRestController.loadType` | — |
| `POST` | `/api/v1/teaching/load-types` | Создать тип нагрузки | ADMIN-authority write | `200` | `TeachingRestController.createLoadType` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/load-types/{loadTypeId}` | Изменить тип нагрузки | ADMIN-authority write | `200` | `TeachingRestController.updateLoadType` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/subject-load-types` | Получить типы нагрузки membership предмета | Authenticated + domain access checks | `200` | `TeachingRestController.getSubjectLoadTypes` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/subject-memberships/{subjectMembershipId}/load-types` | Назначить тип нагрузки предметному membership | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.addSubjectLoadType` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/subject-load-types/{subjectLoadTypeId}/status` | Изменить статус типа нагрузки membership | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.updateSubjectLoadTypeStatus` | — |
| `PUT` | `/api/v1/teaching/subject-load-types/{subjectLoadTypeId}` | Изменить связь типа нагрузки | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.updateSubjectLoadType` | — |
| `GET` | `/api/v1/teaching/assignments` | Получить учебные/тестовые назначения | Authenticated + domain access checks | `200` | `TeachingRestController.assignments` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/assignments/{id}` | Получить назначение | Authenticated + domain access checks | `200` | `TeachingRestController.assignment` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/assignments` | Создать учебное назначение | ADMIN-authority write | `200` | `TeachingRestController.createAssignment` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/assignments/{assignmentId}` | Изменить назначение | ADMIN-authority write | `200` | `TeachingRestController.updateAssignment` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/assignments/{assignmentId}/status` | Изменить статус назначения | ADMIN-authority write | `200` | `TeachingRestController.updateAssignmentStatus` | — |
| `GET` | `/api/v1/teaching/enrollments` | Получить enrollment студентов | Authenticated + domain access checks | `200` | `TeachingRestController.enrollments` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/assignments/{assignmentId}/enrollments` | Добавить enrollment | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.enroll` | — |
| `PUT` | `/api/v1/teaching/enrollments/{enrollmentId}/status` | Изменить статус enrollment | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.updateEnrollmentStatus` | — |
| `GET` | `/api/v1/teaching/lecture-assignments` | Получить назначения лекций | Authenticated + domain access checks | `200` | `TeachingRestController.lectureAssignments` | `teaching.api.js` |
| `POST` | `/api/v1/teaching/assignments/{assignmentId}/lecture-assignments` | Назначить лекцию | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.assignLecture` | `teaching.api.js` |
| `PUT` | `/api/v1/teaching/lecture-assignments/{lectureAssignmentId}` | Изменить назначение лекции | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.updateLectureAssignment` | — |
| `PUT` | `/api/v1/teaching/lecture-assignments/{lectureAssignmentId}/status` | Изменить статус назначения лекции | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.updateLectureAssignmentStatus` | `teaching.api.js` |
| `GET` | `/api/v1/teaching/lecture-progress` | Получить прогресс лекций | Authenticated + domain access checks | `200` | `TeachingRestController.progress` | — |
| `POST` | `/api/v1/teaching/lecture-assignments/{lectureAssignmentId}/progress` | Обновить прогресс лекции | TEACHER / ADMIN + teaching.manage/object access | `200` | `TeachingRestController.updateProgress` | — |
| `GET` | `/api/v1/tests` | Получить список ресурсов | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.all` | `tests.api.js` |
| `GET` | `/api/v1/tests/{id}` | Получить ресурс по идентификатору | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.one` | — |
| `POST` | `/api/v1/tests` | Создать ресурс | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.create` | `tests.api.js` |
| `PUT` | `/api/v1/tests/{id}` | Изменить ресурс | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.update` | — |
| `DELETE` | `/api/v1/tests/{id}` | Удалить ресурс | TEACHER / ADMIN + tests.manage/ownership | `204` | `TestRestController.delete` | `tests.api.js` |
| `GET` | `/api/v1/tests/{testId}/selection-rules` | Получить правила отбора вопросов | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.selectionRules` | — |
| `PUT` | `/api/v1/tests/{testId}/selection-rules` | Заменить правила отбора вопросов | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.replaceSelectionRules` | — |
| `GET` | `/api/v1/tests/assignments` | Получить учебные/тестовые назначения | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.assignments` | — |
| `GET` | `/api/v1/tests/assignments/{assignmentId}` | Получить назначение | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.assignment` | — |
| `POST` | `/api/v1/tests/{testId}/assignments` | Создать назначение теста | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.assign` | `tests.api.js` |
| `PUT` | `/api/v1/tests/assignments/{assignmentId}` | Изменить назначение | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.updateAssignment` | — |
| `PUT` | `/api/v1/tests/assignments/{assignmentId}/status` | Изменить статус назначения | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.updateAssignmentStatus` | — |
| `GET` | `/api/v1/tests/attempts` | Получить попытки тестов | TEACHER / ADMIN + tests.manage/ownership | `200` | `TestRestController.attempts` | — |
| `GET` | `/api/v1/tests/attempts/{attemptId}` | Получить попытку | Administrative authority | `200` | `TestRestController.attempt` | — |
| `POST` | `/api/v1/tests/assignments/{assignmentId}/attempts` | Начать или продолжить попытку теста | Administrative authority | `200` | `TestRestController.startAttempt` | — |
| `GET` | `/api/v1/tests/attempts/{attemptId}/responses` | Получить ответы попытки | Administrative authority | `200` | `TestRestController.responses` | — |
| `POST` | `/api/v1/tests/attempts/{attemptId}/responses` | Сохранить ответ попытки | Administrative authority | `200` | `TestRestController.submitResponse` | — |
| `GET` | `/api/v1/tests/responses/{responseId}/selected-options` | Получить выбранные варианты ответа | Administrative authority | `200` | `TestRestController.selectedOptions` | — |
| `POST` | `/api/v1/tests/attempts/{attemptId}/complete` | Завершить попытку | Administrative authority | `200` | `TestRestController.completeAttempt` | — |
| `GET` | `/api/v1/topics` | Получить список ресурсов | TEACHER / ADMIN + questions.manage/ownership | `200` | `TopicRestController.all` | `topics.api.js` |
| `GET` | `/api/v1/topics/{id}` | Получить ресурс по идентификатору | TEACHER / ADMIN + questions.manage/ownership | `200` | `TopicRestController.one` | `topics.api.js` |
| `POST` | `/api/v1/topics` | Создать ресурс | TEACHER / ADMIN + questions.manage/ownership | `201` | `TopicRestController.create` | `topics.api.js` |
| `PUT` | `/api/v1/topics/{id}` | Изменить ресурс | TEACHER / ADMIN + questions.manage/ownership | `200` | `TopicRestController.update` | `topics.api.js` |
| `DELETE` | `/api/v1/topics/{id}` | Удалить ресурс | TEACHER / ADMIN + questions.manage/ownership | `204` | `TopicRestController.delete` | `topics.api.js` |
| `GET` | `/api/v1/users` | Получить пользователей | ADMIN / users.read | `200` | `UserProfileRestController.users` | `users.api.js` |
| `GET` | `/api/v1/users/{id}` | Получить пользователя | ADMIN / users.read | `200` | `UserProfileRestController.user` | — |
| `GET` | `/api/v1/users/me` | Получить сведения о текущем пользователе | Authenticated | `200` | `UserProfileRestController.me` | `users.api.js` |
| `GET` | `/api/v1/users/people` | Получить профили Person | ADMIN/TEACHER or people/users permission | `200` | `UserProfileRestController.people` | `users.api.js` |
| `GET` | `/api/v1/users/people/{personId}` | Получить профиль Person | ADMIN/TEACHER or people/users permission | `200` | `UserProfileRestController.person` | `users.api.js` |
| `POST` | `/api/v1/users/people` | Создать профиль Person | ADMIN/TEACHER or people/users permission | `200` | `UserProfileRestController.createPerson` | `users.api.js` |
| `PUT` | `/api/v1/users/people/{personId}` | Изменить профиль Person | ADMIN/TEACHER or people/users permission | `200` | `UserProfileRestController.updatePerson` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/active` | Изменить активность ресурса | ADMIN / users.write | `200` | `UserProfileRestController.setActive` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/person` | Привязать User к Person | ADMIN / roles.manage | `204` | `UserProfileRestController.setPersonBinding` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/roles` | Заменить роли пользователя | ADMIN / roles.manage | `200` | `UserRoleRestController.setRoles` | `users.api.js` |
| `PUT` | `/api/v1/users/{id}/permissions` | Заменить прямые permissions пользователя | ADMIN / roles.manage | `200` | `UserRoleRestController.setPermissions` | — |

## Итог

Текущий backend публикует **159 endpoint'ов**. Frontend объявляет **114 HTTP-вызовов**, и каждый из них сопоставляется с существующим backend route. Остальные backend endpoints включают служебные, расширенные CRUD и низкоуровневые административные операции.
