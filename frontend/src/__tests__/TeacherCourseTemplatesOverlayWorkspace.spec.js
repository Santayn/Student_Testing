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

const courseTemplateEditors = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'composables',
    'useCourseTemplateEditors.js'
  ),
  'utf8'
)

const courseTemplateDrawer = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'components',
    'teacher',
    'CourseTemplateEditorDrawer.vue'
  ),
  'utf8'
)

const courseVersionDrawer = readFileSync(
  resolve(
    process.cwd(),
    'src',
    'components',
    'teacher',
    'CourseVersionEditorDrawer.vue'
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

  it('moves template and version editing into dedicated drawer components and shared editor state', () => {
    expect(courseTemplates).toContain('useCourseTemplateEditors')
    expect(courseTemplates).toContain('<CourseTemplateEditorDrawer')
    expect(courseTemplates).toContain('<CourseVersionEditorDrawer')
    expect(courseTemplateEditors).toContain('useOverlayForm')
    expect(courseTemplateEditors).toContain('templateOverlay')
    expect(courseTemplateEditors).toContain('versionOverlay')
    expect(courseTemplateEditors).toContain('openCreateTemplate')
    expect(courseTemplateEditors).toContain('openEditTemplate')
    expect(courseTemplateEditors).toContain('openCreateVersion')
    expect(courseTemplateEditors).toContain('openEditVersion')
    expect(courseTemplateDrawer).toContain('<UiDrawer')
    expect(courseVersionDrawer).toContain('<UiDrawer')
    expect(courseTemplates).toContain('UiUnsavedChangesConfirm')
  })

  it('prevents nested working drawers by closing the other editor before opening one', () => {
    const createVersionStart = courseTemplateEditors.indexOf('function openCreateVersion()')
    const createVersionEnd = courseTemplateEditors.indexOf('function openEditVersion')
    const createVersionSource = courseTemplateEditors.slice(createVersionStart, createVersionEnd)

    const editTemplateStart = courseTemplateEditors.indexOf('function openEditTemplate(template)')
    const editTemplateEnd = courseTemplateEditors.indexOf('function requestTemplateDrawerClose')
    const editTemplateSource = courseTemplateEditors.slice(editTemplateStart, editTemplateEnd)

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
