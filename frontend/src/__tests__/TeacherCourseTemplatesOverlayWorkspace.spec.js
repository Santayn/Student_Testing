// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

const courseTemplates = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'views',
    'teacher',
    'CourseTemplatesView.vue'
  ),
  'utf8'
)

describe('teacher course templates overlay workspace', () => {
  it('keeps templates and versions as searchable workspace lists', () => {
    expect(courseTemplates).toContain('UiFilterBar')
    expect(courseTemplates).toContain('filteredTemplates')
    expect(courseTemplates).toContain('filteredVersions')
    expect(courseTemplates).toContain('templateVisibilityFilter')
    expect(courseTemplates).toContain('versionPublicationFilter')
    expect(courseTemplates).toContain('templateSortMode')
    expect(courseTemplates).toContain('versionSortMode')
    expect(courseTemplates).not.toContain('UiTable')
  })

  it('moves template and version editing into shared drawers', () => {
    expect(courseTemplates).toContain('UiDrawer')
    expect(courseTemplates).toContain('useOverlayForm')
    expect(courseTemplates).toContain('templateOverlay')
    expect(courseTemplates).toContain('versionOverlay')
    expect(courseTemplates).toContain('openCreateTemplate')
    expect(courseTemplates).toContain('openEditTemplate')
    expect(courseTemplates).toContain('openCreateVersion')
    expect(courseTemplates).toContain('openEditVersion')
    expect(courseTemplates).toContain('UiUnsavedChangesConfirm')
  })

  it('prevents nested working drawers by closing the other editor before opening one', () => {
    const createVersionStart = courseTemplates.indexOf('function openCreateVersion()')
    const createVersionEnd = courseTemplates.indexOf('function openEditVersion')
    const createVersionSource = courseTemplates.slice(createVersionStart, createVersionEnd)

    const editTemplateStart = courseTemplates.indexOf('function openEditTemplate(template)')
    const editTemplateEnd = courseTemplates.indexOf('function requestTemplateDrawerClose')
    const editTemplateSource = courseTemplates.slice(editTemplateStart, editTemplateEnd)

    expect(createVersionSource).toContain('closeTemplateDrawerImmediately()')
    expect(editTemplateSource).toContain('closeVersionDrawerImmediately()')
  })

  it('uses an application dialog instead of browser confirmation for template deletion', () => {
    expect(courseTemplates).toContain('deleteConfirmVisible')
    expect(courseTemplates).toContain('title="Удалить шаблон курса?"')
    expect(courseTemplates).toContain('requestDeleteTemplate')
    expect(courseTemplates).not.toContain('window.confirm')
  })

  it('preserves backend contracts and membership revalidation before mutations', () => {
    const guards = courseTemplates.match(
      /ensureSelectedMembershipActive\(\)/g
    ) ?? []

    expect(guards.length).toBeGreaterThanOrEqual(4)
    expect(courseTemplates).toContain('coursesApi.createTemplate(payload)')
    expect(courseTemplates).toContain('coursesApi.updateTemplate(editingId, payload)')
    expect(courseTemplates).toContain('coursesApi.removeTemplate(template.id)')
    expect(courseTemplates).toContain('coursesApi.createVersion(')
    expect(courseTemplates).toContain('coursesApi.updateVersion(editingId, basePayload)')
    expect(courseTemplates).toContain('coursesApi.publishVersion(version.id)')
    expect(courseTemplates).toContain('coursesApi.unpublishVersion(version.id)')
  })

  it('does not invent unsupported version deletion and keeps contextual lecture navigation', () => {
    expect(courseTemplates).not.toContain('removeVersion')
    expect(courseTemplates).toContain("name: 'teacher-lectures'")
    expect(courseTemplates).toContain('templateId')
    expect(courseTemplates).toContain('versionId')
  })

  it('keeps filter state independent from save flows', () => {
    const saveTemplateStart = courseTemplates.indexOf('async function saveTemplate()')
    const saveTemplateEnd = courseTemplates.indexOf('async function deleteTemplate()')
    const templateSaveSource = courseTemplates.slice(saveTemplateStart, saveTemplateEnd)

    const saveVersionStart = courseTemplates.indexOf('async function saveVersion()')
    const saveVersionEnd = courseTemplates.indexOf('async function publishVersion')
    const versionSaveSource = courseTemplates.slice(saveVersionStart, saveVersionEnd)

    expect(templateSaveSource).not.toContain('resetTemplateFilters()')
    expect(versionSaveSource).not.toContain('resetVersionFilters()')
  })
})
