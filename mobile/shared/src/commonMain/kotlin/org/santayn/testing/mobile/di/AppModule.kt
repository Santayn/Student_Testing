package org.santayn.testing.mobile.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlinx.io.files.Path
import org.santayn.testing.mobile.core.files.OfflineFiles
import org.santayn.testing.mobile.core.network.ApiClient
import org.santayn.testing.mobile.core.network.AppJson
import org.santayn.testing.mobile.core.network.NetworkStatus
import org.santayn.testing.mobile.core.network.createHttpClient
import org.santayn.testing.mobile.core.storage.FileBlobStore
import org.santayn.testing.mobile.core.storage.OfflineFilesLimit
import org.santayn.testing.mobile.core.storage.ResponseCache
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.storage.AppSettings
import org.santayn.testing.mobile.core.storage.PlatformStores
import org.santayn.testing.mobile.data.api.*
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.StudentContextLoader
import org.santayn.testing.mobile.domain.TeacherContextLoader
import org.santayn.testing.mobile.domain.AttemptDraftStore
import org.santayn.testing.mobile.domain.TeacherSubjectSelection

/** Платформенные зависимости: хранилища, наблюдатель сети, синтез речи (Context на Android). */
expect fun platformModule(): Module

val coreModule = module {
    single { AppJson }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { createHttpClient(get()) }
    single { AppSettings(get<PlatformStores>().settings) }
    single { SessionManager(get(), get(), get<PlatformStores>().secure, get(), get()) }
    single { NetworkStatus(get(), get(), NetworkStatus.httpProbe(get(), get())) }
    single { ResponseCache(FileBlobStore(Path(get<PlatformStores>().dataDir, "http-cache")), get()) }
    single {
        val settings = get<AppSettings>()
        OfflineFiles(Path(get<PlatformStores>().dataDir, "offline-files").toString()) { OfflineFilesLimit.bytes(settings.offlineFilesLimitMb.value) }
    }
    single { ApiClient(get(), get(), get(), get(), get(), get()) }
    single { ContextCache() }
    single { AttemptDraftStore(get<PlatformStores>().settings, get()) }
    single { TeacherSubjectSelection(get<PlatformStores>().settings) }
}

val apiModule = module {
    singleOf(::FacultiesApi)
    singleOf(::GroupsApi)
    singleOf(::SubjectsApi)
    singleOf(::UsersApi)
    singleOf(::RolesApi)
    singleOf(::MembershipsApi)
    singleOf(::TeachingApi)
    singleOf(::CoursesApi)
    singleOf(::LecturesApi)
    singleOf(::TopicsApi)
    singleOf(::QuestionsApi)
    singleOf(::TestsApi)
    singleOf(::LearningApi)
    singleOf(::ResultsApi)
    singleOf(::DatabaseBackupsApi)
    singleOf(::StudentContextLoader)
    singleOf(::TeacherContextLoader)
}

fun appModules(): List<Module> = listOf(platformModule(), coreModule, apiModule, viewModelModule)
