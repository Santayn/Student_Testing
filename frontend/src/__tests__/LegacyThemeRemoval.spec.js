// @vitest-environment node

import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { dirname, extname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const srcDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')

function source(relativePath) {
  return readFileSync(resolve(srcDir, relativePath), 'utf8')
}

function productionSources(dir = srcDir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    if (entry.name === '__tests__') return []
    const path = join(dir, entry.name)
    if (entry.isDirectory()) return productionSources(path)
    return ['.vue', '.css', '.js', '.html'].includes(extname(entry.name))
      ? [path]
      : []
  })
}

const legacyTokens = [
  'bg', 'surface', 'surface-secondary', 'text', 'text-secondary',
  'text-on-brand', 'border', 'brand', 'brand-hover', 'brand-soft',
  'focus-ring', 'success', 'success-soft', 'warning', 'warning-soft',
  'warning-border', 'danger', 'danger-soft', 'danger-border', 'overlay',
  'shadow', 'shadow-elevated', 'shadow-hover', 'header-bg', 'header-text',
  'header-muted-text', 'header-border', 'header-hover',
  'header-button-border', 'header-button-hover',
]

const legacyUse = new RegExp(
  `(?:var\\(\\s*|(?<![\\w-])--)(?:${legacyTokens.join('|')})(?=\\s*[,):]|\\s*:)`,
  'g',
)

describe('legacy stylesheet removal', () => {
  it('removes the old entrypoint and keeps the new base stylesheet after tokens', () => {
    const main = source('main.js')
    expect(existsSync(resolve(srcDir, 'assets/theme.css'))).toBe(false)
    expect(main).not.toContain("import '@/assets/theme.css'")
    expect(main.indexOf("import '@/theme/tokens.css'"))
      .toBeLessThan(main.indexOf("import '@/theme/base.css'"))
    expect(main.indexOf("import '@/theme/base.css'"))
      .toBeLessThan(main.indexOf("import '@/theme/foundation.css'"))
  })

  it('keeps document defaults, both color schemes and selection styling', () => {
    const base = source('theme/base.css')
    expect(base).toMatch(/html\[data-theme='light'\]\s*\{\s*color-scheme:\s*light;/)
    expect(base).toMatch(/html\[data-theme='dark'\]\s*\{\s*color-scheme:\s*dark;/)
    expect(base).toMatch(/html\s*\{[^}]*background:\s*var\(--st-page-bg\)/s)
    expect(base).toMatch(/html\s*\{[^}]*color:\s*var\(--st-text\)/s)
    expect(base).toMatch(/body\s*\{[^}]*background-color\s+0\.16s\s+ease/s)
    expect(base).toMatch(/::selection\s*\{[^}]*color:\s*var\(--st-on-primary\)/s)
    expect(base).toMatch(/::selection\s*\{[^}]*background:\s*var\(--st-primary\)/s)
  })

  it('does not leave legacy color variables in production source', () => {
    for (const path of productionSources()) {
      const content = readFileSync(path, 'utf8')
      const matches = [...content.matchAll(legacyUse)].map(([match]) => match)
      expect(matches, `legacy tokens remain in ${path}`).toEqual([])
    }
  })

  it('declares every production --st-* reference in the design system', () => {
    const tokens = source('theme/tokens.css')
    const declared = new Set(
      [...tokens.matchAll(/(--st-[\w-]+)\s*:/g)].map(([, name]) => name),
    )
    for (const path of productionSources()) {
      const content = readFileSync(path, 'utf8')
      const references = [...content.matchAll(/var\((--st-[\w-]+)/g)]
        .map(([, name]) => name)
      for (const token of references) {
        expect(declared.has(token), `${path} uses undefined ${token}`).toBe(true)
      }
    }
  })

  it('retains AdminTable with declared Student Testing theme tokens', () => {
    const table = source('components/admin/AdminTable.vue')
    const tokens = source('theme/tokens.css')
    const declared = new Set(
      [...tokens.matchAll(/(--st-[\w-]+)\s*:/g)].map(([, name]) => name),
    )
    const references = [...table.matchAll(/var\((--st-[\w-]+)/g)]
      .map(([, name]) => name)
    expect(references.length).toBeGreaterThan(0)
    for (const token of references) {
      expect(declared.has(token), `${token} is not declared`).toBe(true)
    }
  })
})
