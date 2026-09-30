// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import {
  describe,
  expect,
  it,
} from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

describe('motion design contract', () => {
  const tokens = source('src/theme/tokens.css')
  const base = source('src/theme/base.css')
  const foundation = source('src/theme/foundation.css')
  const header = source('src/components/layout/AppHeader.vue')
  const sidebar = source('src/components/layout/AppSidebar.vue')
  const testView = source('src/views/tests/TestView.vue')

  it('defines the shared motion duration and easing tokens', () => {
    expect(tokens).toContain('--st-motion-fast: 140ms')
    expect(tokens).toContain('--st-motion-normal: 180ms')
    expect(tokens).toContain('--st-motion-slow: 240ms')
    expect(tokens).toContain('--st-ease-standard: cubic-bezier(0.2, 0, 0, 1)')
    expect(tokens).toContain('--st-ease-emphasized: var(--st-ease-standard)')
  })

  it('uses motion tokens for foundation and shell transitions', () => {
    for (const value of [
      base,
      foundation,
      header,
      sidebar,
      testView,
    ]) {
      expect(value).toContain('var(--st-motion-')
      expect(value).toContain('var(--st-ease-')
    }
  })

  it('keeps reduced-motion overrides', () => {
    expect(base).toContain('@media (prefers-reduced-motion: reduce)')
    expect(foundation).toContain('@media (prefers-reduced-motion: reduce)')
    expect(header).toContain('@media (prefers-reduced-motion: reduce)')
    expect(sidebar).toContain('@media (prefers-reduced-motion: reduce)')
    expect(testView).toContain('@media (prefers-reduced-motion: reduce)')
  })

  it('does not introduce transition all', () => {
    const combined = [base, foundation, header, sidebar, testView].join('\n')
    expect(combined).not.toMatch(/transition\s*:\s*all\b/)
  })
})
