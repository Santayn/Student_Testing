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

describe('UI Stage 1 foundation contracts', () => {
  it('uses readable muted text and a separate subtle token', () => {
    const tokens = source('theme', 'tokens.css')

    expect(tokens).toContain('--st-text-muted: #64748b')
    expect(tokens).toContain('--st-text-subtle: #94a3b8')
    expect(tokens).toContain('--st-font-page-title')
    expect(tokens).toContain('--st-line-relaxed')
  })

  it('centralizes workspace page headers', () => {
    const shell = source(
      'components',
      'layout',
      'WorkspacePageShell.vue'
    )

    expect(shell).toContain('workspace-page-shell__header')
    expect(shell).toContain('var(--st-font-page-title)')
    expect(shell).toContain('var(--st-radius-card)')
    expect(shell).toContain('min-height: 44px')
  })

  it('separates loading from empty states', () => {
    const loadingState = source(
      'components',
      'ui',
      'UiLoadingState.vue'
    )

    expect(loadingState).toContain('role="status"')
    expect(loadingState).toContain('aria-live="polite"')
    expect(loadingState).toContain('aria-busy="true"')

    const results = source(
      'views',
      'results',
      'ResultsView.vue'
    )

    expect(results).toContain('UiLoadingState')
    expect(results).toContain('Загрузка результатов...')
  })

  it('provides one reusable stat pattern', () => {
    const stat = source('components', 'ui', 'UiStat.vue')
    const grid = source('components', 'ui', 'UiStatGrid.vue')

    expect(stat).toContain('st-ui-stat__label')
    expect(stat).toContain('st-ui-stat__value')
    expect(grid).toContain('st-ui-stat-grid')
  })

  it('keeps mobile global header targets at least 44px', () => {
    const header = source(
      'components',
      'layout',
      'AppHeader.vue'
    )

    expect(header).toContain('width: 44px')
    expect(header).toContain('height: 44px')
    expect(header).toContain('min-height: 44px')
  })
})
