/*
 * Разбор Markdown текста лекции в дерево блоков.
 *
 * Поддерживается то, что выдаёт распознавание сканов и что удобно писать вручную:
 * заголовки #–####, абзацы, маркированные и нумерованные списки, цитаты, блоки кода,
 * таблицы, горизонтальная линия, **жирный**, *курсив*, `код` и ссылки http(s).
 * HTML не поддерживается намеренно: дерево отрисовывается узлами Vue, без v-html.
 */

const HEADING = /^(#{1,4})\s+(.+?)\s*#*\s*$/
const BULLET = /^\s*[-*+•]\s+(.*)$/
const ORDERED = /^\s*\d{1,3}[.)]\s+(.*)$/
const QUOTE = /^\s*>\s?(.*)$/
const FENCE = /^\s*```/
const RULE = /^\s*(?:-{3,}|\*{3,}|_{3,})\s*$/
const TABLE_ROW = /^\s*\|.*\|\s*$/
const TABLE_DIVIDER = /^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$/

function splitTableRow(line) {
  return line
    .trim()
    .replace(/^\|/, '')
    .replace(/\|$/, '')
    .split('|')
    .map((cell) => cell.trim())
}

function isSafeUrl(url) {
  return /^https?:\/\//i.test(url)
}

/** Разбирает строку на inline-узлы: text, strong, em, code, link. */
export function parseInline(text) {
  const source = String(text ?? '')
  const nodes = []
  const pattern = /(`[^`]+`)|(\*\*[^*]+\*\*|__[^_]+__)|(\*[^*\s][^*]*\*|_[^_\s][^_]*_)|(\[[^\]]+\]\([^)\s]+\))/g
  let lastIndex = 0
  let match

  while ((match = pattern.exec(source)) !== null) {
    if (match.index > lastIndex) {
      nodes.push({ type: 'text', text: source.slice(lastIndex, match.index) })
    }

    const token = match[0]

    if (match[1]) {
      nodes.push({ type: 'code', text: token.slice(1, -1) })
    } else if (match[2]) {
      nodes.push({ type: 'strong', children: parseInline(token.slice(2, -2)) })
    } else if (match[3]) {
      nodes.push({ type: 'em', children: parseInline(token.slice(1, -1)) })
    } else {
      const [, label, url] = token.match(/^\[([^\]]+)\]\(([^)\s]+)\)$/)
      nodes.push(
        isSafeUrl(url)
          ? { type: 'link', url, children: parseInline(label) }
          : { type: 'text', text: label }
      )
    }

    lastIndex = match.index + token.length
  }

  if (lastIndex < source.length) {
    nodes.push({ type: 'text', text: source.slice(lastIndex) })
  }

  return nodes
}

export function parseMarkdown(markdown) {
  const lines = String(markdown ?? '')
    .replace(/\r\n?/g, '\n')
    .split('\n')
  const blocks = []
  let index = 0

  while (index < lines.length) {
    const line = lines[index]

    if (!line.trim()) {
      index++
      continue
    }

    if (FENCE.test(line)) {
      const code = []
      index++
      while (index < lines.length && !FENCE.test(lines[index])) {
        code.push(lines[index])
        index++
      }
      index++
      blocks.push({ type: 'code', text: code.join('\n') })
      continue
    }

    const heading = line.match(HEADING)
    if (heading) {
      blocks.push({
        type: 'heading',
        level: heading[1].length,
        children: parseInline(heading[2]),
      })
      index++
      continue
    }

    if (RULE.test(line)) {
      blocks.push({ type: 'rule' })
      index++
      continue
    }

    if (TABLE_ROW.test(line) && index + 1 < lines.length && TABLE_DIVIDER.test(lines[index + 1])) {
      const header = splitTableRow(line).map(parseInline)
      const rows = []
      index += 2
      while (index < lines.length && TABLE_ROW.test(lines[index])) {
        rows.push(splitTableRow(lines[index]).map(parseInline))
        index++
      }
      blocks.push({ type: 'table', header, rows })
      continue
    }

    if (QUOTE.test(line)) {
      const quote = []
      while (index < lines.length && QUOTE.test(lines[index])) {
        quote.push(lines[index].match(QUOTE)[1])
        index++
      }
      blocks.push({ type: 'quote', children: parseInline(quote.join(' ').trim()) })
      continue
    }

    if (BULLET.test(line) || ORDERED.test(line)) {
      const ordered = !BULLET.test(line)
      const pattern = ordered ? ORDERED : BULLET
      const items = []
      while (index < lines.length && pattern.test(lines[index])) {
        items.push(parseInline(lines[index].match(pattern)[1]))
        index++
      }
      blocks.push({ type: 'list', ordered, items })
      continue
    }

    const paragraph = []
    while (
      index < lines.length &&
      lines[index].trim() &&
      !HEADING.test(lines[index]) &&
      !FENCE.test(lines[index]) &&
      !QUOTE.test(lines[index]) &&
      !BULLET.test(lines[index]) &&
      !ORDERED.test(lines[index]) &&
      !RULE.test(lines[index]) &&
      !TABLE_ROW.test(lines[index])
    ) {
      paragraph.push(lines[index].trim())
      index++
    }
    if (paragraph.length) {
      blocks.push({ type: 'paragraph', children: parseInline(paragraph.join(' ')) })
    } else {
      // Строка таблицы без разделителя — показываем как обычный текст.
      blocks.push({ type: 'paragraph', children: parseInline(line.trim()) })
      index++
    }
  }

  return blocks
}

function inlinePlain(nodes) {
  return (nodes ?? []).map((node) => (
    node.children ? inlinePlain(node.children) : node.text ?? ''
  )).join('')
}

function sentence(text) {
  const value = String(text ?? '').trim()
  return !value || /[.!?…:;]$/.test(value) ? value : `${value}.`
}

/** Текст лекции без разметки — для чтения вслух: блоки разделены точками, код пропускается. */
export function markdownPlainText(markdown) {
  return parseMarkdown(markdown).flatMap((block) => {
    switch (block.type) {
      case 'heading':
      case 'paragraph':
      case 'quote':
        return [sentence(inlinePlain(block.children))]
      case 'list':
        return block.items.map((item) => sentence(inlinePlain(item)))
      case 'table':
        return [block.header, ...block.rows].map((row) => sentence(row.map(inlinePlain).join(', ')))
      default:
        return []
    }
  }).filter(Boolean).join('\n')
}
