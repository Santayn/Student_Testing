package org.santayn.testing.mobile.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.santayn.testing.mobile.domain.MdBlock
import org.santayn.testing.mobile.domain.MdInline
import org.santayn.testing.mobile.domain.parseMarkdown

/** Текст лекции в Markdown: заголовки размечены для TalkBack/VoiceOver, ссылки открываются в браузере. */
@Composable
fun MarkdownView(source: String, modifier: Modifier = Modifier) {
    val blocks = remember(source) { parseMarkdown(source) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        blocks.forEach { block -> MarkdownBlock(block) }
    }
}

@Composable
private fun MarkdownBlock(block: MdBlock) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    when (block) {
        is MdBlock.Heading -> Text(
            inlineText(block.inlines),
            style = when (block.level) {
                1, 2 -> typography.titleLarge
                3 -> typography.titleMedium
                else -> typography.titleSmall
            },
            modifier = Modifier.semantics { heading() },
        )
        is MdBlock.Paragraph -> Text(inlineText(block.inlines), style = typography.bodyLarge)
        is MdBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            block.items.forEachIndexed { index, item ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (block.ordered) "${index + 1}." else "•",
                        style = typography.bodyLarge,
                        color = colors.primary,
                        modifier = Modifier.widthIn(min = 18.dp),
                    )
                    Text(inlineText(item), style = typography.bodyLarge, modifier = Modifier.weight(1f))
                }
            }
        }
        is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(colors.primary))
            Text(
                inlineText(block.inlines),
                style = typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        is MdBlock.Code -> Text(
            block.text,
            style = typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant, RoundedCornerShape(8.dp))
                .horizontalScroll(rememberScrollState())
                .padding(12.dp),
        )
        is MdBlock.Table -> MarkdownTable(block)
        MdBlock.Rule -> HorizontalDivider()
    }
}

@Composable
private fun MarkdownTable(table: MdBlock.Table) {
    val colors = MaterialTheme.colorScheme
    val columns = (listOf(table.header) + table.rows).maxOf { it.size }.coerceAtLeast(1)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp)),
    ) {
        // Колонки одной ширины: строки выровнены и заполняют рамку; не помещаются — таблица прокручивается.
        val cellWidth = maxOf(maxWidth / columns, MIN_TABLE_CELL_WIDTH)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            (listOf(table.header) + table.rows).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) HorizontalDivider(Modifier.width(cellWidth * columns), color = colors.outlineVariant)
                Row(
                    Modifier.background(if (rowIndex == 0) colors.surfaceVariant else colors.surface),
                ) {
                    repeat(columns) { column ->
                        Text(
                            inlineText(row.getOrElse(column) { emptyList() }),
                            style = if (rowIndex == 0) {
                                MaterialTheme.typography.labelLarge
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            modifier = Modifier.width(cellWidth).padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

private val MIN_TABLE_CELL_WIDTH = 120.dp

@Composable
private fun inlineText(nodes: List<MdInline>): AnnotatedString {
    val colors = MaterialTheme.colorScheme
    return buildAnnotatedString { appendInlines(nodes, colors.primary, colors.surfaceVariant) }
}

private fun AnnotatedString.Builder.appendInlines(
    nodes: List<MdInline>,
    linkColor: androidx.compose.ui.graphics.Color,
    codeBackground: androidx.compose.ui.graphics.Color,
) {
    nodes.forEach { node ->
        when (node) {
            is MdInline.Text -> append(node.text)
            is MdInline.Strong -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                appendInlines(node.children, linkColor, codeBackground)
            }
            is MdInline.Emphasis -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                appendInlines(node.children, linkColor, codeBackground)
            }
            is MdInline.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                append(node.text)
            }
            is MdInline.Link -> withLink(
                LinkAnnotation.Url(
                    node.url,
                    TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                ),
            ) {
                appendInlines(node.children, linkColor, codeBackground)
            }
        }
    }
}
