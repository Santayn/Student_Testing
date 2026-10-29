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

describe('scrollbar design contract', () => {
  const tokens = source('src/theme/tokens.css')
  const base = source('src/theme/base.css')
  const sidebar = source(
    'src/components/layout/AppSidebar.vue'
  )
  const header = source(
    'src/components/layout/AppHeader.vue'
  )
  const breadcrumb = source(
    'src/components/layout/AppBreadcrumb.vue'
  )
  const foundation = source(
    'src/theme/foundation.css'
  )

  it('defines shared workspace and shell scrollbar tokens', () => {
    expect(tokens).toContain(
      '--st-scrollbar-size: 8px'
    )
    expect(tokens).toContain(
      '--st-scrollbar-thumb:'
    )
    expect(tokens).toContain(
      '--st-scrollbar-thumb-hover:'
    )
    expect(tokens).toContain(
      '--st-scrollbar-shell-thumb:'
    )
  })

  it('provides a reusable scrollbar utility without styling the document scrollbar', () => {
    expect(base).toContain('.st-scrollbar {')
    expect(base).toContain('.st-scrollbar--shell')

    expect(base).not.toMatch(
      /html::?-webkit-scrollbar|body::?-webkit-scrollbar/
    )
  })

  it('uses the shared contract in shell and horizontal navigation scroll regions', () => {
    expect(sidebar).toContain(
      'st-scrollbar st-scrollbar--shell'
    )
    expect(header).toContain(
      'st-scrollbar st-scrollbar--shell'
    )
    expect(breadcrumb).toContain('st-scrollbar')
  })

  it('keeps the sidebar collapse control fixed above its independently scrolling menu', () => {
    expect(sidebar).toMatch(/class="app-sidebar__toolbar"[\s\S]*?class="app-sidebar__collapse"/)
    expect(sidebar).toContain('class="app-sidebar__nav st-scrollbar st-scrollbar--shell st-scrollbar--sidebar"')
    expect(sidebar).toMatch(/\.app-sidebar__toolbar\s*\{[^}]*position:\s*sticky;[^}]*top:\s*0;/)
    expect(sidebar).toMatch(/\.app-sidebar__nav\s*\{[^}]*overflow-y:\s*auto;/)
    expect(sidebar).toMatch(/\.app-sidebar\s*\{[^}]*overflow:\s*hidden;/)
  })

  it('uses translucent theme-derived scrollbar colors only inside the sidebar', () => {
    expect(tokens).toContain('--st-scrollbar-sidebar-thumb: color-mix(')
    expect(tokens).toContain('--st-scrollbar-sidebar-thumb-hover: color-mix(')
    expect(base).toContain('.st-scrollbar--sidebar {')
    expect(base).toContain('scrollbar-color: var(--st-scrollbar-sidebar-thumb) transparent')
    expect(base).toContain('.st-scrollbar--sidebar::-webkit-scrollbar-thumb:hover')
    expect(base).not.toMatch(/html::?-webkit-scrollbar|body::?-webkit-scrollbar/)
  })

  it('covers common PrimeVue and table internal scrollers', () => {
    expect(foundation).toContain(
      '.p-select-list-container::-webkit-scrollbar'
    )
    expect(foundation).toContain(
      '.st-ui-table-scroll::-webkit-scrollbar'
    )
    expect(foundation).toContain(
      '.st-ui-drawer.p-drawer .p-drawer-content::-webkit-scrollbar'
    )
    expect(foundation).toContain(
      '.st-ui-dialog.p-dialog .p-dialog-content::-webkit-scrollbar'
    )
  })
})
