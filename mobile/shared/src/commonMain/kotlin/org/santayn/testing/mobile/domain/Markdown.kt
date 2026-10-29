package org.santayn.testing.mobile.domain

/*
 * Разбор Markdown текста лекции (тот же набор, что у веба: frontend/src/utils/markdown.js):
 * заголовки #–####, абзацы, списки, цитаты, код, таблицы, линия, **жирный**, *курсив*, `код`,
 * ссылки http(s). HTML не поддерживается — текст отрисовывается своими компонентами.
 */

sealed interface MdInline {
    data class Text(val text: String) : MdInline
    data class Strong(val children: List<MdInline>) : MdInline
    data class Emphasis(val children: List<MdInline>) : MdInline
    data class Code(val text: String) : MdInline
    data class Link(val url: String, val children: List<MdInline>) : MdInline
}

sealed interface MdBlock {
    data class Heading(val level: Int, val inlines: List<MdInline>) : MdBlock
    data class Paragraph(val inlines: List<MdInline>) : MdBlock
    data class ListBlock(val ordered: Boolean, val items: List<List<MdInline>>) : MdBlock
    data class Quote(val inlines: List<MdInline>) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Table(val header: List<List<MdInline>>, val rows: List<List<List<MdInline>>>) : MdBlock
    data object Rule : MdBlock
}

private val HEADING = Regex("""^(#{1,4})\s+(.+?)\s*#*\s*$""")
private val BULLET = Regex("""^\s*[-*+•]\s+(.*)$""")
private val ORDERED = Regex("""^\s*\d{1,3}[.)]\s+(.*)$""")
private val QUOTE = Regex("""^\s*>\s?(.*)$""")
private val FENCE = Regex("""^\s*```""")
private val RULE = Regex("""^\s*(?:-{3,}|\*{3,}|_{3,})\s*$""")
private val TABLE_ROW = Regex("""^\s*\|.*\|\s*$""")
private val TABLE_DIVIDER = Regex("""^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$""")
private val INLINE = Regex("""(`[^`]+`)|(\*\*[^*]+\*\*|__[^_]+__)|(\*[^*\s][^*]*\*|_[^_\s][^_]*_)|(\[[^\]]+]\([^)\s]+\))""")
private val LINK = Regex("""^\[([^\]]+)]\(([^)\s]+)\)$""")

fun parseInline(text: String): List<MdInline> {
    val nodes = mutableListOf<MdInline>()
    var last = 0
    for (match in INLINE.findAll(text)) {
        if (match.range.first > last) nodes += MdInline.Text(text.substring(last, match.range.first))
        val token = match.value
        nodes += when {
            match.groups[1] != null -> MdInline.Code(token.substring(1, token.length - 1))
            match.groups[2] != null -> MdInline.Strong(parseInline(token.substring(2, token.length - 2)))
            match.groups[3] != null -> MdInline.Emphasis(parseInline(token.substring(1, token.length - 1)))
            else -> {
                val (label, url) = LINK.find(token)!!.destructured
                if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                    MdInline.Link(url, parseInline(label))
                } else {
                    MdInline.Text(label)
                }
            }
        }
        last = match.range.last + 1
    }
    if (last < text.length) nodes += MdInline.Text(text.substring(last))
    return nodes
}

private fun splitRow(line: String): List<String> =
    line.trim().removePrefix("|").removeSuffix("|").split('|').map { it.trim() }

fun parseMarkdown(markdown: String): List<MdBlock> {
    val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val blocks = mutableListOf<MdBlock>()
    var index = 0
    fun startsBlock(line: String) = HEADING.matches(line) || FENCE.containsMatchIn(line) || QUOTE.matches(line) ||
        BULLET.matches(line) || ORDERED.matches(line) || RULE.matches(line) || TABLE_ROW.matches(line)

    while (index < lines.size) {
        val line = lines[index]
        when {
            line.isBlank() -> index++
            FENCE.containsMatchIn(line) -> {
                val code = mutableListOf<String>()
                index++
                while (index < lines.size && !FENCE.containsMatchIn(lines[index])) code += lines[index++]
                index++
                blocks += MdBlock.Code(code.joinToString("\n"))
            }
            HEADING.matches(line) -> {
                val match = HEADING.find(line)!!
                blocks += MdBlock.Heading(match.groupValues[1].length, parseInline(match.groupValues[2]))
                index++
            }
            RULE.matches(line) -> {
                blocks += MdBlock.Rule
                index++
            }
            TABLE_ROW.matches(line) && index + 1 < lines.size && TABLE_DIVIDER.matches(lines[index + 1]) -> {
                val header = splitRow(line).map(::parseInline)
                val rows = mutableListOf<List<List<MdInline>>>()
                index += 2
                while (index < lines.size && TABLE_ROW.matches(lines[index])) rows += splitRow(lines[index++]).map(::parseInline)
                blocks += MdBlock.Table(header, rows)
            }
            QUOTE.matches(line) -> {
                val quote = mutableListOf<String>()
                while (index < lines.size && QUOTE.matches(lines[index])) quote += QUOTE.find(lines[index++])!!.groupValues[1]
                blocks += MdBlock.Quote(parseInline(quote.joinToString(" ").trim()))
            }
            BULLET.matches(line) || ORDERED.matches(line) -> {
                val ordered = !BULLET.matches(line)
                val pattern = if (ordered) ORDERED else BULLET
                val items = mutableListOf<List<MdInline>>()
                while (index < lines.size && pattern.matches(lines[index])) items += parseInline(pattern.find(lines[index++])!!.groupValues[1])
                blocks += MdBlock.ListBlock(ordered, items)
            }
            else -> {
                val paragraph = mutableListOf<String>()
                while (index < lines.size && lines[index].isNotBlank() && !startsBlock(lines[index])) paragraph += lines[index++].trim()
                if (paragraph.isEmpty()) {
                    // Строка таблицы без разделителя — обычный текст.
                    paragraph += line.trim()
                    index++
                }
                blocks += MdBlock.Paragraph(parseInline(paragraph.joinToString(" ")))
            }
        }
    }
    return blocks
}

fun List<MdInline>.plainText(): String = joinToString("") { node ->
    when (node) {
        is MdInline.Text -> node.text
        is MdInline.Code -> node.text
        is MdInline.Strong -> node.children.plainText()
        is MdInline.Emphasis -> node.children.plainText()
        is MdInline.Link -> node.children.plainText()
    }
}

/** Текст без разметки — для озвучивания «Прочитать вслух». */
fun markdownPlainText(markdown: String): String = parseMarkdown(markdown).mapNotNull { block ->
    when (block) {
        is MdBlock.Heading -> block.inlines.plainText()
        is MdBlock.Paragraph -> block.inlines.plainText()
        is MdBlock.Quote -> block.inlines.plainText()
        is MdBlock.ListBlock -> block.items.joinToString("\n") { it.plainText() }
        is MdBlock.Table -> (listOf(block.header) + block.rows).joinToString("\n") { row -> row.joinToString(" — ") { it.plainText() } }
        is MdBlock.Code -> block.text
        MdBlock.Rule -> null
    }
}.filter { it.isNotBlank() }.joinToString("\n\n")
