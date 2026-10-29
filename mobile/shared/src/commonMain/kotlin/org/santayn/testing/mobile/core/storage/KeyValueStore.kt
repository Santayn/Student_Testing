package org.santayn.testing.mobile.core.storage

/**
 * Минимальное key-value хранилище строк.
 *
 * Платформенные реализации:
 * - обычное (настройки, черновики): SharedPreferences / NSUserDefaults;
 * - защищённое (токены): SharedPreferences + Tink/Keystore / Keychain.
 */
interface KeyValueStore {
    fun getString(key: String): String?

    fun putString(key: String, value: String?)

    fun keys(): Set<String>
}

/**
 * Хранилища, которые предоставляет платформа.
 * @property dataDir каталог данных приложения (офлайн-кеш, сохранённые материалы).
 */
class PlatformStores(
    val settings: KeyValueStore,
    val secure: KeyValueStore,
    val dataDir: String,
)
