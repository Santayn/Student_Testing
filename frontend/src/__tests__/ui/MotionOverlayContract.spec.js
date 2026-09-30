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

describe('motion stage 2 overlay contract', () => {
  const foundation = source('src/theme/foundation.css')

  it('uses the fast motion token for anchored overlays', () => {
    expect(foundation).toContain('.p-anchored-overlay-enter-active')
    expect(foundation).toContain(
      'opacity var(--st-motion-fast) var(--st-ease-standard)'
    )
    expect(foundation).toContain('transform: translateY(2px)')
  })

  it('uses normal motion for dialogs', () => {
    expect(foundation).toContain('.p-dialog-enter-active')
    expect(foundation).toContain(
      'opacity var(--st-motion-normal) var(--st-ease-standard)'
    )
    expect(foundation).toContain('transform: translateY(4px)')
  })

  it('keeps drawer direction owned by PrimeVue and only normalizes timing', () => {
    expect(foundation).toContain('.p-drawer-enter-active')
    expect(foundation).toContain(
      'transition-duration: var(--st-motion-slow) !important'
    )
    expect(foundation).toContain(
      'transition-timing-function: var(--st-ease-standard) !important'
    )
  })

  it('uses normal motion for toast messages', () => {
    expect(foundation).toContain('.p-toast-message-enter-active')
    expect(foundation).toContain(
      'transform: translateY(-4px)'
    )
  })

  it('disables overlay movement under reduced motion', () => {
    expect(foundation).toMatch(
      /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.p-anchored-overlay-enter-active/
    )
    expect(foundation).toMatch(
      /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.p-dialog-enter-active/
    )
    expect(foundation).toMatch(
      /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.p-drawer-enter-active/
    )
    expect(foundation).toMatch(
      /@media \(prefers-reduced-motion: reduce\)[\s\S]*\.p-toast-message-enter-active/
    )
  })

  it('does not introduce bounce, rotation or scale-heavy overlay motion', () => {
    const stage2Start = foundation.indexOf(
      'Motion Stage 2 — PrimeVue overlay motion contract'
    )
    const stage2End = foundation.indexOf(
      '@media (max-width: 640px)',
      stage2Start
    )
    const stage2 = foundation.slice(stage2Start, stage2End)

    expect(stage2).not.toContain('scale(')
    expect(stage2).not.toContain('rotate(')
    expect(stage2).not.toContain('bounce')
  })
})
