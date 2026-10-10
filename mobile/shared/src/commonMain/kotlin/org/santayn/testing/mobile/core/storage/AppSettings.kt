package org.santayn.testing.mobile.core.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.santayn.testing.mobile.core.platform.defaultServerUrl

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Место под материалы лекций для работы без сети, МБ. */
object OfflineFilesLimit {
    const val MIN_MB = 100
    const val MAX_MB = 10 * 1024
    const val DEFAULT_MB = 200

    private val UNIT_REGEX = Regex("""^(\d+(?:\.\d+)?)\s*(мб|mb|м|m|гб|gb|г|g)?$""")

    fun clamp(mb: Int) = mb.coerceIn(MIN_MB, MAX_MB)

    fun bytes(mb: Int): Long = mb * 1024L * 1024L

    /** «200 МБ», «1.5 ГБ», «10 ГБ». */
    fun title(mb: Int): String = if (mb < 1024) {
        "$mb МБ"
    } else {
        val gb = (mb / 1024.0 * 10).toLong() / 10.0
        (if (gb % 1.0 == 0.0) gb.toLong().toString() else gb.toString()) + " ГБ"
    }

    /**
     * Разбор ввода с клавиатуры: «500», «500 мб», «2 гб», «1,5 GB». Число без единиц — мегабайты.
     * @return МБ или null, если ввод не распознан (без ограничения диапазона — его применяет [clamp]).
     */
    fun parse(input: String): Int? {
        val text = input.trim().lowercase().replace(',', '.')
        val match = UNIT_REGEX.find(text) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2]
        val mb = if (unit.startsWith("г") || unit.startsWith("g")) value * 1024 else value
        return mb.toInt()
    }
}

/** Размер текста поверх системной настройки шрифта. */
enum class TextScale(val factor: Float, val title: String) {
    STANDARD(1f, "Обычный"),
    LARGE(1.25f, "Крупный"),
    LARGEST(1.5f, "Очень крупный"),
}

/** Настройки приложения (аналог stores/theme.js + env-переменных фронта). */
class AppSettings(private val store: KeyValueStore) {

    private val _themeMode = MutableStateFlow(
        store.getString(KEY_THEME)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _textScale = MutableStateFlow(
        store.getString(KEY_TEXT_SCALE)
            ?.let { runCatching { TextScale.valueOf(it) }.getOrNull() }
            ?: TextScale.STANDARD
    )
    val textScale: StateFlow<TextScale> = _textScale.asStateFlow()

    private val _offlineFilesLimitMb = MutableStateFlow(
        store.getString(KEY_OFFLINE_LIMIT)?.toIntOrNull()?.let(OfflineFilesLimit::clamp) ?: OfflineFilesLimit.DEFAULT_MB
    )

    /** Лимит места под материалы лекций, МБ (100 МБ … 10 ГБ). */
    val offlineFilesLimitMb: StateFlow<Int> = _offlineFilesLimitMb.asStateFlow()

    fun setOfflineFilesLimitMb(mb: Int) {
        val value = OfflineFilesLimit.clamp(mb)
        store.putString(KEY_OFFLINE_LIMIT, value.toString())
        _offlineFilesLimitMb.value = value
    }

    private val _highContrast = MutableStateFlow(store.getString(KEY_HIGH_CONTRAST) == "true")
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    private val _serverUrl = MutableStateFlow(
        store.getString(KEY_SERVER_URL) ?: defaultServerUrl()
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        store.putString(KEY_THEME, mode.name)
        _themeMode.value = mode
    }

    fun setTextScale(scale: TextScale) {
        store.putString(KEY_TEXT_SCALE, scale.name)
        _textScale.value = scale
    }

    fun setHighContrast(enabled: Boolean) {
        store.putString(KEY_HIGH_CONTRAST, enabled.toString())
        _highContrast.value = enabled
    }

    fun setServerUrl(url: String) {
        val normalized = normalizeServerUrl(url)
        store.putString(KEY_SERVER_URL, normalized)
        _serverUrl.value = normalized
    }

    fun resetServerUrl() {
        store.putString(KEY_SERVER_URL, null)
        _serverUrl.value = defaultServerUrl()
    }

    var preferredWorkspaceRole: String?
        get() = store.getString(KEY_WORKSPACE_ROLE)
        set(value) = store.putString(KEY_WORKSPACE_ROLE, value)

    var lastLogin: String?
        get() = store.getString(KEY_LAST_LOGIN)
        set(value) = store.putString(KEY_LAST_LOGIN, value)

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_TEXT_SCALE = "text_scale"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_OFFLINE_LIMIT = "offline_files_limit_mb"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_WORKSPACE_ROLE = "workspace_role"
        private const val KEY_LAST_LOGIN = "last_login"

        /**
         * Приводит адрес к виду `http(s)://host[:port]/api/v1` без завершающего слэша.
         * Если пользователь ввёл только хост — добавляет схему и `/api/v1`.
         */
        fun normalizeServerUrl(raw: String): String {
            var url = raw.trim().trimEnd('/')
            if (url.isEmpty()) return defaultServerUrl()
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://$url"
            }
            if (!url.contains("/api/")) {
                url = "$url/api/v1"
            }
            return url
        }
    }
}
