# Каталог frontend-тестов

Каталог построен по `frontend/src/__tests__`. Колонка **Test declarations** считает объявления `it/test(...)` и `it/test.each(...)`; параметризованный тест во время выполнения может развернуться в несколько runtime cases.

Всего spec-файлов: **166**. Всего деклараций: **740**.

## `admin/`

| Spec | Test declarations |
|---|---:|
| `admin/database-backups/DatabaseBackupLifecycle.spec.js` | 4 |
| `admin/database-backups/DatabaseBackupTechnicalContract.spec.js` | 3 |
| `admin/database-backups/DatabaseBackupUtils.spec.js` | 7 |
| `admin/database-backups/DatabaseBackupWorkspace.spec.js` | 4 |
| `admin/faculties/AdminFacultiesData.spec.js` | 4 |
| `admin/faculties/AdminFacultiesOverlayExemplar.spec.js` | 6 |
| `admin/faculty-subjects/AdminFacultySubjectsData.spec.js` | 5 |
| `admin/faculty-subjects/AdminFacultySubjectsWorkspace.spec.js` | 4 |
| `admin/groups/AdminGroupMembersDrawer.spec.js` | 6 |
| `admin/groups/AdminGroupMembersDrawerComponent.spec.js` | 2 |
| `admin/groups/AdminGroupMembersRace.spec.js` | 2 |
| `admin/groups/AdminGroupMembersState.spec.js` | 5 |
| `admin/groups/AdminGroupsData.spec.js` | 5 |
| `admin/groups/AdminGroupsOverlayWorkspace.spec.js` | 6 |
| `admin/roles-permissions/AdminRolePermissionsDrawer.spec.js` | 2 |
| `admin/roles-permissions/AdminRolePermissionsEditor.spec.js` | 3 |
| `admin/roles-permissions/AdminRolesPermissionsData.spec.js` | 6 |
| `admin/roles-permissions/AdminRolesPermissionsMutations.spec.js` | 4 |
| `admin/roles-permissions/AdminRolesPermissionsWorkspace.spec.js` | 4 |
| `admin/subjects/AdminSubjectsData.spec.js` | 4 |
| `admin/subjects/AdminSubjectsOverlayWorkspace.spec.js` | 6 |
| `admin/teacher-subjects/AdminTeacherSubjectsData.spec.js` | 5 |
| `admin/teacher-subjects/AdminTeacherSubjectsWorkspace.spec.js` | 5 |
| `admin/teaching-assignments/AdminLoadTypeManager.spec.js` | 4 |
| `admin/teaching-assignments/AdminLoadTypeManagerComponent.spec.js` | 2 |
| `admin/teaching-assignments/AdminTeachingAssignmentDrawer.spec.js` | 3 |
| `admin/teaching-assignments/AdminTeachingAssignmentEditor.spec.js` | 4 |
| `admin/teaching-assignments/AdminTeachingAssignmentMutations.spec.js` | 4 |
| `admin/teaching-assignments/AdminTeachingAssignmentsData.spec.js` | 5 |
| `admin/teaching-assignments/AdminTeachingWorkloadWorkspace.spec.js` | 6 |
| `admin/teaching-assignments/TeachingAssignmentMembershipEligibility.spec.js` | 2 |
| `admin/users/AdminPersonCreationFlow.spec.js` | 4 |
| `admin/users/AdminPersonEditingFlow.spec.js` | 6 |
| `admin/users/AdminPersonEditor.spec.js` | 2 |
| `admin/users/AdminPersonEditorState.spec.js` | 4 |
| `admin/users/AdminUserDrawer.spec.js` | 3 |
| `admin/users/AdminUsersData.spec.js` | 5 |
| `admin/users/AdminUsersRolesWorkspace.spec.js` | 6 |
## `architecture/`

| Spec | Test declarations |
|---|---:|
| `architecture/AdminFrontendContext.spec.js` | 4 |
| `architecture/ApiContracts.spec.js` | 18 |
| `architecture/AuthProfileAuditClosure.spec.js` | 3 |
| `architecture/FormLifecycleContract.spec.js` | 3 |
| `architecture/FrontendEnvironmentContract.spec.js` | 1 |
| `architecture/MutationSubmissionGuard.spec.js` | 2 |
| `architecture/RequestLifecycleContract.spec.js` | 7 |
| `architecture/RuntimeErrorConsistency.spec.js` | 3 |
| `architecture/RuntimeResilience.spec.js` | 3 |
| `architecture/StaleContextHardening.spec.js` | 5 |
| `architecture/StudentTeacherSharedContext.spec.js` | 2 |
| `architecture/TeacherMembershipGuard.spec.js` | 4 |
| `architecture/WorkspaceMode.spec.js` | 5 |
## `auth/`

| Spec | Test declarations |
|---|---:|
| `auth/AccountPendingAccess.spec.js` | 3 |
| `auth/AccountPendingNavigationContract.spec.js` | 5 |
| `auth/AuthApiContract.spec.js` | 2 |
| `auth/AuthErrorMessage.spec.js` | 4 |
| `auth/AuthHttpSessionEpoch.spec.js` | 2 |
| `auth/AuthLifecycleHardening.spec.js` | 8 |
| `auth/LoginView.spec.js` | 5 |
| `auth/RegistrationFlowContract.spec.js` | 5 |
## `core/`

| Spec | Test declarations |
|---|---:|
| `core/AbortableRequestGuard.spec.js` | 3 |
| `core/ApiErrorCodeContract.spec.js` | 5 |
| `core/ApiErrorConsistency.spec.js` | 7 |
| `core/ApiErrorPresentation.spec.js` | 6 |
| `core/App.spec.js` | 1 |
| `core/BootstrapFailure.spec.js` | 3 |
| `core/FormErrorLifecycle.spec.js` | 4 |
| `core/LatestRequestGuard.spec.js` | 2 |
## `navigation/`

| Spec | Test declarations |
|---|---:|
| `navigation/AppBreadcrumb.spec.js` | 6 |
| `navigation/AppFooterNavigation.spec.js` | 3 |
| `navigation/AppHeaderNavigation.spec.js` | 3 |
| `navigation/AppSidebarNavigation.spec.js` | 9 |
| `navigation/ContextualNavigationContract.spec.js` | 4 |
| `navigation/HomeNavigationContract.spec.js` | 3 |
| `navigation/NavigationConfig.spec.js` | 15 |
| `navigation/WorkspaceRouteGuard.spec.js` | 6 |
## `profile/`

| Spec | Test declarations |
|---|---:|
| `profile/PasswordChange.spec.js` | 4 |
| `profile/ProfileView.spec.js` | 4 |
## `results/`

| Spec | Test declarations |
|---|---:|
| `results/MultiRoleResultsMode.spec.js` | 2 |
| `results/ResultContracts.spec.js` | 3 |
| `results/ResultScoring.spec.js` | 6 |
| `results/ResultsData.spec.js` | 4 |
| `results/ResultsFilters.spec.js` | 5 |
| `results/ResultsRequestCancellation.spec.js` | 5 |
| `results/ResultsSearchableFilters.spec.js` | 2 |
| `results/StudentBestAttemptScoring.spec.js` | 1 |
| `results/StudentResultVisibilityContract.spec.js` | 3 |
## `student/`

| Spec | Test declarations |
|---|---:|
| `student/TestAnswerProgress.spec.js` | 3 |
| `student/attempts/AttemptDraft.spec.js` | 4 |
| `student/attempts/LearningAttemptContract.spec.js` | 3 |
| `student/attempts/TestAttemptDraft.spec.js` | 4 |
| `student/attempts/TestAttemptLifecycle.spec.js` | 6 |
| `student/attempts/TestAttemptReload.spec.js` | 4 |
| `student/attempts/TestViewDraft.spec.js` | 6 |
| `student/attempts/TestViewRace.spec.js` | 5 |
| `student/learning/LearningContextCache.spec.js` | 4 |
| `student/learning/LearningContextMutationInvalidation.spec.js` | 2 |
| `student/learning/LectureDetailsWorkspaceMode.spec.js` | 1 |
| `student/learning/StudentLearningContext.spec.js` | 4 |
| `student/learning/StudentLearningRouterContract.spec.js` | 5 |
| `student/learning/StudentSubjectDetailsContract.spec.js` | 4 |
## `teacher/`

| Spec | Test declarations |
|---|---:|
| `teacher/context/TeacherSubjectSelectionEligibility.spec.js` | 7 |
| `teacher/context/TeacherSubjectsMembershipState.spec.js` | 4 |
| `teacher/course-templates/CourseTemplateAdminContext.spec.js` | 4 |
| `teacher/course-templates/CourseTemplateEditorDrawers.spec.js` | 3 |
| `teacher/course-templates/CourseTemplateEditors.spec.js` | 4 |
| `teacher/course-templates/CourseTemplateMutations.spec.js` | 8 |
| `teacher/course-templates/CourseTemplatesData.spec.js` | 6 |
| `teacher/course-templates/TeacherCourseTemplatesOverlayWorkspace.spec.js` | 7 |
| `teacher/lectures/LectureDelete.spec.js` | 3 |
| `teacher/lectures/LectureDrawerWorkspace.spec.js` | 4 |
| `teacher/lectures/LectureEditorDrawer.spec.js` | 5 |
| `teacher/lectures/LectureManagementData.spec.js` | 5 |
| `teacher/lectures/LectureMaterials.spec.js` | 5 |
| `teacher/lectures/LectureMaterialsManager.spec.js` | 4 |
| `teacher/lectures/LecturePartialCreateRetry.spec.js` | 2 |
| `teacher/lectures/LectureSaveFlow.spec.js` | 8 |
| `teacher/lectures/TeacherLecturesOverlayWorkspace.spec.js` | 5 |
| `teacher/questions/MatchingQuestionUx.spec.js` | 7 |
| `teacher/questions/QuestionDrawerWorkspace.spec.js` | 7 |
| `teacher/questions/QuestionEditorState.spec.js` | 4 |
| `teacher/questions/QuestionFilters.spec.js` | 5 |
| `teacher/questions/QuestionImport.spec.js` | 7 |
| `teacher/questions/QuestionMutations.spec.js` | 5 |
| `teacher/questions/QuestionOptions.spec.js` | 5 |
| `teacher/questions/QuestionTopicContext.spec.js` | 6 |
| `teacher/questions/QuestionsList.spec.js` | 5 |
| `teacher/questions/TeacherQuestionsOverlayWorkspace.spec.js` | 8 |
| `teacher/tests/TestCreationFlow.spec.js` | 3 |
| `teacher/tests/TestEditorSaveFlow.spec.js` | 4 |
| `teacher/tests/TestEditorState.spec.js` | 4 |
| `teacher/tests/TestEditorUnsavedNavigation.spec.js` | 5 |
| `teacher/tests/UnsavedNavigationGuard.spec.js` | 5 |
| `teacher/topics/TeacherTopicMutations.spec.js` | 5 |
| `teacher/topics/TeacherTopicsData.spec.js` | 5 |
| `teacher/topics/TeacherTopicsOverlayExemplar.spec.js` | 6 |
| `teacher/workload/TeacherWorkloadData.spec.js` | 7 |
| `teacher/workload/TeacherWorkloadMembershipContext.spec.js` | 2 |
| `teacher/workload/TeacherWorkloadPresentation.spec.js` | 3 |
| `teacher/workload/TeacherWorkloadWorkspaceFilters.spec.js` | 5 |
## `ui/`

| Spec | Test declarations |
|---|---:|
| `ui/AccessibilityMetadata.spec.js` | 5 |
| `ui/AccessibilityMetadataContract.spec.js` | 3 |
| `ui/AdminExperienceStage4.spec.js` | 6 |
| `ui/AppRuntimeErrorBoundary.spec.js` | 1 |
| `ui/FinalVisualAccessibilityStage5.spec.js` | 4 |
| `ui/HeaderFooterPolish.spec.js` | 3 |
| `ui/LectureContentTypography.spec.js` | 5 |
| `ui/LegacyThemeRemoval.spec.js` | 6 |
| `ui/MotionContract.spec.js` | 4 |
| `ui/MotionOverlayContract.spec.js` | 6 |
| `ui/MotionStateRevealContract.spec.js` | 4 |
| `ui/OverlayCrudFoundation.spec.js` | 6 |
| `ui/RouteMotionContract.spec.js` | 5 |
| `ui/ScrollbarContract.spec.js` | 4 |
| `ui/ServicePagesThemeMigration.spec.js` | 3 |
| `ui/StudentPagesResponsiveLayout.spec.js` | 4 |
| `ui/TeacherExperienceStage3.spec.js` | 5 |
| `ui/TeacherPagesResponsiveLayout.spec.js` | 6 |
| `ui/TeacherSectionFinalUiAudit.spec.js` | 5 |
| `ui/TechnicalIdPresentation.spec.js` | 2 |
| `ui/TypographyFoundation.spec.js` | 3 |
| `ui/UiChoiceControls.spec.js` | 4 |
| `ui/UiFoundation.spec.js` | 3 |
| `ui/UiFoundationComponents.spec.js` | 1 |
| `ui/UiFoundationStage1.spec.js` | 5 |
| `ui/UiMobileResponsiveness.spec.js` | 5 |
| `ui/UiPaletteComponents.spec.js` | 5 |
