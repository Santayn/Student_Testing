package org.santayn.testing.mobile.feature.teacher

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.compose.viewmodel.koinViewModel
import org.santayn.testing.mobile.core.network.userMessage
import org.santayn.testing.mobile.core.ui.AppButton
import org.santayn.testing.mobile.core.ui.AppCard
import org.santayn.testing.mobile.core.ui.AppFilterChip
import org.santayn.testing.mobile.core.ui.AppOutlinedButton
import org.santayn.testing.mobile.core.ui.AppScreen
import org.santayn.testing.mobile.core.ui.Banner
import org.santayn.testing.mobile.core.ui.BaseViewModel
import org.santayn.testing.mobile.core.ui.CollectMessages
import org.santayn.testing.mobile.core.ui.EmptyState
import org.santayn.testing.mobile.core.ui.FormField
import org.santayn.testing.mobile.core.ui.LoadingState
import org.santayn.testing.mobile.core.ui.ScreenList
import org.santayn.testing.mobile.core.ui.StatusChip
import org.santayn.testing.mobile.core.ui.Tone
import org.santayn.testing.mobile.core.ui.screenPadding
import org.santayn.testing.mobile.core.util.formatNumber
import org.santayn.testing.mobile.data.api.ResultsApi
import org.santayn.testing.mobile.data.model.QuestionType
import org.santayn.testing.mobile.data.model.ReviewItemDto
import org.santayn.testing.mobile.data.model.ReviewStatus
import org.santayn.testing.mobile.domain.AiFeatures
import org.santayn.testing.mobile.navigation.AppNavigator

/*
 * Проверка ответов — views/results/ReviewQueueView.vue: свободные ответы, в оценке которых ИИ
 * не уверен или которые он не смог проверить; преподаватель ставит баллы или подтверждает оценку.
 */

data class ReviewUiState(
    val status: String = ReviewStatus.NEEDS_TEACHER,
    val loading: Boolean = false,
    val items: List<ReviewItemDto> = emptyList(),
    val counts: Map<String, Long> = emptyMap(),
    val error: String? = null,
    val busyResponseId: Long? = null,
)

class ReviewViewModel(private val results: ResultsApi, val ai: AiFeatures) : BaseViewModel() {
    private val _state = MutableStateFlow(ReviewUiState())
    val state = _state.asStateFlow()

    init {
        ai.ensureLoaded()
        load()
    }

    fun selectStatus(status: String) {
        _state.update { it.copy(status = status) }
        load()
    }

    fun load() = launchSafe(onError = { e -> _state.update { it.copy(loading = false, error = e.userMessage()) } }) {
        _state.update { it.copy(loading = true, error = null) }
        val queue = results.getReviewQueue(_state.value.status)
        _state.update { it.copy(loading = false, items = queue.items, counts = queue.counts) }
    }

    fun grade(item: ReviewItemDto, points: String, comment: String) {
        val value = points.replace(',', '.').toDoubleOrNull()
        val max = item.questionPoints ?: 0.0
        if (value == null || value < 0 || value > max) {
            toast("Баллы должны быть от 0 до ${formatNumber(max)}")
            return
        }
        act(item, "Оценка сохранена") { results.gradeResponse(item.responseId, value, comment.trim().ifBlank { null }) }
    }

    fun accept(item: ReviewItemDto) = act(item, "Оценка подтверждена") { results.acceptResponse(item.responseId) }

    fun regrade(item: ReviewItemDto) = act(item, "Ответ отправлен ИИ на повторную проверку") {
        results.regradeResponse(item.responseId)
    }

    private fun act(item: ReviewItemDto, success: String, block: suspend () -> Unit) {
        if (_state.value.busyResponseId != null) return
        _state.update { it.copy(busyResponseId = item.responseId) }
        launchSafe(onError = { e ->
            _state.update { it.copy(busyResponseId = null) }
            toast(e.userMessage("Не удалось сохранить проверку"))
        }) {
            block()
            _state.update { it.copy(busyResponseId = null) }
            toast(success)
            load()
        }
    }
}

private val statusFilters = listOf(
    ReviewStatus.NEEDS_TEACHER,
    ReviewStatus.AI_PENDING,
    ReviewStatus.AI_GRADED,
    ReviewStatus.TEACHER_CONFIRMED,
)

@Composable
fun ReviewScreen(navigator: AppNavigator) {
    val vm = koinViewModel<ReviewViewModel>()
    CollectMessages(vm)
    val state by vm.state.collectAsState()
    val aiStatus by vm.ai.status.collectAsState()

    AppScreen(
        title = "Проверка ответов",
        onBack = navigator::back,
        actions = { IconButton(onClick = vm::load) { Icon(Icons.Default.Refresh, "Обновить") } },
    ) { padding ->
        ScreenList(contentPadding = screenPadding(padding)) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    statusFilters.forEach { status ->
                        val count = state.counts[status] ?: 0
                        AppFilterChip(
                            selected = state.status == status,
                            onClick = { vm.selectStatus(status) },
                            label = "${ReviewStatus.title(status)}: $count",
                        )
                    }
                }
            }
            if (aiStatus != null && aiStatus?.grading == false) {
                item {
                    Banner("Проверка ИИ выключена: здесь ответы, засчитанные по похожести на эталон.", Tone.INFO)
                }
            }
            state.error?.let { item { Banner(it, Tone.DANGER) } }
            when {
                state.loading && state.items.isEmpty() -> item { LoadingState() }
                state.items.isEmpty() -> item {
                    EmptyState(
                        if (state.status == ReviewStatus.NEEDS_TEACHER) "Все ответы проверены" else "Здесь пока пусто",
                    )
                }
                else -> items(state.items, key = { it.responseId }) { item ->
                    ReviewCard(
                        item = item,
                        busy = state.busyResponseId == item.responseId,
                        aiAvailable = aiStatus?.grading == true,
                        vm = vm,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(item: ReviewItemDto, busy: Boolean, aiAvailable: Boolean, vm: ReviewViewModel) {
    var points by rememberSaveable(item.responseId, item.awardedPoints) { mutableStateOf(formatNumber(item.awardedPoints ?: 0.0)) }
    var comment by rememberSaveable(item.responseId) { mutableStateOf(item.teacherComment.orEmpty()) }
    AppCard {
        Text(item.studentName ?: "Студент", style = MaterialTheme.typography.titleMedium)
        Text(item.testName.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            StatusChip(
                ReviewStatus.title(item.reviewStatus),
                if (item.reviewStatus == ReviewStatus.NEEDS_TEACHER) Tone.WARNING else Tone.INFO,
            )
            StatusChip("${formatNumber(item.awardedPoints ?: 0.0)} из ${formatNumber(item.questionPoints ?: 0.0)}")
            if (item.suspicious) StatusChip("Похоже на попытку обмануть ИИ", Tone.DANGER)
        }
        Labeled("Вопрос", item.questionText.orEmpty())
        Labeled("Ответ студента", item.answerText ?: "—")
        Labeled("Эталон", item.correctAnswer ?: "—")
        item.gradingCriteria?.takeIf { it.isNotBlank() }?.let { Labeled("Критерии", it) }
        if (item.aiFeedback != null || item.matched.isNotEmpty() || item.missing.isNotEmpty()) {
            val confidence = item.aiConfidence?.let { " · уверенность ${(it * 100).toInt()}%" }.orEmpty()
            val details = buildList {
                item.aiFeedback?.let { add(it) }
                item.matched.forEach { add("✓ $it") }
                item.missing.forEach { add("✗ $it") }
            }.joinToString("\n")
            Banner(details, Tone.PRIMARY, title = "Оценка ИИ$confidence")
        }
        FormField(points, { points = it }, "Баллы", keyboardType = KeyboardType.Decimal, enabled = !busy)
        FormField(comment, { comment = it }, "Комментарий студенту", singleLine = false, minLines = 2, maxLength = 2000, enabled = !busy)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton("Сохранить оценку", onClick = { vm.grade(item, points, comment) }, enabled = !busy)
            if (item.reviewStatus == ReviewStatus.NEEDS_TEACHER || item.reviewStatus == ReviewStatus.AI_GRADED) {
                AppOutlinedButton("Подтвердить как есть", onClick = { vm.accept(item) }, enabled = !busy)
            }
            if (aiAvailable && item.questionType == QuestionType.TEXT && item.reviewStatus != ReviewStatus.AI_PENDING) {
                AppOutlinedButton("Перепроверить ИИ", onClick = { vm.regrade(item) }, enabled = !busy, icon = Icons.Default.AutoAwesome)
            }
        }
    }
}

@Composable
private fun Labeled(label: String, text: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
