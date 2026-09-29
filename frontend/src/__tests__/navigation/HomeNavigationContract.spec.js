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
  it('keeps role-specific Home actions scoped to the current workspace', () => {
    expect(view).not.toContain('<RouterLink')
    expect(view).not.toContain('commonActions')
    expect(view).not.toContain('teacherActions')
    expect(view).not.toContain('adminActions')
    expect(view).not.toContain('action-grid')
    expect(view).not.toContain('action-card')
    expect(view).toContain('studentActions')
    expect(view).toContain("route: { name: 'subjects' }")
    expect(view).toContain("route: { name: 'results' }")
    expect(view).toContain("route: { name: 'profile' }")
  })

  it('uses a task-oriented student landing without explaining Sidebar mechanics', () => {
    expect(view).toContain('workspaceSummary')
    expect(view).toContain('Текущий режим')
    expect(view).toContain('Текущая сессия')
    expect(view).toContain('Продолжайте обучение')
    expect(view).not.toContain('Для перехода между разделами используйте Sidebar слева.')
  })

  it('keeps identity refresh as a secondary session action', () => {
    expect(view).toContain('async function refreshUser()')
    expect(view).toContain('@click="refreshUser"')
    expect(view).toContain('Обновить данные')
  })
})
