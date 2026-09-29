// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(process.cwd(), relativePath),
    'utf8'
  )
}

describe('typography foundation', () => {
  it('loads local Manrope before application typography tokens', () => {
    const main = source('src/main.js')
    const fonts = source('src/theme/fonts.css')
    const html = source('index.html')

    expect(main).toContain("import '@/theme/fonts.css'")
    expect(main.indexOf("import '@/theme/fonts.css'"))
      .toBeLessThan(main.indexOf("import '@/theme/tokens.css'"))

    expect(fonts).toContain('font-family: "Manrope"')
    expect(fonts).toContain('/fonts/Manrope-VariableFont_wght.ttf')
    expect(fonts).toContain('font-weight: 200 800')
    expect(fonts).toContain('font-display: swap')

    expect(html).toContain('rel="preload"')
    expect(html).toContain('/fonts/Manrope-VariableFont_wght.ttf')
    expect(html).toContain('type="font/ttf"')
  })

  it('exposes one font stack and exactly four application weight tokens', () => {
    const tokens = source('src/theme/tokens.css')

    expect(tokens).toContain('"Manrope", ui-sans-serif')
    expect(tokens).toContain('--st-font-weight-regular: 400;')
    expect(tokens).toContain('--st-font-weight-medium: 500;')
    expect(tokens).toContain('--st-font-weight-semibold: 600;')
    expect(tokens).toContain('--st-font-weight-bold: 700;')
  })

  it('inherits the application font globally without overriding icon fonts', () => {
    const base = source('src/theme/base.css')

    expect(base).toContain('font-family: var(--st-font-sans);')
    expect(base).toContain('font-weight: var(--st-font-weight-regular);')
    expect(base).toContain('button,')
    expect(base).toContain('font: inherit;')
    expect(base).not.toContain('* {\n  font-family:')
  })
})
