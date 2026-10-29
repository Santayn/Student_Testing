import {
  describe,
  expect,
  it,
} from 'vitest'

import { mount } from '@vue/test-utils'

import LectureMarkdown from '@/components/lectures/LectureMarkdown.vue'
import {
  parseInline,
  parseMarkdown,
} from '@/utils/markdown'

describe('lecture markdown parser', () => {
  it('parses headings, paragraphs, lists, quotes, code and tables', () => {
    const blocks = parseMarkdown([
      '## Реляционная модель',
      'Данные хранятся',
      'в таблицах.',
      '',
      '- строки',
      '- столбцы',
      '',
      '1. первый',
      '2) второй',
      '',
      '> Отношение — это таблица',
      '',
      '```',
      'SELECT * FROM t;',
      '```',
      '',
      '| Термин | Смысл |',
      '|---|---|',
      '| Кортеж | Строка |',
    ].join('\n'))

    expect(blocks.map((block) => block.type)).toEqual([
      'heading', 'paragraph', 'list', 'list', 'quote', 'code', 'table',
    ])
    expect(blocks[0].level).toBe(2)
    expect(blocks[1].children).toEqual([{ type: 'text', text: 'Данные хранятся в таблицах.' }])
    expect(blocks[2].ordered).toBe(false)
    expect(blocks[3].ordered).toBe(true)
    expect(blocks[3].items).toHaveLength(2)
    expect(blocks[5].text).toBe('SELECT * FROM t;')
    expect(blocks[6].rows[0][0]).toEqual([{ type: 'text', text: 'Кортеж' }])
  })

  it('keeps only http(s) links and parses emphasis', () => {
    expect(parseInline('**важно** и *курсив* и `код`').map((node) => node.type))
      .toEqual(['strong', 'text', 'em', 'text', 'code'])
    expect(parseInline('[сайт](https://example.org)')[0]).toMatchObject({ type: 'link', url: 'https://example.org' })
    expect(parseInline('[клик](javascript:alert(1))')[0]).toMatchObject({ type: 'text' })
  })

  it('renders semantic lecture classes and never injects raw html', () => {
    const wrapper = mount(LectureMarkdown, {
      props: {
        source: '# Тема\n### Подтема\n<script>alert(1)</script> текст\n\n[ссылка](https://example.org)',
      },
    })

    expect(wrapper.find('h1').exists()).toBe(false)
    expect(wrapper.find('h2.lecture-content__section').text()).toBe('Тема')
    expect(wrapper.find('h3.lecture-content__subsection').text()).toBe('Подтема')
    expect(wrapper.find('script').exists()).toBe(false)
    expect(wrapper.text()).toContain('<script>alert(1)</script> текст')
    const link = wrapper.find('a.lecture-content__link')
    expect(link.attributes('rel')).toBe('noopener noreferrer')
    expect(link.attributes('target')).toBe('_blank')

    wrapper.unmount()
  })
})
