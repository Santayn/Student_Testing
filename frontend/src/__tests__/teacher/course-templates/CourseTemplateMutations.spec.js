import { ref } from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const {
  createTemplate,
  updateTemplate,
  removeTemplate,
  createVersion,
  updateVersion,
  publishVersion,
  unpublishVersion,
  getApiErrorMessage,
} = vi.hoisted(() => ({
  createTemplate: vi.fn(),
  updateTemplate: vi.fn(),
  removeTemplate: vi.fn(),
  createVersion: vi.fn(),
  updateVersion: vi.fn(),
  publishVersion: vi.fn(),
  unpublishVersion: vi.fn(),
  getApiErrorMessage: vi.fn((error, fallback) => error?.message || fallback),
}))

vi.mock('@/api', () => ({
  coursesApi: {
    createTemplate,
    updateTemplate,
    removeTemplate,
    createVersion,
    updateVersion,
    publishVersion,
    unpublishVersion,
  },
  getApiErrorMessage,
}))

import {
  useCourseTemplateMutations,
} from '@/composables/course-templates/useCourseTemplateMutations'

function overlay(form) {
  return {
    form,
    saving: ref(false),
    beginSaving: vi.fn(),
    finishSaving: vi.fn(),
    failSaving: vi.fn(),
  }
}

function createState(overrides = {}) {
  const templateOverlay = overlay({
    id: null,
    name: '  Основной шаблон  ',
    publicVisible: true,
  })

  const versionOverlay = overlay({
    id: null,
    versionNumber: 2,
    title: '  Версия 2  ',
    description: '  Описание  ',
    changeNotes: '  Изменения  ',
    published: false,
  })

  const state = useCourseTemplateMutations({
    selectedSubjectId: ref(7),
    selectedTemplateId: ref(11),
    versions: ref([{ id: 101 }]),
    templateOverlay,
    versionOverlay,
    templateFormError: ref(''),
    versionFormError: ref(''),
    templateCreationAllowed: ref(true),
    templateValidationMessage: vi.fn(() => ''),
    versionValidationMessage: vi.fn(() => ''),
    ensureSelectedMembershipActive: vi.fn().mockResolvedValue({ id: 5 }),
    loadTemplates: vi.fn().mockResolvedValue(undefined),
    loadVersions: vi.fn().mockResolvedValue(undefined),
    closeTemplateDrawerImmediately: vi.fn(),
    closeVersionDrawerImmediately: vi.fn(),
    notice: ref({ type: 'info', message: '' }),
    ...overrides,
  })

  return {
    state,
    templateOverlay,
    versionOverlay,
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('course template mutations', () => {
  it('owns a complete delete-dialog lifecycle', () => {
    const { state } = createState()
    const template = { id: 15, name: 'Шаблон' }

    state.requestDeleteTemplate(template)

    expect(state.deleteTarget.value).toEqual(template)
    expect(state.deleteConfirmVisible.value).toBe(true)

    state.deleteError.value = 'Ошибка'
    state.closeDeleteDialog()

    expect(state.deleteTarget.value).toBeNull()
    expect(state.deleteConfirmVisible.value).toBe(false)
    expect(state.deleteError.value).toBe('')
  })


  it('ignores repeated template and version saves while a mutation is pending', async () => {
    const { state, templateOverlay, versionOverlay } = createState()
    templateOverlay.saving.value = true
    versionOverlay.saving.value = true

    expect(await state.saveTemplate()).toBe(false)
    expect(await state.saveVersion()).toBe(false)

    expect(templateOverlay.beginSaving).not.toHaveBeenCalled()
    expect(versionOverlay.beginSaving).not.toHaveBeenCalled()
    expect(createTemplate).not.toHaveBeenCalled()
    expect(updateTemplate).not.toHaveBeenCalled()
    expect(createVersion).not.toHaveBeenCalled()
    expect(updateVersion).not.toHaveBeenCalled()
  })

  it('creates a template only after membership revalidation and preserves the backend payload', async () => {
    const ensureSelectedMembershipActive = vi.fn().mockResolvedValue({ id: 5 })
    const loadTemplates = vi.fn().mockResolvedValue(undefined)
    createTemplate.mockResolvedValue({ data: { id: 31 } })

    const { state, templateOverlay } = createState({
      ensureSelectedMembershipActive,
      loadTemplates,
    })

    await state.saveTemplate()

    expect(ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(createTemplate).toHaveBeenCalledWith({
      subjectId: 7,
      name: 'Основной шаблон',
      publicVisible: true,
    })
    expect(templateOverlay.finishSaving).toHaveBeenCalledWith({ close: true })
    expect(loadTemplates).toHaveBeenCalledWith({ preferredTemplateId: 31 })
  })

  it('deletes the selected template and clears its local version context', async () => {
    const selectedTemplateId = ref(11)
    const versions = ref([{ id: 101 }])
    const closeVersionDrawerImmediately = vi.fn()
    const loadTemplates = vi.fn().mockResolvedValue(undefined)
    removeTemplate.mockResolvedValue({})

    const { state } = createState({
      selectedTemplateId,
      versions,
      closeVersionDrawerImmediately,
      loadTemplates,
    })

    state.requestDeleteTemplate({ id: 11, name: 'Удаляемый' })
    await state.deleteTemplate()

    expect(removeTemplate).toHaveBeenCalledWith(11)
    expect(selectedTemplateId.value).toBeNull()
    expect(versions.value).toEqual([])
    expect(closeVersionDrawerImmediately).toHaveBeenCalledTimes(1)
    expect(state.deleteConfirmVisible.value).toBe(false)
    expect(state.deleteTarget.value).toBeNull()
    expect(loadTemplates).toHaveBeenCalledTimes(1)
  })


  it('ignores repeated publication changes while another publication mutation is pending', async () => {
    const { state } = createState()
    state.publishingVersionId.value = 44

    expect(await state.publishVersion({ id: 45, published: false })).toBe(false)

    expect(publishVersion).not.toHaveBeenCalled()
    expect(unpublishVersion).not.toHaveBeenCalled()
  })

  it('publishes a version after membership revalidation and refreshes versions', async () => {
    const ensureSelectedMembershipActive = vi.fn().mockResolvedValue({ id: 5 })
    const loadVersions = vi.fn().mockResolvedValue(undefined)
    publishVersion.mockResolvedValue({})

    const { state } = createState({
      ensureSelectedMembershipActive,
      loadVersions,
    })

    await state.publishVersion({ id: 44, published: false })

    expect(ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    expect(publishVersion).toHaveBeenCalledWith(44)
    expect(loadVersions).toHaveBeenCalledTimes(1)
    expect(state.publishingVersionId.value).toBeNull()
  })

  it('keeps version creation bound to the template selected when save started', async () => {
    let releaseMembership
    const ensureSelectedMembershipActive = vi.fn(() => new Promise((resolve) => {
      releaseMembership = () => resolve({ id: 5 })
    }))
    const selectedTemplateId = ref(11)
    const loadVersions = vi.fn().mockResolvedValue(undefined)
    const notice = ref({ type: 'info', message: '' })
    createVersion.mockResolvedValue({})

    const { state } = createState({
      ensureSelectedMembershipActive,
      selectedTemplateId,
      loadVersions,
      notice,
    })

    const saving = state.saveVersion()

    await vi.waitFor(() => {
      expect(ensureSelectedMembershipActive).toHaveBeenCalledTimes(1)
    })

    selectedTemplateId.value = 22
    releaseMembership()
    await saving

    expect(createVersion).toHaveBeenCalledWith(
      11,
      expect.objectContaining({
        versionNumber: 2,
        title: 'Версия 2',
        published: false,
      })
    )
    expect(loadVersions).not.toHaveBeenCalled()
    expect(notice.value.message).toBe('')
  })

  it('does not apply publication side effects to a different selected template', async () => {
    let releasePublish
    const selectedTemplateId = ref(11)
    const loadVersions = vi.fn().mockResolvedValue(undefined)
    const notice = ref({ type: 'info', message: '' })
    publishVersion.mockImplementation(() => new Promise((resolve) => {
      releasePublish = () => resolve({})
    }))

    const { state } = createState({
      selectedTemplateId,
      loadVersions,
      notice,
    })

    const publishing = state.publishVersion({ id: 44, published: false })

    await vi.waitFor(() => {
      expect(publishVersion).toHaveBeenCalledWith(44)
    })

    selectedTemplateId.value = 22
    releasePublish()
    await publishing

    expect(loadVersions).not.toHaveBeenCalled()
    expect(notice.value.message).toBe('')
    expect(state.publishingVersionId.value).toBeNull()
  })

})
