<script>
import { h } from 'vue'

import { parseMarkdown } from '@/utils/markdown'

const HEADING_CLASSES = {
  1: ['h2', 'lecture-content__section'],
  2: ['h2', 'lecture-content__section'],
  3: ['h3', 'lecture-content__subsection'],
  4: ['h4', 'lecture-content__heading'],
}

function renderInline(nodes) {
  return nodes.map((node) => {
    switch (node.type) {
      case 'strong':
        return h('strong', renderInline(node.children))
      case 'em':
        return h('em', renderInline(node.children))
      case 'code':
        return h('code', { class: 'lecture-content__inline-code' }, node.text)
      case 'link':
        return h(
          'a',
          {
            class: 'lecture-content__link',
            href: node.url,
            target: '_blank',
            rel: 'noopener noreferrer',
          },
          renderInline(node.children)
        )
      default:
        return node.text
    }
  })
}

function renderBlock(block, index) {
  switch (block.type) {
    case 'heading': {
      const [tag, className] = HEADING_CLASSES[block.level] ?? HEADING_CLASSES[4]
      return h(tag, { key: index, class: className }, renderInline(block.children))
    }
    case 'list':
      return h(
        block.ordered ? 'ol' : 'ul',
        { key: index, class: 'lecture-content__list' },
        block.items.map((item, itemIndex) => h('li', { key: itemIndex }, renderInline(item)))
      )
    case 'quote':
      return h('blockquote', { key: index, class: 'lecture-content__quote' }, renderInline(block.children))
    case 'code':
      return h('pre', { key: index, class: 'lecture-content__code' }, h('code', block.text))
    case 'rule':
      return h('hr', { key: index, class: 'lecture-content__rule' })
    case 'table':
      return h('div', { key: index, class: 'lecture-content__table-wrap' }, [
        h('table', { class: 'lecture-content__table' }, [
          h('thead', h('tr', block.header.map((cell, cellIndex) => h('th', { key: cellIndex }, renderInline(cell))))),
          h(
            'tbody',
            block.rows.map((row, rowIndex) =>
              h('tr', { key: rowIndex }, row.map((cell, cellIndex) => h('td', { key: cellIndex }, renderInline(cell))))
            )
          ),
        ]),
      ])
    default:
      return h('p', { key: index, class: 'lecture-content__text' }, renderInline(block.children))
  }
}

/** Текст лекции в Markdown, отрисованный семантическими классами lecture-content. */
export default {
  name: 'LectureMarkdown',
  props: {
    source: { type: String, default: '' },
  },
  setup(props) {
    return () => h('div', { class: 'lecture-content' }, parseMarkdown(props.source).map(renderBlock))
  },
}
</script>
