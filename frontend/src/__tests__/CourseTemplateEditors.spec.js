import { ref } from 'vue'

import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  useCourseTemplateEditors,
} from '@/composables/useCourseTemplateEditors'

function setup(overrides = {}) {
  const versions = ref([
    { id: 1, versionNumber: 1, title: 'Первая' },
    { id: 3, versionNumber: 3, title: 'Третья' },
  ])
  const selectedTemplate = ref({ id: 10, name: 'Базовый поток' })
  const selectedSubjectId = ref(7)
  const canWorkWithTemplates = ref(true)
  const templateCreationAllowed = ref(true)
  const notice = ref({ type: 'info', message: '' })

  const state = useCourseTemplateEditors({
    versions,
    selectedTemplate,
    selectedSubjectId,
    canWorkWithTemplates,
    templateCreationAllowed,
    notice,
    ...overrides,
  })

  return {
    versions,
    selectedTemplate,
    selectedSubjectId,
    canWorkWithTemplates,
    templateCreationAllowed,
    notice,
    ...state,
  }
}

describe('course template editor state', () => {
  it('owns template overlay state and dirty close confirmation', () => {
    const state = setup()

    state.openCreateTemplate()

    expect(state.templateOverlay.isOpen.value).toBe(true)
    expect(state.templateOverlay.isCreate.value).toBe(true)
    expect(state.templateDrawerTitle.value).toBe('Новый шаблон курса')
    expect(state.templateOverlay.form).toMatchObject({
      id: null,
      name: '',
      publicVisible: true,
    })
    expect(state.templateOverlay.dirty.value).toBe(false)

    state.templateOverlay.form.name = 'Новый шаблон'
    expect(state.templateOverlay.dirty.value).toBe(true)
    expect(state.requestTemplateDrawerClose()).toBe(false)
    expect(state.templateOverlay.confirmCloseVisible.value).toBe(true)

    state.discardTemplateDrawer()
    expect(state.templateOverlay.isOpen.value).toBe(false)
  })

  it('keeps template and version drawers mutually exclusive', () => {
    const state = setup()

    state.openEditTemplate({
      id: 55,
      name: 'Редактируемый',
      publicVisible: false,
    })
    expect(state.templateOverlay.isOpen.value).toBe(true)

    state.openCreateVersion()

    expect(state.templateOverlay.isOpen.value).toBe(false)
    expect(state.versionOverlay.isOpen.value).toBe(true)
    expect(state.versionOverlay.isCreate.value).toBe(true)
    expect(state.versionOverlay.form).toMatchObject({
      id: null,
      versionNumber: 4,
      title: 'Базовый поток',
      published: false,
    })

    state.openEditTemplate({ id: 56, name: 'Другой', publicVisible: true })
    expect(state.versionOverlay.isOpen.value).toBe(false)
    expect(state.templateOverlay.isOpen.value).toBe(true)
  })

  it('preserves template validation and create restrictions', () => {
    const state = setup()

    state.openCreateTemplate()
    expect(state.templateValidationMessage()).toBe('Введите название шаблона.')

    state.templateOverlay.form.name = 'x'.repeat(201)
    expect(state.templateValidationMessage()).toContain('200 символов')

    state.templateOverlay.form.name = 'Корректный шаблон'
    expect(state.templateValidationMessage()).toBe('')

    state.closeTemplateDrawerImmediately()
    state.canWorkWithTemplates.value = false
    state.openCreateTemplate()
    expect(state.templateOverlay.isOpen.value).toBe(false)
    expect(state.notice.value).toEqual({
      type: 'danger',
      message: 'Выберите предмет преподавателя.',
    })

    state.canWorkWithTemplates.value = true
    state.templateCreationAllowed.value = false
    state.openCreateTemplate()
    expect(state.templateOverlay.isOpen.value).toBe(false)
    expect(state.notice.value).toEqual({
      type: 'info',
      message: 'Новый шаблон должен создать сам преподаватель.',
    })
  })

  it('preserves version validation including duplicate numbers', () => {
    const state = setup()
    state.openCreateVersion()

    state.versionOverlay.form.versionNumber = 0
    expect(state.versionValidationMessage()).toContain('больше нуля')

    state.versionOverlay.form.versionNumber = 3
    expect(state.versionValidationMessage()).toContain('уже существует')

    state.versionOverlay.form.versionNumber = 4
    state.versionOverlay.form.title = ''
    expect(state.versionValidationMessage()).toBe('Введите название версии.')

    state.versionOverlay.form.title = 'Версия 4'
    expect(state.versionValidationMessage()).toBe('')

    state.openEditVersion({
      id: 3,
      versionNumber: 3,
      title: 'Третья',
      description: '',
      changeNotes: '',
      published: true,
    })
    expect(state.versionValidationMessage()).toBe('')
  })
})
