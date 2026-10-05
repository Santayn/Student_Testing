// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

const sourcePath = (...segments) =>
  resolve(process.cwd(), 'src', ...segments)

const foundationSource = readFileSync(
  sourcePath('theme', 'foundation.css'),
  'utf8'
)

const uiTableSource = readFileSync(
  sourcePath('components', 'ui', 'UiTable.vue'),
  'utf8'
)

const uiFileInputSource = readFileSync(
  sourcePath('components', 'ui', 'UiFileInput.vue'),
  'utf8'
)

describe('Ui mobile responsiveness contracts', () => {
  it('keeps mobile touch targets and iOS-safe form font sizes', () => {
    expect(foundationSource).toContain('@media (max-width: 640px)')
    expect(foundationSource).toContain('min-height: 44px')
    expect(foundationSource).toContain('font-size: 16px')
    expect(foundationSource).toContain('.st-ui-button--sm.p-button')
    expect(foundationSource).toContain('.p-select-option')
  })

  it('wraps UiTable in a horizontally scrollable accessible region', () => {
    expect(uiTableSource).toContain('class="st-ui-table-scroll"')
    expect(uiTableSource).toContain('role="region"')
    expect(uiTableSource).toContain(':aria-label="ariaLabel"')
    expect(uiTableSource).toContain('tabindex="0"')
    expect(foundationSource).toContain('overflow-x: auto')
    expect(foundationSource).toContain('width: max-content')
  })

  it('stacks toolbars and constrains overlays to the mobile viewport', () => {
    expect(foundationSource).toContain('flex-direction: column')
    expect(foundationSource).toContain('max-height: calc(100dvh - 1rem)')
    expect(foundationSource).toContain('overscroll-behavior: contain')
    expect(foundationSource).toContain('max-width: calc(100vw - 1rem)')
  })

  it('keeps action menus and toasts touch-safe on narrow screens', () => {
    expect(foundationSource).toContain('.st-ui-action-trigger.p-button')
    expect(foundationSource).toContain('width: min(19rem, calc(100vw - 1rem))')
    expect(foundationSource).toContain('env(safe-area-inset-left)')
    expect(foundationSource).toContain('env(safe-area-inset-right)')
    expect(foundationSource).toContain('100vw - 2.5rem')
  })

  it('keeps native file input inside its container without mobile zoom', () => {
    expect(uiFileInputSource).toContain('@media (max-width: 640px)')
    expect(uiFileInputSource).toContain('font-size: 16px')
    expect(uiFileInputSource).toContain('text-overflow: ellipsis')
    expect(uiFileInputSource).toContain('box-sizing: border-box')
  })
})
