// @vitest-environment node
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const source = (file) => readFileSync(resolve(process.cwd(), file), 'utf8')

describe('Glass material and sweep stylesheet', () => {
  const css = source('src/theme/glass.css')
  const tokens = source('src/theme/tokens.css')
  const main = source('src/main.js')

  it('loads after motion and foundational styles, without replacing existing defaults', () => {
    expect(main.indexOf("import '@/theme/glass.css'")).toBeGreaterThan(
      main.indexOf("import '@/theme/motion.css'")
    )
    expect(css).toContain('.st-glass-surface')
    expect(css).toContain('.st-glass-surface--frosted')
  })

  it('uses a single fixed horizontal sweep and never tracks cursor coordinates', () => {
    expect(css).toContain('@keyframes st-glass-sweep')
    expect(css).toContain('linear-gradient(')
    expect(css).toContain('110deg')
    expect(css).toContain('translate3d(-60%, 0, 0)')
    expect(css).toContain('translate3d(60%, 0, 0)')
    expect(css).toMatch(/st-glass-shine-duration\)\s+var\(--st-ease-standard\)\s+1\s+both/)
    expect(css).not.toContain('infinite')
    expect(css).not.toContain('mousemove')
    expect(css).not.toContain('pointermove')
    expect(css).toContain('@media (hover: hover) and (pointer: fine)')
    expect(css).toContain(':focus-visible::before')
  })

  it('keeps glass button text color stable for hover, focus and active states', () => {
    const foundation = source('src/theme/foundation.css')
    expect(foundation).toContain('color: var(--st-on-primary);')

    expect(css).toMatch(/--st-glass-button-text-color:\s*var\(--st-text\)/)
    expect(css).toMatch(/--st-glass-button-text-color:\s*var\(--st-primary-soft-text\)/)
    expect(css).toMatch(/--st-glass-button-text-color:\s*var\(--st-danger-text\)/)
    expect(css).toMatch(/--st-glass-button-text-color:\s*var\(--st-success-text\)/)
    expect(css).toContain('.st-ui-button.p-button.st-glass-surface:not(:disabled):is(:hover, :focus-visible, :active)')
    expect(css).toContain(".st-ui-link-button.st-glass-surface:not([aria-disabled='true']):is(:hover, :focus-visible, :active)")
    expect(css).toMatch(/color:\s*var\(--st-glass-button-text-color\)/)
  })

  it('provides theme-specific sun/moon colors and accessible fallbacks', () => {
    expect(tokens).toContain('--st-glass-shine-soft: rgb(246 197 110 / 23%);')
    expect(tokens).toContain('--st-glass-shine-soft: rgb(122 168 234 / 22%);')
    expect(css).toContain('pointer-events: none')
    expect(css).toContain('@media (prefers-reduced-motion: reduce)')
    expect(css).toContain('@media (prefers-reduced-transparency: reduce)')
    expect(css).toContain('@supports not (backdrop-filter: blur(1px))')
    expect(css).not.toMatch(/transition\s*:\s*all/)
  })
})
