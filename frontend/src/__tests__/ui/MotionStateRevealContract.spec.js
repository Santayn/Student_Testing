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

describe('motion stage 3 state reveal contract', () => {
  const foundation = source('src/theme/foundation.css')
  const loading = source('src/components/ui/UiLoadingState.vue')

  it('defines one bounded reusable state reveal', () => {
    expect(foundation).toContain('.st-motion-state-reveal')
    expect(foundation).toContain('@keyframes st-motion-state-reveal')
    expect(foundation).toContain('transform: translateY(2px)')
    expect(foundation).toContain('var(--st-motion-normal)')
    expect(foundation).toContain('var(--st-ease-standard)')
  })

  it('applies reveal to loading, validation errors and alerts', () => {
    expect(loading).toContain(
      'class="st-ui-loading st-motion-state-reveal"'
    )
    expect(foundation).toMatch(
      /\.st-ui-field__message--error\s*\{[\s\S]*?animation:[\s\S]*?st-motion-state-reveal/
    )
    expect(foundation).toMatch(
      /\.st-ui-alert\.p-message\s*\{[\s\S]*?animation:[\s\S]*?st-motion-state-reveal/
    )
  })

  it('disables state reveal under reduced motion', () => {
    expect(foundation).toMatch(
      /@media \(prefers-reduced-motion: reduce\)[\s\S]*?\.st-motion-state-reveal/
    )
  })

  it('does not add route or card entrance animation contracts', () => {
    expect(foundation).not.toContain('.app-main.st-motion-state-reveal')
    expect(foundation).not.toContain('.st-ui-card.st-motion-state-reveal')
  })
})
