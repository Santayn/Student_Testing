import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  flushPromises,
  shallowMount,
} from '@vue/test-utils'

const state = vi.hoisted(() => ({
  auth: {
    isStudent: true,
    isTeacher: true,
    isAdmin: false,
    isStudentMode: false,
    isTeacherMode: true,
    isAdminMode: false,
  },
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.auth,
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: (_error, fallback) => fallback,
  lecturesApi: {
    getAll: vi.fn(),
    getTests: vi.fn(),
  },
  subjectsApi: {
    getAll: vi.fn(),
  },
  resultsApi: {
    getStudentSubjects: vi.fn(),
    getStudentData: vi.fn(),
    getTeacherSubjects: vi.fn(),
    getTeacherLectures: vi.fn(),
    getTeacherTests: vi.fn(),
    getTeacherGroups: vi.fn(),
    getTeacherStudents: vi.fn(),
    getTeacherData: vi.fn(),
  },
}))

import { resultsApi } from '@/api'
import ResultsView from '@/views/results/ResultsView.vue'

// Only the filter controls are stubbed: their props, labels, selected
// values and emitted change events remain observable at the view boundary.
const UiSelectStub = {
  name: 'UiSelect',
  props: {
    modelValue: { default: null },
    label: { type: String, default: '' },
    options: { type: Array, default: () => [] },
    filter: { type: Boolean, default: false },
    filterPlaceholder: { type: String, default: '' },
    optionLabel: { type: [String, Function], default: 'label' },
    optionValue: { type: [String, Function], default: 'value' },
    placeholder: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
  },
  emits: ['update:modelValue', 'change'],
  template: '<div class="select-stub" />',
}

function mountResults() {
  return shallowMount(ResultsView, {
    global: {
      stubs: {
        ResultsPageShell: {
          template: '<div><slot name="actions" /><slot /></div>',
        },
        UiCard: {
          template: '<section><slot /></section>',
        },
        UiSelect: UiSelectStub,
      },
    },
  })
}

function selectFor(wrapper, label) {
  const select = wrapper
    .findAllComponents(UiSelectStub)
    .find((candidate) => candidate.props('label') === label)

  expect(select, `Missing results filter: ${label}`).toBeTruthy()
  return select
}

async function selectValue(wrapper, label, value) {
  const select = selectFor(wrapper, label)
  select.vm.$emit('update:modelValue', value)
  await wrapper.vm.$nextTick()
  select.vm.$emit('change', { value })
  await flushPromises()
}

describe('ResultsView searchable catalogs', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.assign(state.auth, {
      isStudent: true,
      isTeacher: true,
      isAdmin: false,
      isStudentMode: false,
      isTeacherMode: true,
      isAdminMode: false,
    })

    resultsApi.getTeacherSubjects.mockResolvedValue({
      data: [{ id: 17, name: 'Математика' }],
    })
    resultsApi.getTeacherLectures.mockResolvedValue({
      data: [{ id: 23, title: 'Лекция' }],
    })
    resultsApi.getTeacherTests.mockResolvedValue({
      data: [{ id: 31, title: 'Тест' }],
    })
    resultsApi.getTeacherGroups.mockResolvedValue({
      data: [{ id: 47, name: 'Группа А' }],
    })
    resultsApi.getTeacherStudents.mockResolvedValue({
      data: [{ id: 59, fullName: 'Анна Иванова' }],
    })
    resultsApi.getStudentSubjects.mockResolvedValue({
      data: [{ id: 17, name: 'Математика' }],
    })
    resultsApi.getStudentData.mockResolvedValue({
      data: { stats: { total: 0, right: 0, percent: 0 }, attempts: [] },
    })
  })

  it('offers local search without changing the teacher filter cascade', async () => {
    const wrapper = mountResults()
    await flushPromises()

    for (const [label, placeholder] of [
      ['Предмет', 'Поиск по предметам'],
      ['Группа', 'Поиск по группам'],
      ['Студент', 'Поиск по студентам'],
    ]) {
      const select = selectFor(wrapper, label)
      expect(select.props('filter')).toBe(true)
      expect(select.props('filterPlaceholder')).toBe(placeholder)
    }

    expect(selectFor(wrapper, 'Группа').props('disabled')).toBe(true)
    expect(selectFor(wrapper, 'Студент').props('disabled')).toBe(true)
    expect(resultsApi.getTeacherLectures).not.toHaveBeenCalled()

    await selectValue(wrapper, 'Предмет', 17)
    expect(resultsApi.getTeacherLectures).toHaveBeenCalledWith(
      17,
      expect.objectContaining({ signal: expect.any(Object) })
    )
    expect(selectFor(wrapper, 'Лекция').props('options')).toEqual([
      { id: 23, title: 'Лекция' },
    ])

    await selectValue(wrapper, 'Лекция', 23)
    expect(resultsApi.getTeacherTests).toHaveBeenCalledWith(
      23,
      expect.objectContaining({ signal: expect.any(Object) })
    )

    await selectValue(wrapper, 'Тест', 31)
    expect(resultsApi.getTeacherGroups).toHaveBeenCalledWith(
      31,
      expect.objectContaining({ signal: expect.any(Object) })
    )
    expect(selectFor(wrapper, 'Группа').props('options')).toEqual([
      { id: 47, name: 'Группа А' },
    ])

    await selectValue(wrapper, 'Группа', 47)
    expect(resultsApi.getTeacherStudents).toHaveBeenCalledWith(
      47,
      expect.objectContaining({ signal: expect.any(Object) })
    )
    expect(selectFor(wrapper, 'Студент').props('options')).toEqual([
      { id: 59, fullName: 'Анна Иванова' },
    ])
    expect(resultsApi.getTeacherData).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('keeps the student subject filter searchable without exposing teacher filters', async () => {
    Object.assign(state.auth, {
      isStudentMode: true,
      isTeacherMode: false,
    })
    const wrapper = mountResults()
    await flushPromises()

    const subject = selectFor(wrapper, 'Предмет')
    expect(subject.props('filter')).toBe(true)
    expect(subject.props('filterPlaceholder')).toBe('Поиск по предметам')
    expect(wrapper.findAllComponents(UiSelectStub)).toHaveLength(2)
    expect(resultsApi.getTeacherSubjects).not.toHaveBeenCalled()

    await selectValue(wrapper, 'Предмет', 17)
    expect(resultsApi.getStudentData).toHaveBeenLastCalledWith(
      { subjectId: 17 },
      expect.objectContaining({ signal: expect.any(Object) })
    )
    expect(resultsApi.getTeacherData).not.toHaveBeenCalled()

    wrapper.unmount()
  })
})
