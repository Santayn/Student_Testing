package org.santayn.testing.mobile.core.storage

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.santayn.testing.mobile.core.util.nowInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Плоское хранилище текстовых файлов (для тестов есть реализация в памяти). */
interface BlobStore {
    fun read(name: String): String?
    fun write(name: String, content: String)
    fun delete(name: String)
    fun clear()
}

/** Файлы в каталоге приложения через kotlinx-io. */
class FileBlobStore(private val dir: Path) : BlobStore {
    private val fs = SystemFileSystem

    private fun file(name: String) = Path(dir, name)

    override fun read(name: String): String? {
        val path = file(name)
        if (!fs.exists(path)) return null
        return fs.source(path).buffered().use { it.readString() }
    }

    override fun write(name: String, content: String) {
        fs.createDirectories(dir)
        val tmp = file("$name.tmp")
        fs.sink(tmp).buffered().use { it.writeString(content) }
        fs.atomicMove(tmp, file(name))
    }

    override fun delete(name: String) {
        fs.delete(file(name), mustExist = false)
    }

    override fun clear() {
        if (!fs.exists(dir)) return
        fs.list(dir).forEach { fs.delete(it, mustExist = false) }
    }
}

class MemoryBlobStore : BlobStore {
    val files = mutableMapOf<String, String>()
    override fun read(name: String) = files[name]
    override fun write(name: String, content: String) { files[name] = content }
    override fun delete(name: String) { files.remove(name) }
    override fun clear() = files.clear()
}

@Serializable
private data class CachedEntry(val key: String, val savedAtMs: Long, val body: String)

@Serializable
private data class IndexEntry(val savedAtMs: Long, val size: Int)

/** Сохранённый ответ сервера. */
class CachedResponse(val body: String, val savedAt: Instant)

/**
 * Офлайн-кеш GET-ответов API: последний успешный ответ по ключу «пользователь + URL».
 *
 * Используется, когда сервер недоступен или на устройстве нет сети.
 * Размер ограничен ([maxBytes], [maxEntries]); при переполнении удаляются самые старые записи.
 * Очищается при выходе из аккаунта.
 */
class ResponseCache(
    private val store: BlobStore,
    private val json: Json,
    private val maxBytes: Long = 20L * 1024 * 1024,
    private val maxEntries: Int = 1_000,
    private val maxAge: Duration = 30.days,
    private val maxEntryBytes: Int = 2 * 1024 * 1024,
) {
    private val mutex = Mutex()
    private var index: MutableMap<String, IndexEntry>? = null

    suspend fun get(key: String): CachedResponse? = io(null) {
        val name = fileName(key)
        val entry = store.read(name)
            ?.let { runCatching { json.decodeFromString(CachedEntry.serializer(), it) }.getOrNull() }
            ?.takeIf { it.key == key }
            ?: return@io null
        val savedAt = Instant.fromEpochMilliseconds(entry.savedAtMs)
        if (nowInstant() - savedAt > maxAge) return@io null
        CachedResponse(entry.body, savedAt)
    }

    suspend fun put(key: String, body: String): Unit = io(Unit) {
        if (body.length > maxEntryBytes) return@io
        val name = fileName(key)
        val now = nowInstant().toEpochMilliseconds()
        store.write(name, json.encodeToString(CachedEntry.serializer(), CachedEntry(key, now, body)))
        val idx = loadIndex()
        idx[name] = IndexEntry(now, body.length)
        evict(idx)
        saveIndex(idx)
    }

    /** Занятое место, байт (по индексу). */
    suspend fun totalBytes(): Long = io(0L) { loadIndex().values.sumOf { it.size.toLong() } }

    suspend fun clear(): Unit = io(Unit) {
        store.clear()
        index = mutableMapOf()
    }

    private fun evict(idx: MutableMap<String, IndexEntry>) {
        var total = idx.values.sumOf { it.size.toLong() }
        if (total <= maxBytes && idx.size <= maxEntries) return
        for ((name, entry) in idx.entries.sortedBy { it.value.savedAtMs }) {
            if (total <= maxBytes && idx.size <= maxEntries) break
            store.delete(name)
            idx.remove(name)
            total -= entry.size
        }
    }

    private fun loadIndex(): MutableMap<String, IndexEntry> = index ?: run {
        val loaded = store.read(INDEX)?.let {
            runCatching { json.decodeFromString<Map<String, IndexEntry>>(it) }.getOrNull()
        }.orEmpty().toMutableMap()
        index = loaded
        loaded
    }

    private fun saveIndex(idx: Map<String, IndexEntry>) {
        store.write(INDEX, json.encodeToString(idx))
    }

    /** Кеш не должен ломать работу приложения: ошибки диска логируются, возвращается [fallback]. */
    private suspend fun <T> io(fallback: T, block: () -> T): T = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching(block).getOrElse { error ->
                Logger.withTag("ResponseCache").w(error) { "cache io failed" }
                fallback
            }
        }
    }

    companion object {
        private const val INDEX = "index.json"

        /** Имя файла — 64-битный FNV-1a от ключа; полный ключ хранится внутри для проверки. */
        fun fileName(key: String): String {
            var hash = -0x340d631b7bdddcdbL
            for (ch in key) {
                hash = hash xor ch.code.toLong()
                hash *= 0x100000001b3L
            }
            return hash.toULong().toString(16).padStart(16, '0') + ".json"
        }
    }
}
