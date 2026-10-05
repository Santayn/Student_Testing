// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(path) {
  return readFileSync(
    resolve(process.cwd(), path),
    'utf8'
  )
}

describe('lecture content typography contract', () => {
  const css = source('src/theme/lecture-content.css')
  const main = source('src/main.js')
  const tokens = source('src/theme/tokens.css')

  it('loads the lecture content stylesheet globally', () => {
    expect(main).toContain("import '@/theme/lecture-content.css'")
  })

  it('provides semantic lecture content classes', () => {
    for (const className of [
      '.lecture-content',
      '.lecture-content__text',
      '.lecture-content__lead',
      '.lecture-content__section',
      '.lecture-content__subsection',
      '.lecture-content__heading',
      '.lecture-content__note',
      '.lecture-content__quote',
      '.lecture-content__list',
      '.lecture-content__code',
      '.lecture-content__figure',
      '.lecture-content__caption',
      '.lecture-content__link',
    ]) {
      expect(css).toContain(className)
    }
  })

  it('uses the fixed application font-weight tokens', () => {
    expect(tokens).toContain('--st-font-weight-regular: 400')
    expect(tokens).toContain('--st-font-weight-medium: 500')
    expect(tokens).toContain('--st-font-weight-semibold: 600')
    expect(tokens).toContain('--st-font-weight-bold: 700')

    expect(css).toContain('var(--st-font-weight-regular)')
    expect(css).toContain('var(--st-font-weight-medium)')
    expect(css).toContain('var(--st-font-weight-semibold)')
    expect(css).toContain('var(--st-font-weight-bold)')
  })

  it('keeps code content on the dedicated monospace token', () => {
    expect(css).toContain('font-family: var(--st-font-mono)')
  })

  it('does not introduce a lecture h1 style', () => {
    expect(css).not.toContain('lecture-content__h1')
  })
})
