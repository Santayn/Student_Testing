package org.santayn.testing.mobile.feature.tests

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import org.santayn.testing.mobile.core.ui.AppTheme
import org.santayn.testing.mobile.core.ui.design.Design
import org.santayn.testing.mobile.data.model.PublicQuestionDto
import org.santayn.testing.mobile.domain.AttemptAnswers

/** Перетаскиваемый вариант: текст, откуда взят (подсказка или null — из списка) и где сейчас палец. */
private data class Dragging(val text: String, val from: Int?, val pointer: Offset, val grab: Offset, val size: IntSize)

/**
 * Сопоставление перетаскиванием (как на сайте): сверху — список вариантов, ниже — подсказки со слотами.
 * Вариант перетаскивается в слот, между слотами (занятые меняются местами) и обратно в список.
 *
 * Без перетаскивания (WCAG 2.5.7): нажать вариант, затем слот; крестик в слоте возвращает вариант.
 * TalkBack/VoiceOver: у слота действия «Поставить «…»» и «Убрать», у варианта — «Поставить к «…»».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MatchingBoard(
    question: PublicQuestionDto,
    answers: AttemptAnswers,
    enabled: Boolean,
    onChange: ((AttemptAnswers) -> AttemptAnswers) -> Unit,
) {
    val prompts = question.matchingPrompts.sortedBy { it.ordinal }
    val selections = answers.matching[question.id].orEmpty()
    val pool = question.matchingOptions.filter { it !in selections }

    var picked by remember(question.id) { mutableStateOf<String?>(null) }
    var dragging by remember(question.id) { mutableStateOf<Dragging?>(null) }
    val slotBounds = remember(question.id) { mutableStateMapOf<Int, Rect>() }
    var poolBounds by remember(question.id) { mutableStateOf(Rect.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    fun move(text: String, to: Int?) {
        onChange { it.withMatchingMove(question.id, text, to) }
        picked = null
    }

    val hoveredSlot = dragging?.let { d -> slotBounds.entries.firstOrNull { it.value.contains(d.pointer) }?.key }
    val hoveringPool = dragging?.let { d -> d.from != null && poolBounds.contains(d.pointer) } == true

    fun drop() {
        val d = dragging ?: return
        val target = slotBounds.entries.firstOrNull { it.value.contains(d.pointer) }?.key
        when {
            target != null -> move(d.text, target)
            d.from != null && poolBounds.contains(d.pointer) -> move(d.text, null)
        }
        dragging = null
    }

    val dragModifier = { text: String, from: Int? ->
        Modifier.dragSource(
            key = text to from,
            enabled = enabled,
            onStart = { pointer, grab, size -> dragging = Dragging(text, from, pointer, grab, size) },
            onMove = { delta -> dragging = dragging?.let { it.copy(pointer = it.pointer + delta) } },
            onEnd = ::drop,
            onCancel = { dragging = null },
        )
    }

    Box(Modifier.fillMaxWidth().onGloballyPositioned { origin = it.positionInRoot() }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Сопоставлено ${selections.size} из ${prompts.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            // Список вариантов: сюда же можно вернуть вариант из слота.
            val poolHighlight by animateColorAsState(
                if (hoveringPool || (picked != null && picked in selections)) AppTheme.status.primarySoft else Color.Transparent
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(Design.tokens.cardShape).background(poolHighlight)
                    .onGloballyPositioned { poolBounds = it.boundsInRoot() }
                    .then(
                        // Выбранный нажатием вариант из слота возвращается в список нажатием на список.
                        Modifier.tapOnly(picked != null && picked in selections) { picked?.let { move(it, null) } }
                    )
                    .padding(4.dp),
            ) {
                if (pool.isEmpty()) {
                    Text(
                        "Все варианты расставлены",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                pool.forEach { text -> key(text) {
                    MatchChip(
                        text = text,
                        selected = picked == text,
                        ghost = dragging?.text == text,
                        modifier = dragModifier(text, null),
                        semantics = Modifier.semantics {
                            role = Role.Button
                            stateDescription = if (picked == text) "выбран, нажмите на подсказку" else "не расставлен"
                            onClick(label = "выбрать") { picked = if (picked == text) null else text; true }
                            customActions = prompts.map { p ->
                                CustomAccessibilityAction("Поставить к «${p.text}»") { move(text, p.ordinal); true }
                            }
                        },
                        onClick = { if (enabled) picked = if (picked == text) null else text },
                    )
                } }
            }

            prompts.forEach { prompt ->
                val current = selections.entries.firstOrNull { it.value == prompt.ordinal }?.key
                MatchRow(
                    prompt = prompt.text,
                    current = current,
                    hovered = hoveredSlot == prompt.ordinal,
                    awaiting = picked != null && picked != current,
                    enabled = enabled,
                    ghost = current != null && dragging?.text == current,
                    pickedHere = current != null && picked == current,
                    slotModifier = Modifier.onGloballyPositioned { slotBounds[prompt.ordinal] = it.boundsInRoot() },
                    chipModifier = if (current != null) dragModifier(current, prompt.ordinal) else Modifier,
                    onSlotClick = { picked?.let { move(it, prompt.ordinal) } },
                    onChipClick = { if (enabled && current != null) picked = if (picked == current) null else current },
                    onRemove = { if (current != null) move(current, null) },
                    accessibility = Modifier.semantics {
                        contentDescription = "Соответствие для «${prompt.text}»"
                        stateDescription = current ?: "не выбрано"
                        role = Role.Button
                        if (picked != null) onClick(label = "поставить «$picked»") { picked?.let { move(it, prompt.ordinal) }; true }
                        customActions = buildList {
                            question.matchingOptions.filter { it != current }.forEach { option ->
                                add(CustomAccessibilityAction("Поставить «$option»") { move(option, prompt.ordinal); true })
                            }
                            if (current != null) add(CustomAccessibilityAction("Убрать «$current»") { move(current, null); true })
                        }
                    },
                )
            }
        }

        // «Призрак» перетаскиваемого варианта под пальцем.
        dragging?.let { d ->
            val topLeft = d.pointer - d.grab - origin
            val density = LocalDensity.current
            MatchChip(
                text = d.text,
                selected = true,
                ghost = false,
                lifted = true,
                modifier = Modifier.zIndex(10f)
                    .offset { IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt()) }
                    .width(with(density) { d.size.width.toDp() })
                    .clearAndSetSemantics {},
                onClick = {},
            )
        }
    }
}

/** Нажатие без семантики: альтернатива для пальца, у программы чтения с экрана есть свои действия. */
private fun Modifier.tapOnly(enabled: Boolean, onTap: () -> Unit): Modifier = composed {
    if (!enabled) return@composed Modifier
    val tap by rememberUpdatedState(onTap)
    Modifier.pointerInput(Unit) { detectTapGestures(onTap = { tap() }) }
}

/** Перетаскивание с запоминанием точки захвата; координаты — в системе корня окна. */
private fun Modifier.dragSource(
    key: Any,
    enabled: Boolean,
    onStart: (pointer: Offset, grab: Offset, size: IntSize) -> Unit,
    onMove: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
): Modifier = composed {
    if (!enabled) return@composed Modifier
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val start by rememberUpdatedState(onStart)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    val cancel by rememberUpdatedState(onCancel)
    Modifier
        .onGloballyPositioned {
            rootPosition = it.positionInRoot()
            size = it.size
        }
        .pointerInput(key) {
            detectDragGestures(
                onDragStart = { grab -> start(rootPosition + grab, grab, size) },
                onDrag = { change, amount ->
                    change.consume()
                    move(amount)
                },
                onDragEnd = { end() },
                onDragCancel = { cancel() },
            )
        }
}

@Composable
private fun MatchChip(
    text: String,
    selected: Boolean,
    ghost: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    lifted: Boolean = false,
    /** Семантика на нажимаемой части чипа (один узел с текстом для программы чтения с экрана). */
    semantics: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = Design.tokens.chipShape
    Surface(
        shape = shape,
        color = if (selected) AppTheme.status.primarySoft else scheme.surfaceContainerHigh,
        contentColor = if (selected) AppTheme.status.primarySoftText else scheme.onSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = modifier
            .alpha(if (ghost) 0.35f else 1f)
            .then(if (lifted) Modifier.shadow(10.dp, shape) else Modifier),
    ) {
        Row(
            Modifier.clip(shape).clickable(onClick = onClick).then(semantics).heightIn(min = 48.dp).padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.DragIndicator, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MatchRow(
    prompt: String,
    current: String?,
    hovered: Boolean,
    awaiting: Boolean,
    enabled: Boolean,
    ghost: Boolean,
    pickedHere: Boolean,
    slotModifier: Modifier,
    chipModifier: Modifier,
    onSlotClick: () -> Unit,
    onChipClick: () -> Unit,
    onRemove: () -> Unit,
    accessibility: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = Design.tokens
    val slotBorder by animateColorAsState(
        when {
            hovered -> scheme.primary
            awaiting && enabled -> scheme.primary.copy(alpha = 0.6f)
            current != null -> scheme.outlineVariant
            else -> scheme.outline
        }
    )
    val slotFill by animateColorAsState(if (hovered) AppTheme.status.primarySoft else Color.Transparent)
    Surface(
        shape = tokens.cardShape,
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Вся строка — одно поле для программы чтения с экрана (подсказка входит в его описание).
        // Выбранный нажатием вариант ставится нажатием на любую часть строки (только для пальца:
        // программа чтения с экрана ставит его действием строки).
        Row(Modifier.height(IntrinsicSize.Min).tapOnly(enabled && awaiting, onSlotClick).then(accessibility)) {
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(14.dp).clearAndSetSemantics {},
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(prompt, style = MaterialTheme.typography.bodyLarge)
            }
            Box(Modifier.width(1.dp).fillMaxHeight().background(scheme.outlineVariant))
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(8.dp).then(slotModifier)
                    .clip(tokens.tileShape).background(slotFill)
                    .border(if (hovered) 2.dp else 1.dp, slotBorder, tokens.tileShape)
                    .heightIn(min = 56.dp)
                    .padding(4.dp)
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.CenterStart,
            ) {
                if (current == null) {
                    Text(
                        if (awaiting) "Нажмите, чтобы поставить" else "Перетащите вариант сюда",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (awaiting) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MatchChip(
                            text = current,
                            selected = pickedHere,
                            ghost = ghost,
                            onClick = onChipClick,
                            modifier = Modifier.weight(1f, fill = false).then(chipModifier),
                        )
                        if (enabled) {
                            IconButton(onClick = onRemove) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = scheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
