package org.santayn.testing.mobile.core.storage

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

private class SettingsStore(private val settings: Settings) : KeyValueStore {
    override fun getString(key: String): String? = settings.getStringOrNull(key)

    override fun putString(key: String, value: String?) {
        if (value == null) settings.remove(key) else settings.putString(key, value)
    }

    override fun keys(): Set<String> = settings.keys
}

/** Настройки — NSUserDefaults. */
fun userDefaultsStore(): KeyValueStore =
    SettingsStore(NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))

/** Токены — Keychain. */
@OptIn(ExperimentalSettingsImplementation::class)
fun keychainStore(): KeyValueStore =
    SettingsStore(KeychainSettings(service = "org.santayn.testing.mobile"))
