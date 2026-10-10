package org.santayn.testing.mobile.feature.teacher

import org.santayn.testing.mobile.core.ui.ReloadOnReconnect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.santayn.testing.mobile.core.files.openWithDefaultApp
import org.santayn.testing.mobile.core.files.saveToCache
import org.santayn.testing.mobile.core.files.toUploadFile
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.session.WorkspaceRole
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.ConfirmDialog
import org.santayn.testing.mobile.core.ui.EditorDialog
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.MultiSelectField
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.SearchField
import org.santayn.testing.mobile.core.ui.SectionTitle
import org.santayn.testing.mobile.core.ui.SelectOption
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.SwitchRow
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.data.api.LecturesApi
import org.santayn.testing.mobile.data.api.TestsApi
import org.santayn.testing.mobile.data.model.LectureDto
import org.santayn.testing.mobile.data.model.LectureMaterialDto
import org.santayn.testing.mobile.data.model.LectureRequest
import org.santayn.testing.mobile.data.model.TestDto
import org.santayn.testing.mobile.domain.ContextCache
import org.santayn.testing.mobile.feature.learning.MaterialRow
import org.santayn.testing.mobile.navigation.AppNavigator
import org.santayn.testing.mobile.navigation.TeacherLecturesRoute

/*
 * Лекции преподавателя — views/teacher/LectureManagementView.vue, components/teacher/LectureEditorDrawer.vue,
 * composables/lectures.
 */

data class LectureForm(
    val id: Int? = null,
    val title: String = "",
    val description: String = "",
    val publicVisible: Boolean = false,
    val testIds: Set<Int> = emptySet(),
    val pendingFiles: List<PlatformFile> = emptyList(),
)

data class LecturesUiState(
    val loading: Boolean = false,
    val lectures: List<LectureDto> = emptyList(),
    val linkedTests: Map<Int, List<TestDto>> = emptyMap(),
    val subjectTests: List<TestDto> = emptyList(),
    val error: String? = null,
    val form: LectureForm? = null,
    val formError: String? = null,
    val saving: Boolean = false,
    val materials: List<LectureMaterialDto> = emptyList(),
    val materialBusy: Set<Int> = emptySet(),
    val deletingMaterial: LectureMaterialDto? = null,
    val deletingLecture: LectureDto? = null,
    val deleteBusy: Boolean = false,
)

enum class PublishFilter(val label: String) { ALL("Все"), PUBLISHED("Опубликованные"), HIDDEN("Скрытые") }

class LectureManagementViewModel(
    private val route: TeacherLecturesRoute,
    val subjects: TeacherSubjectsHolder,
    private val lecturesApi: LecturesApi,
    private val testsApi: TestsApi,
    private val cache: ContextCache,
) : BaseViewModel() {
    private val _state = MutableStateFlow(LecturesUiState())
    val state = _state.asStateFlow()

    init {
        launchSafe {
            subjects.load(preferredSubjectId = route.subjectId)
            load()
        }
    }

    fun selectMembership(id: Int?) {
        subjects.select(id)
        launchSafe { load() }
    }

    fun refresh() = launchSafe { load() }

    /** Связь восстановилась: догружаем то, что не загрузилось, и обновляем списки. */
    fun reloadAfterReconnect() = launchSafe {
        if (subjects.needsLoad) subjects.load(preferredSubjectId = route.subjectId)
        load()
    }

    private suspend fun load() = coroutineScope {
        val picker = subjects.state.value
        val membershipId = picker.selectedMembershipId
        if (membershipId == null) {
            _state.update { it.copy(lectures = emptyList(), linkedTests = emptyMap()) }
            return@coroutineScope
        }
        _state.update { it.copy(loading = true, error = null) }
        try {
            val lectures = lecturesApi.getAll(subjectMembershipId = membershipId).sortedWith(compareBy({ it.ordinal }, { it.id }))
            val linked = lectures.map { l -> async { l.id to runCatching { lecturesApi.getTests(l.id) }.getOrDefault(emptyList()) } }
                .awaitAll().toMap()
            val tests = runCatching { testsApi.getAll(subjectId = picker.selectedSubjectId) }.getOrDefault(emptyList())
            _state.update { it.copy(loading = false, lectures = lectures, linkedTests = linked, subjectTests = tests) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.userMessage()) }
        }
    }

    fun openCreate() = _state.update { it.copy(form = LectureForm(), formError = null, materials = emptyList()) }

    fun openEdit(lecture: LectureDto) {
        val testIds = _state.value.linkedTests[lecture.id].orEmpty().map { it.id }.toSet()
        _state.update {
            it.copy(
                form = LectureForm(lecture.id, lecture.title, lecture.description.orEmpty(), lecture.publicVisible, testIds),
                formError = null,
                materials = emptyList(),
            )
        }
        loadMaterials(lecture.id)
    }

    private fun loadMaterials(lectureId: Int) = launchSafe {
        val materials = lecturesApi.getMaterials(lectureId)
        _state.update { if (it.form?.id == lectureId) it.copy(materials = materials) else it }
    }

    fun updateForm(transform: (LectureForm) -> LectureForm) = _state.update { s -> s.copy(form = s.form?.let(transform)) }

    fun closeForm() = _state.update { it.copy(form = null, formError = null) }

    fun addFiles(files: List<PlatformFile>?) {
        if (files.isNullOrEmpty()) return
        updateForm { it.copy(pendingFiles = (it.pendingFiles + files).distinctBy { f -> f.name }) }
    }

    private fun slug(value: String): String =
        value.trim().lowercase().replace(Regex("[^a-zа-яё0-9]+"), "-").trim('-').take(80).ifBlank { "lecture" }

    fun save() {
        val form = _state.value.form ?: return
        if (form.title.isBlank()) {
            _state.update { it.copy(formError = "Введите название лекции.") }
            return
        }
        _state.update { it.copy(saving = true, formError = null) }
        launchSafe(onError = { e -> _state.update { it.copy(saving = false, formError = e.userMessage("Не удалось сохранить лекцию")) } }) {
            val membership = subjects.ensureSelectedActive()
            val existing = _state.value.lectures.firstOrNull { it.id == form.id }
            val ordinal = existing?.ordinal ?: ((_state.value.lectures.maxOfOrNull { it.ordinal } ?: 0) + 1)
            val request = LectureRequest(
                subjectId = membership.subjectId,
                subjectMembershipId = membership.membershipId,
                courseVersionId = null,
                ordinal = ordinal,
                title = form.title.trim(),
                description = form.description.trim().ifBlank { null },
                contentFolderKey = existing?.contentFolderKey ?: "lecture-${form.id ?: nowKey()}-${slug(form.title)}",
                linkedTestId = null,
                publicVisible = form.publicVisible,
            )
            val lecture = if (form.id == null) lecturesApi.create(request) else lecturesApi.update(form.id, request)
            // Лекция сохранена: дальше — связи с тестами и файлы (частичный успех показываем отдельно).
            _state.update { it.copy(form = it.form?.copy(id = lecture.id)) }
            lecturesApi.setTests(lecture.id, form.testIds.toList())
            if (form.pendingFiles.isNotEmpty()) {
                val uploads = form.pendingFiles.map { it.toUploadFile() }
                lecturesApi.uploadMaterials(lecture.id, uploads)
            }
            cache.invalidate()
            _state.update { it.copy(saving = false, form = null) }
            toast(if (form.id == null) "Лекция создана" else "Лекция сохранена")
            load()
        }
    }

    private fun nowKey(): Long = org.santayn.testing.mobile.core.util.nowInstant().toEpochMilliseconds()

    fun downloadMaterial(material: LectureMaterialDto) {
        val lectureId = _state.value.form?.id ?: return
        _state.update { it.copy(materialBusy = it.materialBusy + material.id) }
        launchSafe(onError = { e ->
            _state.update { it.copy(materialBusy = it.materialBusy - material.id) }
            toast(e.userMessage("Не удалось скачать файл"))
        }) {
            val file = lecturesApi.downloadMaterial(lectureId, material.id, material.fileName).saveToCache()
            _state.update { it.copy(materialBusy = it.materialBusy - material.id) }
            runCatching { openWithDefaultApp(file) }.onFailure { toast("Нет приложения для открытия файла") }
        }
    }

    fun askDeleteMaterial(material: LectureMaterialDto?) = _state.update { it.copy(deletingMaterial = material) }

    fun confirmDeleteMaterial() {
        val lectureId = _state.value.form?.id ?: return
        val material = _state.value.deletingMaterial ?: return
        _state.update { it.copy(deleteBusy = true) }
        launchSafe(onError = { e ->
            _state.update { it.copy(deleteBusy = false, deletingMaterial = null) }
            toast(e.userMessage("Не удалось удалить файл"))
        }) {
            lecturesApi.removeMaterial(lectureId, material.id)
            _state.update { s -> s.copy(deleteBusy = false, deletingMaterial = null, materials = s.materials.filter { it.id != material.id }) }
            toast("Файл удалён")
        }
    }

    fun askDeleteLecture(lecture: LectureDto?) = _state.update { it.copy(deletingLecture = lecture) }

    fun confirmDeleteLecture() {
        val lecture = _state.value.deletingLecture ?: return
        _state.update { it.copy(deleteBusy = true) }
        launchSafe(onError = { e ->
            _state.update { it.copy(deleteBusy = false, deletingLecture = null) }
            toast(e.userMessage("Не удалось удалить лекцию"))
        }) {
            lecturesApi.remove(lecture.id)
            cache.invalidate()
            _state.update { it.copy(deleteBusy = false, deletingLecture = null) }
            toast("Лекция удалена")
            load()
        }
    }
}

@Composable
fun LectureManagementScreen(route: TeacherLecturesRoute, role: WorkspaceRole, navigator: AppNavigator) {
    val vm = koinViewModel<LectureManagementViewModel>(key = "lectures-mgmt-${route.subjectId}") { parametersOf(route, role) }
    ReloadOnReconnect { vm.reloadAfterReconnect() }
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val picker by vm.subjects.state.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var publish by rememberSaveable { mutableStateOf(PublishFilter.ALL) }
    var withTestsOnly by rememberSaveable { mutableStateOf(false) }

    AppScreen(
        title = "Лекции",
        onBack = navigator::back,
        floatingActionButton = {
            if (picker.selected != null) {
                ExtendedFloatingActionButton(onClick = vm::openCreate, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Лекция") })
            }
        },
    ) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item { TeacherSubjectSelector(picker, vm::selectMembership, vm.subjects.isAdmin) }
            if (picker.selected != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchField(query, { query = it }, placeholder = "Поиск лекции")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PublishFilter.entries.forEach { f -> FilterChip(publish == f, { publish = f }, { Text(f.label) }) }
                            FilterChip(withTestsOnly, { withTestsOnly = !withTestsOnly }, { Text("С тестами") })
                        }
                    }
                }
            }
            when {
                state.loading -> item { LoadingState() }
                state.error != null -> item { Banner(state.error!!, Tone.DANGER) }
                picker.selected != null && state.lectures.isEmpty() -> item {
                    EmptyState("Лекций пока нет", message = "Создайте первую лекцию предмета.", icon = Icons.Default.LibraryBooks)
                }
            }
            val filtered = state.lectures.filter { l ->
                (query.isBlank() || l.title.contains(query.trim(), true) || l.description?.contains(query.trim(), true) == true) &&
                    when (publish) {
                        PublishFilter.ALL -> true
                        PublishFilter.PUBLISHED -> l.publicVisible
                        PublishFilter.HIDDEN -> !l.publicVisible
                    } &&
                    (!withTestsOnly || state.linkedTests[l.id].orEmpty().isNotEmpty())
            }
            items(filtered, key = { it.id }) { lecture ->
                val tests = state.linkedTests[lecture.id].orEmpty()
                AppCard(onClick = { vm.openEdit(lecture) }) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusChip("№${lecture.ordinal}", Tone.PRIMARY)
                        StatusChip(if (lecture.publicVisible) "Опубликована" else "Скрыта", if (lecture.publicVisible) Tone.SUCCESS else Tone.WARNING)
                        StatusChip("Тестов: ${tests.size}")
                    }
                    Text(lecture.title, style = MaterialTheme.typography.titleMedium)
                    lecture.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    if (tests.isNotEmpty()) {
                        Text("Тесты: " + tests.joinToString(", ") { it.title }, style = MaterialTheme.typography.bodySmall)
                    }
                    Row {
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { vm.askDeleteLecture(lecture) }) { Icon(Icons.Default.Delete, "Удалить лекцию") }
                    }
                }
            }
        }
    }

    state.form?.let { form -> LectureEditor(form, state, vm) }

    state.deletingLecture?.let { lecture ->
        ConfirmDialog(
            title = "Удалить лекцию?",
            message = "Лекция «${lecture.title}» и её материалы будут удалены.",
            confirmText = "Удалить",
            destructive = true,
            busy = state.deleteBusy,
            onConfirm = vm::confirmDeleteLecture,
            onDismiss = { vm.askDeleteLecture(null) },
        )
    }
}

@Composable
private fun LectureEditor(form: LectureForm, state: LecturesUiState, vm: LectureManagementViewModel) {
    val picker = rememberFilePickerLauncher(type = FileKitType.File(), mode = FileKitMode.Multiple()) { files -> vm.addFiles(files) }
    EditorDialog(
        title = if (form.id == null) "Новая лекция" else "Редактирование лекции",
        onDismiss = vm::closeForm,
        onSave = vm::save,
        busy = state.saving,
        error = state.formError,
    ) {
        FormField(form.title, { v -> vm.updateForm { it.copy(title = v) } }, "Название", required = true, maxLength = 200)
        FormField(form.description, { v -> vm.updateForm { it.copy(description = v) } }, "Описание",
            singleLine = false, minLines = 4, maxLength = 2000)
        SwitchRow("Опубликовать для студентов", form.publicVisible, { v -> vm.updateForm { it.copy(publicVisible = v) } })
        MultiSelectField(
            label = "Тесты лекции",
            options = state.subjectTests.map { SelectOption(it.id, it.title, "Вопросов: ${it.questionCount}") },
            selected = form.testIds,
            onChange = { ids -> vm.updateForm { it.copy(testIds = ids) } },
            placeholder = if (state.subjectTests.isEmpty()) "У предмета нет тестов" else "Не выбраны",
        )

        SectionTitle("Материалы")
        if (form.id != null && state.materials.isEmpty()) {
            Text("Материалы не загружены.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.materials.forEach { material ->
            MaterialRow(
                name = material.fileName,
                size = material.sizeBytes,
                busy = material.id in state.materialBusy,
                onClick = { vm.downloadMaterial(material) },
                trailing = {
                    IconButton(onClick = { vm.askDeleteMaterial(material) }) { Icon(Icons.Default.Delete, "Удалить файл") }
                },
            )
        }
        form.pendingFiles.forEach { file ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name)
                        Text("Будет загружен при сохранении", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { vm.updateForm { it.copy(pendingFiles = it.pendingFiles - file) } }) {
                        Icon(Icons.Default.Close, "Убрать")
                    }
                }
            }
        }
        OutlinedButton(onClick = { picker.launch() }) {
            Icon(Icons.Default.AttachFile, null)
            Text(" Добавить файлы")
        }
        Text("Максимальный размер файла — 50 МБ.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    state.deletingMaterial?.let { material ->
        ConfirmDialog(
            title = "Удалить файл?",
            message = "Файл «${material.fileName}» будет удалён из лекции.",
            confirmText = "Удалить",
            destructive = true,
            busy = state.deleteBusy,
            onConfirm = vm::confirmDeleteMaterial,
            onDismiss = { vm.askDeleteMaterial(null) },
        )
    }
}
