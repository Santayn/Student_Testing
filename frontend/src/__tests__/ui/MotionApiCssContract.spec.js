// @vitest-environment node
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('Motion API stylesheet', () => {
  const motion = source('src/theme/motion.css')
  const main = source('src/main.js')
  const tokens = source('src/theme/tokens.css')

  it('loads after the current foundation rules without changing their defaults', () => {
    expect(main.indexOf("import '@/theme/motion.css'")).toBeGreaterThan(
      main.indexOf("import '@/theme/foundation.css'")
    )
    for (const token of ['--st-motion-fast:', '--st-motion-normal:', '--st-motion-slow:']) {
      expect(tokens).toContain(token)
    }
  })

  it('supports explicit disabled/default states, accessible reduced motion, and conditional Vue transitions', () => {
    expect(motion).toContain('.st-motion-button--none')
    expect(motion).toContain('.st-motion-dialog--none')
    expect(motion).toContain('.st-motion-expand-enter-from')
    expect(motion).toContain('@media (prefers-reduced-motion: reduce)')
    expect(motion).not.toMatch(/transition\s*:\s*all\b/)
    expect(motion).toContain('.st-motion-button--hover')
    expect(motion).toContain('.st-motion-button--focused:focus-visible')
    expect(motion).toContain('.st-motion-card--focused:focus-within')
    expect(motion).toContain('.st-motion-select--focused:focus-within')
    expect(motion).toContain('@media (hover: hover) and (pointer: fine)')
  })
})
