package org.santayn.testing.mobile.core.files

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.RawSink
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Материалы лекций, сохранённые для просмотра без сети.
 *
 * Каждый материал лежит в своей папке `<lectureId>-<materialId>/<имя файла>`.
 * Скачивание пишется во временную папку и подменяет старую копию только целиком —
 * оборванная загрузка не портит сохранённый файл.
 * Общий объём ограничен [maxBytes] (настройка в профиле): при переполнении удаляются давно скачанные
 * материалы. Очищается при выходе из аккаунта.
 */
class OfflineFiles(
    rootPath: String,
    private val maxBytes: () -> Long,
) {
    private val root = PlatformFile(rootPath)
    private val mutex = Mutex()

    private fun dirFor(lectureId: Int, materialId: Int) = root / "$lectureId-$materialId"

    /**
     * Скачивает материал через [download] прямо в файл и сохраняет его.
     * [download] получает функцию, открывающую файл по имени из ответа сервера.
     */
    suspend fun save(
        lectureId: Int,
        materialId: Int,
        download: suspend (open: suspend (String) -> RawSink) -> String,
    ): PlatformFile {
        val tmp = root / "tmp-$lectureId-$materialId"
        io {
            deleteTree(tmp)
            tmp.createDirectories()
        }
        try {
            val fileName = download { name -> io { (tmp / safeFileName(name)).sink(append = false) } }
            return locked {
                val dir = dirFor(lectureId, materialId)
                deleteTree(dir)
                SystemFileSystem.atomicMove(Path(tmp.path), Path(dir.path))
                evict(keep = dir)
                dir / safeFileName(fileName)
            }
        } catch (e: Throwable) {
            io { deleteTree(tmp) }
            throw e
        }
    }

    /** Сохранённая копия материала или null. */
    suspend fun find(lectureId: Int, materialId: Int): PlatformFile? = locked {
        val dir = dirFor(lectureId, materialId)
        if (!dir.exists()) null else dir.list().firstOrNull { !it.isDirectory() }
    }

    /** Id материалов лекции, доступных без сети. */
    suspend fun savedMaterialIds(lectureId: Int): Set<Int> = locked {
        if (!root.exists()) return@locked emptySet()
        root.list().mapNotNull { dir ->
            val (lecture, material) = dir.name.split('-').takeIf { it.size == 2 } ?: return@mapNotNull null
            material.toIntOrNull()?.takeIf { lecture.toIntOrNull() == lectureId && dir.list().isNotEmpty() }
        }.toSet()
    }

    /** Занятое место, байт. */
    suspend fun totalBytes(): Long = locked { savedDirs().sumOf { it.second } }

    suspend fun clear() = locked {
        deleteTree(root)
    }

    /** Применить новый лимит (после смены настройки). */
    suspend fun trimToLimit() = locked { evict(keep = null) }

    private fun savedDirs(): List<Triple<PlatformFile, Long, kotlin.time.Instant?>> {
        if (!root.exists()) return emptyList()
        return root.list().filter { it.isDirectory() && !it.name.startsWith("tmp-") }.map { dir ->
            val files = dir.list()
            Triple(dir, files.sumOf { it.size() }, files.maxOfOrNull { it.lastModified() })
        }
    }

    private suspend fun evict(keep: PlatformFile?) {
        val dirs = savedDirs()
        val limit = maxBytes()
        var total = dirs.sumOf { it.second }
        for ((dir, size, _) in dirs.sortedBy { it.third }) {
            if (total <= limit) break
            if (keep != null && dir.name == keep.name) continue
            deleteTree(dir)
            total -= size
        }
    }

    /** Рекурсивное удаление папки (или файла). */
    private fun deleteTree(file: PlatformFile) = deleteTree(Path(file.path))

    private fun deleteTree(path: Path) {
        val fs = SystemFileSystem
        if (fs.metadataOrNull(path)?.isDirectory == true) fs.list(path).forEach { deleteTree(it) }
        fs.delete(path, mustExist = false)
    }

    private suspend fun <T> io(block: suspend () -> T): T = withContext(Dispatchers.IO) { block() }

    private suspend fun <T> locked(block: suspend () -> T): T = mutex.withLock { io(block) }
}
