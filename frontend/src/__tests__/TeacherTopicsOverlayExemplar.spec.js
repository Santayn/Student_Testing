// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const topicLibrary = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'teacher',
    'TopicLibraryView.vue'
  ),
  'utf8'
)

describe('teacher topics overlay exemplar', () => {
  it('keeps the page as a searchable workspace instead of an inline editor', () => {
    expect(topicLibrary).toContain('UiFilterBar')
    expect(topicLibrary).toContain('filteredTopics')
    expect(topicLibrary).toContain('descriptionFilter')
    expect(topicLibrary).toContain('sortMode')
    expect(topicLibrary).toContain('resetFilters')
    expect(topicLibrary).not.toContain('UiTable')
  })

  it('creates and edits topics through the shared overlay lifecycle', () => {
    expect(topicLibrary).toContain('useOverlayForm')
    expect(topicLibrary).toContain('topicDialogModel')
    expect(topicLibrary).toContain('openCreateTopic')
    expect(topicLibrary).toContain('openEditTopic')
    expect(topicLibrary).toContain('UiUnsavedChangesConfirm')
    expect(topicLibrary).toContain('requestClose')
  })

  it('uses an application dialog instead of window.confirm for deletion', () => {
    expect(topicLibrary).toContain('deleteConfirmVisible')
    expect(topicLibrary).toContain('title="Удалить тему?"')
    expect(topicLibrary).toContain('requestDeleteTopic')
    expect(topicLibrary).not.toContain('window.confirm')
  })

  it('preserves membership revalidation immediately before mutations', () => {
    const guards = topicLibrary.match(
      /ensureSelectedMembershipActive\(\)/g
    ) ?? []

    expect(guards.length).toBeGreaterThanOrEqual(2)
    expect(topicLibrary).toContain('topicsApi.create(payload)')
    expect(topicLibrary).toContain('topicsApi.update(form.id, payload)')
    expect(topicLibrary).toContain('topicsApi.remove(topic.id)')
  })

  it('preserves deep links and contextual transitions', () => {
    expect(topicLibrary).toContain('resolveRouteTopic')
    expect(topicLibrary).toContain('route.query.topicId')
    expect(topicLibrary).toContain("name: 'teacher-questions'")
    expect(topicLibrary).toContain("name: 'teacher-test-create'")
  })

  it('keeps filters outside the CRUD lifecycle so saving does not reset them', () => {
    const saveStart = topicLibrary.indexOf('async function saveTopic()')
    const saveEnd = topicLibrary.indexOf('async function deleteTopic()')
    const saveSource = topicLibrary.slice(saveStart, saveEnd)

    expect(saveSource).not.toContain('resetFilters()')
    expect(saveSource).toContain('await loadTopics()')
    expect(saveSource).toContain('finishSaving({ close: true })')
  })
})
