// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

const sidebar = source('src/components/layout/AppSidebar.vue')
const group = source('src/components/layout/NavigationDisclosureGroup.vue')
const sheet = source('src/components/layout/NavigationMoreSheet.vue')
const base = source('src/theme/base.css')

describe('navigation geometry stays stable', () => {
  it('keeps sidebar height stable and scrolls the links, not the toolbar', () => {
    expect(sidebar).toMatch(/\.app-sidebar\s*\{[^}]*height:\s*calc\(100dvh - 104px\)/)
    expect(sidebar).toMatch(/\.app-sidebar__nav\s*\{[^}]*overflow-y:\s*auto;/)
    expect(sidebar).toMatch(/\.app-sidebar__toolbar\s*\{[^}]*flex:\s*0 0 auto;/)
  })

  it('expands groups to their intrinsic size without a fixed height cap', () => {
    expect(group).toContain('navigation-group__items-inner')
    expect(group).toMatch(/grid-template-rows:\s*1fr/)
    expect(group).toMatch(/grid-template-rows:\s*0fr/)
    expect(group).not.toContain('max-height: 520px')
    expect(group).toContain('v-if="expanded"')
  })

  it('reserves root scrollbar space only for actually scrolling pages', () => {
    expect(base).toMatch(/html\s*\{[^}]*scrollbar-gutter:\s*auto/)
    expect(base).toMatch(/html\.st-document-scrollable\s*\{[^}]*scrollbar-gutter:\s*stable/)
    expect(base).toMatch(/html:has\(body\.p-overflow-hidden\)\s*\{[^}]*overflow:\s*hidden/)
    expect(sheet).toContain("document.documentElement.classList.add('navigation-sheet-open')")
    expect(sheet).toContain("document.documentElement.classList.remove('navigation-sheet-open')")
    expect(sheet).toMatch(/\.navigation-sheet\s*\{[^}]*height:\s*min\(82dvh, 760px\)/)
    expect(sheet).toMatch(/\.navigation-sheet__body\s*\{[^}]*overflow-y:\s*auto/)
    expect(sheet).toContain('class="navigation-sheet__body st-scrollbar st-scrollbar--shell"')
  })
})
