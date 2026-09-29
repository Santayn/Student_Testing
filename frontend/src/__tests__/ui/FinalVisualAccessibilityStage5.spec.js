// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(...segments) {
  return readFileSync(
    resolve(process.cwd(), 'src', ...segments),
    'utf8'
  )
}

const base = source('theme', 'base.css')
const foundation = source('theme', 'foundation.css')
const header = source('components', 'layout', 'AppHeader.vue')
const breadcrumb = source('components', 'layout', 'AppBreadcrumb.vue')
const footer = source('components', 'layout', 'AppFooter.vue')
const testView = source('views', 'tests', 'TestView.vue')

describe('UI Stage 5 final visual and accessibility contracts', () => {
  it('respects reduced-motion preferences across global and high-motion UI', () => {
    expect(base).toContain('@media (prefers-reduced-motion: reduce)')
    expect(base).toContain('scroll-behavior: auto')
    expect(foundation).toContain('.p-select-option')
    expect(foundation).toContain('.st-ui-dialog.p-dialog')
    expect(foundation).toContain('.st-ui-drawer.p-drawer')
    expect(header).toContain('@media (prefers-reduced-motion: reduce)')
    expect(testView).toContain('.test-progress__track span')
    expect(testView).toContain('transition: none')
  })

  it('keeps the mobile header account panel inside the dynamic viewport', () => {
    expect(header).toContain('max-height: min(620px, calc(100dvh - 58px))')
    expect(header).toContain('overflow-y: auto')
    expect(header).toContain('overscroll-behavior: contain')
  })

  it('keeps breadcrumb navigation touch-safe and keyboard visible', () => {
    expect(breadcrumb).toContain('width: 44px')
    expect(breadcrumb).toContain('height: 44px')
    expect(breadcrumb).toContain('.app-breadcrumb__back:focus-visible')
    expect(breadcrumb).toContain('.app-breadcrumb__link:focus-visible')
  })

  it('uses design-system typography and radius tokens in shared shell components', () => {
    for (const component of [header, breadcrumb, footer]) {
      expect(component).toMatch(/--st-font-(?:xs|sm|md|lg|xl)/)
    }

    expect(header).toContain('var(--st-radius-control)')
    expect(breadcrumb).toContain('var(--st-radius-control)')
  })
})
