// @vitest-environment node

import {
  readFileSync,
} from 'node:fs'
import {
  resolve,
} from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const view = readFileSync(
  resolve(
    process.cwd(),
    'src/views/HomeView.vue'
  ),
  'utf8'
)

describe('Home navigation responsibility', () => {
  it('does not duplicate global Sidebar destinations', () => {
    expect(view).not.toContain('<RouterLink')
    expect(view).not.toContain('commonActions')
    expect(view).not.toContain('teacherActions')
    expect(view).not.toContain('adminActions')
    expect(view).not.toContain('action-grid')
    expect(view).not.toContain('action-card')
  })

  it('keeps Home as a workspace overview rather than a second menu', () => {
    expect(view).toContain('workspaceSummary')
    expect(view).toContain('Текущий режим')
    expect(view).toContain('Текущая сессия')
    expect(view).toContain('Для перехода между разделами используйте Sidebar слева.')
  })

  it('keeps refresh as an action without introducing page navigation', () => {
    expect(view).toContain('async function refreshUser()')
    expect(view).toContain('@click="refreshUser"')
    expect(view).toContain('Обновить данные')
  })
})
