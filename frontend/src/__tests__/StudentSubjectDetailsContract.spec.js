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
  route: {
    params: {
      subjectId: '7',
    },
    query: {},
  },
  auth: {
    isStudent: true,
    isTeacher: false,
    isAdmin: false,
    isStudentMode: true,
    isTeacherMode: false,
    isAdminMode: false,
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => state.route,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.auth,
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: (
    _error,
    fallback
  ) => fallback,
  learningApi: {
    getSubject: vi.fn(),
  },
  subjectsApi: {
    getById: vi.fn(),
  },
}))

import {
  learningApi,
  subjectsApi,
} from '@/api'
import SubjectDetailsView from '@/views/subjects/SubjectDetailsView.vue'

describe('subject details API selection', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    state.route.params.subjectId = '7'
    state.route.query = {}

    Object.assign(state.auth, {
      isStudent: true,
      isTeacher: false,
      isAdmin: false,
      isStudentMode: true,
      isTeacherMode: false,
      isAdminMode: false,
    })

    learningApi.getSubject.mockResolvedValue({
      data: {
        id: 7,
        name: 'Базы данных',
      },
    })
    subjectsApi.getById.mockResolvedValue({
      data: {
        id: 7,
        name: 'Базы данных',
      },
    })
  })

  it('uses public learning API for a pure student context', async () => {
    const wrapper = shallowMount(
      SubjectDetailsView
    )

    await flushPromises()

    expect(
      learningApi.getSubject
    ).toHaveBeenCalledTimes(1)
    expect(
      learningApi.getSubject
    ).toHaveBeenCalledWith(7)
    expect(
      subjectsApi.getById
    ).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it.each([
    {
      role: 'teacher',
      auth: {
        isStudent: false,
        isTeacher: true,
        isAdmin: false,
        isStudentMode: false,
        isTeacherMode: true,
        isAdminMode: false,
      },
    },
    {
      role: 'admin',
      auth: {
        isStudent: false,
        isTeacher: false,
        isAdmin: true,
        isStudentMode: false,
        isTeacherMode: false,
        isAdminMode: true,
      },
    },
  ])('uses management API for $role context', async ({ auth }) => {
    Object.assign(state.auth, auth)

    const wrapper = shallowMount(
      SubjectDetailsView
    )

    await flushPromises()

    expect(
      subjectsApi.getById
    ).toHaveBeenCalledTimes(1)
    expect(
      subjectsApi.getById
    ).toHaveBeenCalledWith(7)
    expect(
      learningApi.getSubject
    ).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('uses student API for a STUDENT + TEACHER account in student mode', async () => {
    Object.assign(state.auth, {
      isStudent: true,
      isTeacher: true,
      isAdmin: false,
      isStudentMode: true,
      isTeacherMode: false,
      isAdminMode: false,
    })

    const wrapper = shallowMount(
      SubjectDetailsView
    )

    await flushPromises()

    expect(
      learningApi.getSubject
    ).toHaveBeenCalledWith(7)
    expect(
      subjectsApi.getById
    ).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('uses management API for a STUDENT + TEACHER account in teacher mode', async () => {
    Object.assign(state.auth, {
      isStudent: true,
      isTeacher: true,
      isAdmin: false,
      isStudentMode: false,
      isTeacherMode: true,
      isAdminMode: false,
    })

    const wrapper = shallowMount(
      SubjectDetailsView
    )

    await flushPromises()

    expect(
      subjectsApi.getById
    ).toHaveBeenCalledWith(7)
    expect(
      learningApi.getSubject
    ).not.toHaveBeenCalled()

    wrapper.unmount()
  })
})
