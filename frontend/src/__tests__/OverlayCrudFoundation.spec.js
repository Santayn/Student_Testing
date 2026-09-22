// @vitest-environment node

import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

import { describe, expect, it } from 'vitest'

import { useOverlayForm } from '@/composables/useOverlayForm'

const sourcePath = (...segments) => resolve(process.cwd(), 'src', ...segments)

const filterBarSource = readFileSync(
  sourcePath('components', 'ui', 'UiFilterBar.vue'),
  'utf8'
)
const confirmSource = readFileSync(
  sourcePath('components', 'ui', 'UiUnsavedChangesConfirm.vue'),
  'utf8'
)
const foundationSource = readFileSync(
  sourcePath('theme', 'foundation.css'),
  'utf8'
)

describe('Teacher CRUD overlay foundation', () => {
  it('tracks create/edit mode and dirty state without knowing entity shape', () => {
    const overlay = useOverlayForm({
      createDefault: () => ({ title: '', tags: [] }),
    })

    overlay.openCreate({ title: 'Новая тема' })
    expect(overlay.isOpen.value).toBe(true)
    expect(overlay.isCreate.value).toBe(true)
    expect(overlay.dirty.value).toBe(false)

    overlay.form.title = 'Изменённая тема'
    expect(overlay.dirty.value).toBe(true)
    expect(overlay.requestClose()).toBe(false)
    expect(overlay.confirmCloseVisible.value).toBe(true)
    expect(overlay.isOpen.value).toBe(true)

    overlay.continueEditing()
    expect(overlay.confirmCloseVisible.value).toBe(false)
    expect(overlay.isOpen.value).toBe(true)

    overlay.discardAndClose()
    expect(overlay.isOpen.value).toBe(false)
    expect(overlay.form.title).toBe('Новая тема')
  })

  it('keeps an overlay open while saving and marks successful values clean', () => {
    const overlay = useOverlayForm({ createDefault: () => ({ title: '' }) })
    overlay.openEdit({ title: 'Тема' })
    overlay.form.title = 'Тема 2'
    overlay.beginSaving()

    expect(overlay.saving.value).toBe(true)
    expect(overlay.requestClose()).toBe(false)
    expect(overlay.isOpen.value).toBe(true)

    overlay.finishSaving({ close: false })
    expect(overlay.saving.value).toBe(false)
    expect(overlay.dirty.value).toBe(false)
    expect(overlay.isOpen.value).toBe(true)
  })

  it('exposes a filter workspace contract with reset, filters and actions', () => {
    expect(filterBarSource).toContain('st-ui-filter-bar')
    expect(filterBarSource).toContain('<UiSearchInput')
    expect(filterBarSource).toContain('<slot name="filters" />')
    expect(filterBarSource).toContain('<slot name="actions" />')
    expect(filterBarSource).toContain("emit('reset')")
    expect(filterBarSource).toContain('Найдено: ${props.resultCount}')
  })

  it('uses a dedicated confirmation dialog for destructive overlay close', () => {
    expect(confirmSource).toContain('<UiDialog')
    expect(confirmSource).toContain(':closable="false"')
    expect(confirmSource).toContain(':close-on-escape="false"')
    expect(confirmSource).toContain("emit('discard')")
    expect(confirmSource).toContain("emit('continue')")
  })

  it('stacks the filter workspace on mobile', () => {
    expect(foundationSource).toContain('.st-ui-filter-bar__controls')
    expect(foundationSource).toContain('.st-ui-filter-bar__filters')
    expect(foundationSource).toContain('@media (max-width: 640px)')
    expect(foundationSource).toContain('grid-template-columns: 1fr')
  })

  it('keeps the overlay foundation entity-agnostic', () => {
    const source = [filterBarSource, confirmSource].join('\n')

    expect(source).not.toContain('topicsApi')
    expect(source).not.toContain('questionsApi')
    expect(source).not.toContain('lecturesApi')
    expect(source).not.toContain('subjectMembershipId')
  })
})
