package org.santayn.testing.mobile.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import org.santayn.testing.mobile.core.network.AndroidConnectivityObserver
import org.santayn.testing.mobile.core.network.ConnectivityObserver
import org.santayn.testing.mobile.core.platform.AndroidTextSpeaker
import org.santayn.testing.mobile.core.platform.TextSpeaker
import org.santayn.testing.mobile.core.storage.EncryptedPrefsStore
import org.santayn.testing.mobile.core.storage.PlatformStores
import org.santayn.testing.mobile.core.storage.SharedPrefsStore

actual fun platformModule(): Module = module {
    single {
        PlatformStores(
            settings = SharedPrefsStore(androidContext(), "student_testing_settings"),
            secure = EncryptedPrefsStore(androidContext(), "student_testing_secure"),
            dataDir = androidContext().filesDir.absolutePath,
        )
    }
    single<ConnectivityObserver> { AndroidConnectivityObserver(androidContext()) }
    single<TextSpeaker> { AndroidTextSpeaker(androidContext()) }
}
