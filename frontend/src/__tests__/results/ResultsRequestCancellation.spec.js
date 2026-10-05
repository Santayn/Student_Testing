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

const auth = vi.hoisted(() => ({
  isStudent: true,
  isTeacher: true,
  isAdmin: false,
  isStudentMode: false,
  isTeacherMode: true,
  isAdminMode: false,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => auth,
}))
vi.mock('@/api', () => ({
  getApiErrorMessage: (_error, fallback) => fallback,
  lecturesApi: { getAll: vi.fn(), getTests: vi.fn() },
  subjectsApi: { getAll: vi.fn() },
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

const UiButtonStub = {
  name: 'UiButton',
  props: { loading: { type: Boolean, default: false } },
  emits: ['click'],
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
}

function mountResults() {
  return shallowMount(ResultsView, {
    global: {
      stubs: {
        ResultsPageShell: {
          template: '<div><slot name="actions" /><slot /></div>',
        },
        UiCard: { template: '<section><slot /></section>' },
        UiSelect: UiSelectStub,
        UiButton: UiButtonStub,
      },
    },
  })
}

async function selectValue(wrapper, label, value) {
  const select = wrapper.findAllComponents(UiSelectStub)
    .find((candidate) => candidate.props('label') === label)
  expect(select, `Missing filter ${label}`).toBeTruthy()
  select.vm.$emit('update:modelValue', value)
  await wrapper.vm.$nextTick()
  select.vm.$emit('change', { value })
  await flushPromises()
}

function deferred() {
  let resolve
  let reject
  const promise = new Promise((done, fail) => {
    resolve = done
    reject = fail
  })
  return { promise, resolve, reject }
}

function canceled() {
  return Object.assign(new Error('canceled'), { code: 'ERR_CANCELED' })
}

describe('ResultsView cancels only its superseded reads', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.assign(auth, {
      isStudent: true,
      isTeacher: true,
      isAdmin: false,
      isStudentMode: false,
      isTeacherMode: true,
      isAdminMode: false,
    })
    resultsApi.getTeacherSubjects.mockResolvedValue({
      data: [{ id: 17, name: 'A' }, { id: 18, name: 'B' }],
    })
    resultsApi.getTeacherLectures.mockResolvedValue({ data: [] })
    resultsApi.getStudentSubjects.mockResolvedValue({ data: [] })
    resultsApi.getStudentData.mockResolvedValue({
      data: { attempts: [], stats: { total: 0, right: 0, percent: 0 } },
    })
    resultsApi.getTeacherData.mockResolvedValue({ data: { attempts: [] } })
  })

  it('aborts a superseded filter request and ignores its late response', async () => {
    const oldResponse = deferred()
    resultsApi.getTeacherLectures
      .mockImplementationOnce((_subjectId, { signal }) => {
        // An adapter that ignores abort() must still be protected by epoch.
        expect(signal).toBeInstanceOf(AbortSignal)
        return oldResponse.promise
      })
      .mockResolvedValueOnce({ data: [{ id: 92, title: 'B lecture' }] })

    const wrapper = mountResults()
    await flushPromises()
    await selectValue(wrapper, 'Предмет', 17)
    const firstSignal = resultsApi.getTeacherLectures.mock.calls[0][1].signal
    expect(firstSignal.aborted).toBe(false)
    const subject = wrapper.findAllComponents(UiSelectStub)
      .find((candidate) => candidate.props('label') === 'Предмет')
    expect(subject.props('disabled')).toBe(false)

    await selectValue(wrapper, 'Предмет', 18)
    expect(firstSignal.aborted).toBe(true)
    expect(resultsApi.getTeacherLectures.mock.calls[1][1].signal.aborted).toBe(false)
    const lecture = wrapper.findAllComponents(UiSelectStub)
      .find((candidate) => candidate.props('label') === 'Лекция')
    expect(lecture.props('options')).toEqual([{ id: 92, title: 'B lecture' }])

    oldResponse.resolve({ data: [{ id: 11, title: 'stale A lecture' }] })
    await flushPromises()
    expect(lecture.props('options')).toEqual([{ id: 92, title: 'B lecture' }])
    wrapper.unmount()
  })

  it('aborts the pending filter read on unmount', async () => {
    const pending = deferred()
    resultsApi.getTeacherLectures.mockImplementationOnce((_id, { signal }) => {
      signal.addEventListener('abort', () => pending.reject(canceled()), { once: true })
      return pending.promise
    })
    const wrapper = mountResults()
    await flushPromises()
    await selectValue(wrapper, 'Предмет', 17)
    const signal = resultsApi.getTeacherLectures.mock.calls[0][1].signal
    wrapper.unmount()
    expect(signal.aborted).toBe(true)
    await flushPromises()
  })

  it('aborts previous results when user filters change and never applies late results', async () => {
    const pending = deferred()
    resultsApi.getTeacherData.mockImplementationOnce((_params, { signal }) => {
      signal.addEventListener('abort', () => pending.reject(canceled()), { once: true })
      return pending.promise
    })
    const wrapper = mountResults()
    await flushPromises()
    const showResults = wrapper.findAllComponents(UiButtonStub)
      .find((button) => button.text().includes('Показать результаты'))
    expect(showResults).toBeTruthy()
    await showResults.trigger('click')
    const signal = resultsApi.getTeacherData.mock.calls[0][1].signal
    expect(signal.aborted).toBe(false)

    await selectValue(wrapper, 'Предмет', 17)
    expect(signal.aborted).toBe(true)
    expect(resultsApi.getTeacherLectures).toHaveBeenCalledTimes(1)
    await flushPromises()
    wrapper.unmount()
  })

  it('cancels the previous student results read when subject changes', async () => {
    Object.assign(auth, { isStudentMode: true, isTeacherMode: false })
    const pending = deferred()
    resultsApi.getStudentData
      .mockImplementationOnce((_params, { signal }) => {
        signal.addEventListener('abort', () => pending.reject(canceled()), { once: true })
        return pending.promise
      })
      .mockResolvedValue({ data: { attempts: [] } })

    const wrapper = mountResults()
    await flushPromises()
    const initialSignal = resultsApi.getStudentData.mock.calls[0][1].signal
    await selectValue(wrapper, 'Предмет', 17)
    expect(initialSignal.aborted).toBe(true)
    expect(resultsApi.getStudentData).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('cancels pending student results when a selected test fails local validation', async () => {
    Object.assign(auth, { isStudentMode: true, isTeacherMode: false })
    const pending = deferred()
    resultsApi.getStudentData
      .mockResolvedValueOnce({
        data: { attempts: [{ attemptId: 1, testId: 31, testName: 'My test' }] },
      })
      .mockImplementationOnce((_params, { signal }) => {
        signal.addEventListener('abort', () => pending.reject(canceled()), { once: true })
        return pending.promise
      })

    const wrapper = mountResults()
    await flushPromises()
    await selectValue(wrapper, 'Тест', 31)
    const signal = resultsApi.getStudentData.mock.calls[1][1].signal
    expect(signal.aborted).toBe(false)

    await selectValue(wrapper, 'Тест', 999)
    expect(signal.aborted).toBe(true)
    expect(resultsApi.getStudentData).toHaveBeenCalledTimes(2)
    const testFilter = wrapper.findAllComponents(UiSelectStub)
      .find((candidate) => candidate.props('label') === 'Тест')
    expect(testFilter.props('modelValue')).toBe('')
    wrapper.unmount()
  })
})
