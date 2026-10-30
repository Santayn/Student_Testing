// @vitest-environment node
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const css = readFileSync(resolve(process.cwd(), 'src/theme/motion.css'), 'utf8')
const disclosure = readFileSync(resolve(process.cwd(), 'src/components/ui/UiDisclosure.vue'), 'utf8')

describe('Complex motion CSS invariants', () => {
  it('keeps table motion paint-only, not per-row transforms or keyframes', () => {
    expect(css).toContain('.st-motion-table--row-highlight')
    expect(css).toContain('transition: background-color')
    expect(css).not.toMatch(/st-motion-table--row-highlight[^{}]*\{[^}]*transform:/)
  })

  it('does not rebuild a disclosure body when hiding', () => {
    expect(disclosure).toContain('v-show="expanded"')
    expect(disclosure).not.toContain('v-if="expanded"')
    expect(disclosure).toContain(':aria-controls="contentId"')
    expect(disclosure).toContain(':aria-expanded=')
  })

  it('respects reduced-motion and avoids layout-wide animation', () => {
    expect(css).toContain('@media (prefers-reduced-motion: reduce)')
    for (const key of ['st-motion-field--error-reveal', 'st-motion-toast--fade-slide', 'st-motion-loading--soft-pulse']) {
      expect(css).toContain(key)
    }
    expect(css).not.toMatch(/transition\s*:\s*all\b/)
  })
})
