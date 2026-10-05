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
    isStudentMode: true,
    isTeacherMode: false,
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

import {
  resultsApi,
} from '@/api'
import ResultsView from '@/views/results/ResultsView.vue'

function emptyStudentResult() {
  return {
    data: {
      stats: {
        total: 0,
        right: 0,
        percent: 0,
      },
      attempts: [],
    },
  }
}

describe('multi-role results mode', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    Object.assign(state.auth, {
      isStudent: true,
      isTeacher: true,
      isAdmin: false,
      isStudentMode: true,
      isTeacherMode: false,
      isAdminMode: false,
    })

    resultsApi.getStudentSubjects.mockResolvedValue({
      data: [],
    })
    resultsApi.getStudentData.mockResolvedValue(
      emptyStudentResult()
    )
    resultsApi.getTeacherSubjects.mockResolvedValue({
      data: [],
    })
  })

  it('uses student results APIs in student mode even when TEACHER role also exists', async () => {
    const wrapper = shallowMount(ResultsView)

    await flushPromises()

    expect(
      resultsApi.getStudentSubjects
    ).toHaveBeenCalledTimes(1)
    expect(
      resultsApi.getStudentData
    ).toHaveBeenCalledTimes(1)
    expect(
      resultsApi.getTeacherSubjects
    ).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('uses teacher results APIs after switching the same account to teacher mode', async () => {
    Object.assign(state.auth, {
      isStudentMode: false,
      isTeacherMode: true,
      isAdminMode: false,
    })

    const wrapper = shallowMount(ResultsView)

    await flushPromises()

    expect(
      resultsApi.getTeacherSubjects
    ).toHaveBeenCalledTimes(1)
    expect(
      resultsApi.getStudentSubjects
    ).not.toHaveBeenCalled()
    expect(
      resultsApi.getStudentData
    ).not.toHaveBeenCalled()

    wrapper.unmount()
  })
})
