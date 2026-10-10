package org.santayn.testing.mobile.feature.learning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.files.formatFileSize
import org.santayn.testing.mobile.core.files.openWithDefaultApp
import org.santayn.testing.mobile.core.files.OfflineFiles
import org.santayn.testing.mobile.core.network.ApiException
import org.santayn.testing.mobile.core.ui.SpeakButton
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.SessionManager
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.BusyButton
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.LoadState
import org.santayn.testing.mobile.core.ui.LoadStateContent
import org.santayn.testing.mobile.core.ui.NavCard
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatDuration
import org.santayn.testing.mobile.data.api.LearningApi
import org.santayn.testing.mobile.data.api.SubjectsApi
import org.santayn.testing.mobile.data.model.PublicLectureDto
import org.santayn.testing.mobile.data.model.PublicMaterialDto
import org.santayn.testing.mobile.data.model.PublicSubjectDto
import org.santayn.testing.mobile.data.model.PublicTestDto
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.domain.StudentContextLoader
import org.santayn.testing.mobile.domain.TeacherContextLoader
import org.santayn.testing.mobile.domain.sortedByName
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.LectureRoute
import org.santayn.testing.mobile.navigation.SubjectDetailsRoute
import org.santayn.testing.mobile.navigation.SubjectLecturesRoute
import org.santayn.testing.mobile.navigation.TeacherTopicsRoute
import org.santayn.testing.mobile.navigation.TestRoute

// region Subjects list (views/subjects/SubjectsView.vue)

data class SubjectItem(val id: Int, val name: String, val description: String?, val facultyId: Int?)

data class SubjectsData(val items: List<SubjectItem>, val emptyReason: String?)

class SubjectsViewModel(
    private val role: WorkspaceRole,
    private val session: SessionManager,
    private val subjects: SubjectsApi,
    private val studentLoader: StudentContextLoader,
    private val teacherLoader: TeacherContextLoader,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow<LoadState<SubjectsData>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    init {
        load()
    }

    fun load(force: Boolean = false) {
        if (_state.value !is LoadState.Ready) _state.value = LoadState.Loading else _refreshing.value = true
        launchSafe(onError = {
            _refreshing.value = false
            if (_state.value is LoadState.Ready) toast(it.userMessage()) else _state.value = LoadState.Error(it.userMessage())
        }) {
            if (force) cache.invalidate()
            val user = session.currentUser
            val data = when (role) {
                WorkspaceRole.ADMIN -> SubjectsData(
                    subjects.getAll().sortedByName { it.name }.map { SubjectItem(it.id, it.name, it.description, null) },
                    "В каталоге пока нет предметов.",
                )
                WorkspaceRole.TEACHER -> {
                    val ctx = teacherLoader.load(user?.personId, isAdmin = false)
                    SubjectsData(
                        ctx.subjects.map { SubjectItem(it.id, it.name, it.description, null) },
                        "Вы пока не закреплены ни за одним предметом. Обратитесь к администратору.",
                    )
                }
                WorkspaceRole.STUDENT -> {
                    val ctx = studentLoader.load(user?.personId)
                    SubjectsData(
                        ctx.subjects.map { SubjectItem(it.id, it.name, it.description, ctx.facultyIdForSubject(it.id)) },
                        if (!ctx.hasActiveGroup) {
                            "Вы пока не состоите в активной учебной группе. Обратитесь к администратору."
                        } else {
                            "Для вашей группы пока не назначены предметы."
                        },
                    )
                }
            }
            _state.value = LoadState.Ready(data)
            _refreshing.value = false
        }
    }
}

@Composable
fun SubjectsScreen(role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<SubjectsViewModel> { parametersOf(role) }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val title = when (role) {
        WorkspaceRole.TEACHER -> "Мои предметы"
        WorkspaceRole.ADMIN -> "Доступные предметы"
        WorkspaceRole.STUDENT -> "Предметы"
    }
    AppScreen(
        title = title,
        actions = { IconButton(onClick = { vm.load(force = true) }) { Icon(Icons.Default.Refresh, "Обновить") } },
    ) { padding ->
        LoadStateContent(state, onRetry = { vm.load() }, refreshing = refreshing, onRefresh = { vm.load(true) }) { data ->
            val filtered = data.items.filter {
                query.isBlank() || it.name.contains(query.trim(), true) || it.description?.contains(query.trim(), true) == true
            }
            ScreenList(contentPadding = screenPadding(padding)) {
                if (data.items.size > 4) item { SearchField(query, { query = it }, placeholder = "Поиск предмета") }
                if (data.items.isEmpty()) {
                    item { EmptyState("Предметов нет", message = data.emptyReason, icon = Icons.AutoMirrored.Filled.MenuBook) }
                } else if (filtered.isEmpty()) {
                    item { EmptyState("Ничего не найдено", message = "Измените поисковый запрос") }
                }
                items(filtered, key = { it.id }) { subject ->
                    NavCard(
                        title = subject.name,
                        description = subject.description,
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = { navigator.open(SubjectDetailsRoute(subject.id, subject.facultyId)) },
                    )
                }
            }
        }
    }
}

// endregion

// region Subject details (views/subjects/SubjectDetailsView.vue)

class SubjectDetailsViewModel(
    private val subjectId: Int,
    private val role: WorkspaceRole,
    private val learning: LearningApi,
    private val subjects: SubjectsApi,
) : BaseViewModel() {
    private val _state = MutableStateFlow<LoadState<PublicSubjectDto>>(LoadState.Loading)
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = LoadState.Loading
        launchSafe(onError = { _state.value = LoadState.Error(it.userMessage()) }) {
            _state.value = LoadState.Ready(
                if (role == WorkspaceRole.STUDENT) {
                    learning.getSubject(subjectId)
                } else {
                    subjects.getById(subjectId).let { PublicSubjectDto(it.id, it.name, it.description) }
                }
            )
        }
    }
}

@Composable
fun SubjectDetailsScreen(route: SubjectDetailsRoute, role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<SubjectDetailsViewModel>(key = "subject-${route.subjectId}") {
        parametersOf(route.subjectId, role)
    }
    val state by vm.state.collectAsState()
    val title = (state as? LoadState.Ready)?.data?.name ?: "Предмет"
    AppScreen(title = title, onBack = navigator::back) { padding ->
        LoadStateContent(state, onRetry = vm::load) { subject ->
            ScreenList(contentPadding = screenPadding(padding)) {
                item {
                    AppCard {
                        Text(subject.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            subject.description?.takeIf { it.isNotBlank() } ?: "Описание не указано.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item { SectionTitle("Разделы") }
                if (role == WorkspaceRole.STUDENT) {
                    item {
                        NavCard(
                            "Лекции",
                            description = "Материалы лекций и тесты",
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                        ) { navigator.open(SubjectLecturesRoute(route.subjectId, route.facultyId)) }
                    }
                } else {
                    item {
                        NavCard(
                            "Темы предмета",
                            description = "Темы, вопросы и тесты по предмету",
                            icon = Icons.Default.Topic,
                        ) { navigator.open(TeacherTopicsRoute(route.subjectId)) }
                    }
                }
            }
        }
    }
}

// endregion

// region Subject lectures (views/lectures/SubjectLecturesView.vue)

data class SubjectLecturesData(val subject: PublicSubjectDto, val lectures: List<PublicLectureDto>)

class SubjectLecturesViewModel(
    private val subjectId: Int,
    private val learning: LearningApi,
) : BaseViewModel() {
    private val _state = MutableStateFlow<LoadState<SubjectLecturesData>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    init {
        load()
    }

    fun load(refresh: Boolean = false) {
        if (refresh) _refreshing.value = true else _state.value = LoadState.Loading
        launchSafe(onError = {
            _refreshing.value = false
            _state.value = LoadState.Error(it.userMessage())
        }) {
            val subject = learning.getSubject(subjectId)
            val lectures = learning.getSubjectLectures(subjectId).sortedWith(compareBy({ it.ordinal }, { it.id }))
            _state.value = LoadState.Ready(SubjectLecturesData(subject, lectures))
            _refreshing.value = false
        }
    }
}

@Composable
fun SubjectLecturesScreen(route: SubjectLecturesRoute, navigator: AppNavigator) {
    val vm = koinViewModel<SubjectLecturesViewModel>(key = "lectures-${route.subjectId}") { parametersOf(route.subjectId) }
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    AppScreen(
        title = "Лекции",
        subtitle = (state as? LoadState.Ready)?.data?.subject?.name,
        onBack = navigator::back,
    ) { padding ->
        LoadStateContent(state, onRetry = { vm.load() }, refreshing = refreshing, onRefresh = { vm.load(true) }) { data ->
            ScreenList(contentPadding = screenPadding(padding)) {
                if (data.lectures.isEmpty()) {
                    item { EmptyState("Лекций пока нет", message = "Преподаватель ещё не опубликовал лекции по предмету.") }
                }
                items(data.lectures, key = { it.id }) { lecture ->
                    NavCard(
                        title = "${lecture.ordinal}. ${lecture.title}",
                        description = lecture.description,
                        icon = Icons.Default.Description,
                        onClick = { navigator.open(LectureRoute(lecture.id, route.subjectId)) },
                    )
                }
            }
        }
    }
}

// endregion

// region Lecture details (views/lectures/LectureDetailsView.vue)

data class LectureData(
    val lecture: PublicLectureDto,
    val materials: List<PublicMaterialDto>,
    val tests: List<PublicTestDto>,
    val materialsError: String? = null,
    val testsError: String? = null,
    /** Материалы, сохранённые на устройстве (открываются без сети). */
    val savedMaterialIds: Set<Int> = emptySet(),
)

class LectureViewModel(
    private val lectureId: Int,
    private val learning: LearningApi,
    private val offlineFiles: OfflineFiles,
) : BaseViewModel() {
    private val _state = MutableStateFlow<LoadState<LectureData>>(LoadState.Loading)
    val state = _state.asStateFlow()
    /** Скачиваемые материалы: id → доля 0..1 (null — размер неизвестен). */
    private val _downloading = MutableStateFlow<Map<Int, Float?>>(emptyMap())
    val downloading = _downloading.asStateFlow()
    private val _refreshing = MutableStateFlow(false)
    val refreshing = _refreshing.asStateFlow()

    init {
        load()
    }

    fun load(refresh: Boolean = false) {
        if (refresh) _refreshing.value = true else _state.value = LoadState.Loading
        launchSafe(onError = {
            _refreshing.value = false
            _state.value = LoadState.Error(it.userMessage())
        }) {
            val lecture = learning.getLecture(lectureId)
            val materials = runCatching { learning.getLectureMaterials(lectureId) }
            val tests = runCatching { learning.getLectureTests(lectureId) }
            _state.value = LoadState.Ready(
                LectureData(
                    lecture = lecture,
                    materials = materials.getOrDefault(emptyList()),
                    tests = tests.getOrDefault(emptyList()),
                    materialsError = materials.exceptionOrNull()?.userMessage(),
                    testsError = tests.exceptionOrNull()?.userMessage(),
                    savedMaterialIds = offlineFiles.savedMaterialIds(lectureId),
                )
            )
            _refreshing.value = false
        }
    }

    /**
     * Скачивает материал и сохраняет его для просмотра без сети.
     * Если связи нет, открывает ранее сохранённую копию.
     */
    fun download(material: PublicMaterialDto) {
        if (material.id in _downloading.value) return
        _downloading.update { it + (material.id to null) }
        launchSafe(onError = { error ->
            _downloading.update { map -> map - material.id }
            toast("Не удалось скачать файл: ${error.userMessage()}")
        }) {
            val file = try {
                offlineFiles.save(lectureId, material.id) { open ->
                    learning.downloadMaterialTo(lectureId, material.id, material.fileName, open) { progress ->
                        _downloading.update { map -> if (material.id in map) map + (material.id to progress) else map }
                    }
                }.also { markSaved(material.id) }
            } catch (e: ApiException) {
                if (!e.isConnectivityProblem) throw e
                val saved = offlineFiles.find(lectureId, material.id) ?: throw e
                toast("Нет связи — открыта сохранённая копия")
                saved
            }
            _downloading.update { it - material.id }
            runCatching { openWithDefaultApp(file) }
                .onFailure { toast("Файл сохранён, но нет приложения для его открытия") }
        }
    }

    private fun markSaved(materialId: Int) {
        _state.update { current ->
            if (current is LoadState.Ready) {
                LoadState.Ready(current.data.copy(savedMaterialIds = current.data.savedMaterialIds + materialId))
            } else {
                current
            }
        }
    }
}

@Composable
fun LectureScreen(route: LectureRoute, navigator: AppNavigator) {
    val vm = koinViewModel<LectureViewModel>(key = "lecture-${route.lectureId}") { parametersOf(route.lectureId) }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val downloading by vm.downloading.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val lecture = (state as? LoadState.Ready)?.data?.lecture
    AppScreen(title = lecture?.title ?: "Лекция", subtitle = lecture?.courseName, onBack = navigator::back) { padding ->
        LoadStateContent(state, onRetry = { vm.load() }, refreshing = refreshing, onRefresh = { vm.load(true) }) { data ->
            ScreenList(contentPadding = screenPadding(padding)) {
                item {
                    AppCard {
                        Text(data.lecture.title, style = MaterialTheme.typography.titleLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusChip("Лекция ${data.lecture.ordinal}", Tone.PRIMARY)
                            if (data.lecture.versionNumber > 0) StatusChip("Версия ${data.lecture.versionNumber}")
                        }
                        Spacer(Modifier.size(4.dp))
                        val description = data.lecture.description?.takeIf { it.isNotBlank() }
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                description ?: "Описание лекции не указано.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (description != null) {
                                SpeakButton(key = "lecture-${data.lecture.id}", text = { "${data.lecture.title}. $description" })
                            }
                        }
                    }
                }

                item { SectionTitle("Материалы") }
                if (data.materialsError != null) item { Banner(data.materialsError, Tone.DANGER) }
                if (data.materials.isEmpty() && data.materialsError == null) {
                    item { Text("К лекции не приложены материалы.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(data.materials, key = { "m${it.id}" }) { material ->
                    MaterialRow(
                        name = material.fileName,
                        size = material.sizeBytes,
                        busy = material.id in downloading,
                        progress = downloading[material.id],
                        savedOffline = material.id in data.savedMaterialIds,
                        onClick = { vm.download(material) },
                    )
                }

                item { SectionTitle("Тесты") }
                if (data.testsError != null) item { Banner(data.testsError, Tone.DANGER) }
                if (data.tests.isEmpty() && data.testsError == null) {
                    item { Text("К лекции не привязаны тесты.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(data.tests, key = { "t${it.id}-${it.assignmentId}" }) { test ->
                    TestCard(test) {
                        val assignmentId = test.assignmentId ?: return@TestCard
                        navigator.open(TestRoute(test.id, assignmentId, route.lectureId, route.subjectId ?: data.lecture.subjectId))
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialRow(
    name: String,
    size: Long,
    busy: Boolean,
    onClick: () -> Unit,
    savedOffline: Boolean = false,
    /** Доля скачанного при [busy]; null — размер неизвестен. */
    progress: Float? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    formatFileSize(size) + when {
                        busy && progress != null -> " · скачивание ${(progress * 100).toInt()} %"
                        busy -> " · скачивание…"
                        savedOffline -> " · сохранён на устройстве"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (busy) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    if (savedOffline) Icons.Default.DownloadDone else Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun TestCard(test: PublicTestDto, onStart: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(test.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
        if (!test.description.isNullOrBlank()) {
            Text(test.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusChip("Вопросов: ${test.questionCount}")
            formatDuration(test.duration)?.let { StatusChip(it) }
            if (test.attemptsAllowed > 0) {
                StatusChip(
                    "Попыток осталось: ${test.attemptsRemaining} из ${test.attemptsAllowed}",
                    if (test.attemptsRemaining > 0) Tone.INFO else Tone.WARNING,
                )
            }
            if (test.canResume) StatusChip("Есть незавершённая попытка", Tone.WARNING)
        }
        if (!test.available && !test.statusMessage.isNullOrBlank()) {
            Banner(test.statusMessage, Tone.WARNING)
        }
        val canStart = test.assignmentId != null && (test.canResume || test.available)
        if (canStart) {
            BusyButton(
                text = if (test.canResume) "Продолжить тест" else "Пройти тест",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// endregion
