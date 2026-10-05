import { ref } from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

vi.mock('@/api', () => ({
  coursesApi: {
    getTemplates: vi.fn(),
    getVersions: vi.fn(),
  },
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

import {
  coursesApi,
  getApiErrorMessage,
} from '@/api'

import {
  useCourseTemplatesData,
} from '@/composables/course-templates/useCourseTemplatesData'

function deferred() {
  let resolve
  let reject

  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return { promise, resolve, reject }
}

function setup({
  subjectId = 7,
  membership = { id: 31, subjectId: 7, personId: 42 },
  isAdminMode = false,
  personId = 42,
  query = {},
} = {}) {
  const selectedSubjectId = ref(subjectId)
  const selectedMembership = ref(membership)
  const authStore = {
    isAdminMode,
    personId,
  }
  const route = { query }
  const notice = ref({ type: 'info', message: '' })
  const beforeTemplateSelection = vi.fn()
  const onOpenRouteVersion = vi.fn()

  return {
    selectedSubjectId,
    selectedMembership,
    authStore,
    route,
    notice,
    beforeTemplateSelection,
    onOpenRouteVersion,
    ...useCourseTemplatesData({
      selectedSubjectId,
      selectedMembership,
      authStore,
      route,
      notice,
      beforeTemplateSelection,
      onOpenRouteVersion,
    }),
  }
}

describe('course templates data state', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    coursesApi.getTemplates.mockResolvedValue({ data: [] })
    coursesApi.getVersions.mockResolvedValue({ data: [] })
  })

  it('loads templates in the current teacher context and auto-selects a single template', async () => {
    coursesApi.getTemplates.mockResolvedValue({
      data: [{ id: 5, name: 'Базовый курс', publicVisible: true }],
    })
    coursesApi.getVersions.mockResolvedValue({
      data: [{ id: 9, versionNumber: 2 }, { id: 8, versionNumber: 1 }],
    })

    const state = setup()
    await state.loadTemplates()

    expect(coursesApi.getTemplates).toHaveBeenCalledWith({
      subjectId: 7,
      authorPersonId: 42,
    })
    expect(state.selectedTemplateId.value).toBe(5)
    expect(state.beforeTemplateSelection).toHaveBeenCalledTimes(1)
    expect(coursesApi.getVersions).toHaveBeenCalledWith(5)
    expect(state.versions.value.map((item) => item.id)).toEqual([9, 8])
    expect(state.loading.value).toBe(false)
    expect(state.loadingVersions.value).toBe(false)
  })

  it('uses the selected membership person for admin reads', async () => {
    const state = setup({
      isAdminMode: true,
      personId: 999,
      membership: { id: 31, subjectId: 7, personId: 42 },
    })

    await state.loadTemplates()

    expect(coursesApi.getTemplates).toHaveBeenCalledWith({
      subjectId: 7,
      authorPersonId: 42,
    })
  })

  it('ignores a stale template response after the subject context is reloaded', async () => {
    const first = deferred()
    const second = deferred()

    coursesApi.getTemplates
      .mockReturnValueOnce(first.promise)
      .mockReturnValueOnce(second.promise)

    const state = setup({ subjectId: 7 })
    const oldLoad = state.loadTemplates()

    state.selectedSubjectId.value = 8
    state.selectedMembership.value = {
      id: 32,
      subjectId: 8,
      personId: 42,
    }
    const newLoad = state.loadTemplates()

    second.resolve({ data: [{ id: 20, name: 'Новый' }] })
    await newLoad

    first.resolve({ data: [{ id: 10, name: 'Старый' }] })
    await oldLoad

    expect(state.templates.value.map((item) => item.id)).toEqual([20])
  })

  it('opens a route-requested version only once for the same template/version key', async () => {
    coursesApi.getTemplates.mockResolvedValue({
      data: [{ id: 5, name: 'Курс' }],
    })
    coursesApi.getVersions.mockResolvedValue({
      data: [
        { id: 91, versionNumber: 1, title: 'Версия 1' },
        { id: 92, versionNumber: 2, title: 'Версия 2' },
      ],
    })

    const state = setup({
      query: {
        templateId: '5',
        versionId: '92',
      },
    })

    await state.loadTemplates({ openRouteVersion: true })
    await state.loadVersions({ openRouteVersion: true })

    expect(state.onOpenRouteVersion).toHaveBeenCalledTimes(1)
    expect(state.onOpenRouteVersion).toHaveBeenCalledWith(
      expect.objectContaining({ id: 92 })
    )
  })

  it('filters templates and versions locally without additional API reads', async () => {
    coursesApi.getTemplates.mockResolvedValue({
      data: [
        { id: 1, name: 'Алгебра', publicVisible: true },
        { id: 2, name: 'Геометрия', publicVisible: false },
      ],
    })

    const state = setup()
    await state.loadTemplates()

    state.templateVisibilityFilter.value = 'hidden'
    state.templateSearchQuery.value = 'гео'

    expect(state.filteredTemplates.value.map((item) => item.id)).toEqual([2])
    expect(coursesApi.getTemplates).toHaveBeenCalledTimes(1)

    state.versions.value = [
      { id: 3, versionNumber: 2, title: 'Весна', published: true },
      { id: 4, versionNumber: 1, title: 'Осень', published: false },
    ]
    state.versionPublicationFilter.value = 'draft'

    expect(state.filteredVersions.value.map((item) => item.id)).toEqual([4])
    expect(coursesApi.getVersions).not.toHaveBeenCalled()
  })

  it('reports a current read failure through the existing notice surface', async () => {
    coursesApi.getTemplates.mockRejectedValue(new Error('Сеть недоступна'))

    const state = setup()
    await state.loadTemplates()

    expect(state.notice.value).toEqual({
      type: 'danger',
      message: 'Сеть недоступна',
    })
    expect(getApiErrorMessage).toHaveBeenCalled()
    expect(state.loading.value).toBe(false)
  })
})
