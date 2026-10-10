package org.santayn.testing.mobile.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.feature.learning.LectureViewModel
import org.santayn.testing.mobile.feature.learning.SubjectDetailsViewModel
import org.santayn.testing.mobile.feature.learning.SubjectLecturesViewModel
import org.santayn.testing.mobile.feature.learning.SubjectsViewModel
import org.santayn.testing.mobile.feature.profile.ProfileViewModel
import org.santayn.testing.mobile.feature.results.ResultsViewModel
import org.santayn.testing.mobile.feature.tests.TestViewModel
import org.santayn.testing.mobile.feature.teacher.CourseTemplatesViewModel
import org.santayn.testing.mobile.feature.teacher.LectureManagementViewModel
import org.santayn.testing.mobile.feature.teacher.QuestionsViewModel
import org.santayn.testing.mobile.feature.teacher.TeacherSubjectsHolder
import org.santayn.testing.mobile.feature.teacher.TestEditorViewModel
import org.santayn.testing.mobile.feature.teacher.TopicsViewModel
import org.santayn.testing.mobile.feature.teacher.WorkloadViewModel
import org.santayn.testing.mobile.navigation.TeacherCoursesRoute
import org.santayn.testing.mobile.navigation.TeacherLecturesRoute
import org.santayn.testing.mobile.navigation.TeacherQuestionsRoute
import org.santayn.testing.mobile.navigation.TeacherTestCreateRoute
import org.santayn.testing.mobile.navigation.TeacherTopicsRoute
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.feature.admin.*

val viewModelModule = module {
    viewModelOf(::ProfileViewModel)

    // Обучение
    viewModel { (role: WorkspaceRole) -> SubjectsViewModel(role, get(), get(), get(), get(), get()) }
    viewModel { (subjectId: Int, role: WorkspaceRole) -> SubjectDetailsViewModel(subjectId, role, get(), get()) }
    viewModel { (subjectId: Int) -> SubjectLecturesViewModel(subjectId, get()) }
    viewModel { (lectureId: Int) -> LectureViewModel(lectureId, get(), get()) }
    viewModel { (testId: Int, assignmentId: Int) -> TestViewModel(testId, assignmentId, get(), get(), get()) }
    viewModel { (role: WorkspaceRole) -> ResultsViewModel(role, get(), get(), get()) }

    // Преподаватель
    factory { (role: WorkspaceRole) -> TeacherSubjectsHolder(role, get(), get(), get(), get(), get(), get()) }
    viewModel { (route: TeacherTopicsRoute, role: WorkspaceRole) ->
        TopicsViewModel(route.subjectId, get { parametersOf(role) }, get(), get())
    }
    viewModel { (route: TeacherQuestionsRoute, role: WorkspaceRole) ->
        QuestionsViewModel(route, get { parametersOf(role) }, get(), get())
    }
    viewModel { (route: TeacherTestCreateRoute, role: WorkspaceRole) ->
        TestEditorViewModel(route, get { parametersOf(role) }, get(), get(), get(), get(), get())
    }
    viewModel { (route: TeacherLecturesRoute, role: WorkspaceRole) ->
        LectureManagementViewModel(route, get { parametersOf(role) }, get(), get(), get())
    }
    viewModel { (route: TeacherCoursesRoute, role: WorkspaceRole) ->
        CourseTemplatesViewModel(route, get { parametersOf(role) }, get())
    }
    viewModelOf(::WorkloadViewModel)

    // Администратор
    viewModelOf(::FacultiesViewModel)
    viewModelOf(::SubjectsAdminViewModel)
    viewModelOf(::GroupsViewModel)
    viewModelOf(::FacultySubjectsViewModel)
    viewModelOf(::TeacherSubjectsViewModel)
    viewModelOf(::TeachingAssignmentsViewModel)
    viewModelOf(::UsersViewModel)
    viewModelOf(::RolesViewModel)
    viewModelOf(::BackupsViewModel)
}
