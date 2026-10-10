package org.santayn.testing.mobile.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.santayn.testing.mobile.core.network.ConnectivityObserver
import org.santayn.testing.mobile.core.network.IosConnectivityObserver
import org.santayn.testing.mobile.core.platform.IosTextSpeaker
import org.santayn.testing.mobile.core.platform.TextSpeaker
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import org.santayn.testing.mobile.core.storage.PlatformStores
import org.santayn.testing.mobile.core.storage.keychainStore
import org.santayn.testing.mobile.core.storage.userDefaultsStore

actual fun platformModule(): Module = module {
    single { PlatformStores(settings = userDefaultsStore(), secure = keychainStore(), dataDir = appDataDir()) }
    single<ConnectivityObserver> { IosConnectivityObserver() }
    single<TextSpeaker> { IosTextSpeaker() }
}

/** Application Support/<bundle> — данные, которые не должны попадать в общий доступ. */
private fun appDataDir(): String {
    val base = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)
        .first() as String
    return "$base/StudentTesting"
}
