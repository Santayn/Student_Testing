// @vitest-environment node
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import { isDocumentScrollable } from '@/utils/documentScrollbar'

const read = (path) => readFileSync(resolve(process.cwd(), path), 'utf8')

describe('document scrollbar/gutter lifecycle', () => {
  it('reserves no gutter on short pages and detects actual document overflow', () => {
    expect(isDocumentScrollable({ scrollHeight: 800, clientHeight: 800 })).toBe(false)
    expect(isDocumentScrollable({ scrollHeight: 801, clientHeight: 800 })).toBe(false)
    expect(isDocumentScrollable({ scrollHeight: 1010, clientHeight: 800 })).toBe(true)
    expect(isDocumentScrollable(null)).toBe(false)
  })

  it('keeps normal document scrollbar browser-native and covers PrimeVue locks', () => {
    const base = read('src/theme/base.css')
    const manager = read('src/utils/documentScrollbar.js')
    const main = read('src/main.js')
    expect(base).toContain('scrollbar-gutter: auto')
    expect(base).toContain('html.st-document-scrollable')
    expect(base).toContain('html:has(body.p-overflow-hidden)')
    expect(base).not.toMatch(/html::?-webkit-scrollbar|body::?-webkit-scrollbar/)
    expect(manager).toContain("body.classList.contains('p-overflow-hidden')")
    expect(manager).toContain("root.classList.contains('navigation-sheet-open')")
    expect(manager).toContain('if (disposed || isLocked()) return')
    expect(main).toContain('initDocumentScrollbar()')
  })
})
