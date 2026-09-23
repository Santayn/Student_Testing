import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import {
  defineComponent,
  h,
} from 'vue'
import {
  flushPromises,
  mount,
} from '@vue/test-utils'

const state = vi.hoisted(() => ({
  workspaceAccess: true,
  router: {
    replace: vi.fn(),
  },
  authStore: {
    user: null,
    personId: 7,
    roles: ['STUDENT'],
    workspaceRole: 'STUDENT',
    fullName: 'Иван Иванов',
    isStudent: true,
    isTeacher: false,
    isAuthenticated: true,
    changingPassword: false,
    passwordError: null,
    loadCurrentUser: vi.fn(),
    refreshIdentity: vi.fn(),
    changePassword: vi.fn(),
    clearError: vi.fn(),
  },
  api: {
    subjectMemberships: vi.fn(),
    subjectsAll: vi.fn(),
    subjectById: vi.fn(),
    assignments: vi.fn(),
    groupsAll: vi.fn(),
    groupById: vi.fn(),
  },
  studentContext: vi.fn(),
}))

vi.mock('vue-router', () => ({
  useRouter: () => state.router,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.authStore,
}))

vi.mock('@/utils/accountAccess', () => ({
  hasWorkspaceAccess: () => state.workspaceAccess,
}))

vi.mock('@/utils/studentLearningContext', () => ({
  loadStudentLearningContext: state.studentContext,
}))

vi.mock('@/api', () => ({
  facultiesApi: {},
  groupsApi: {
    getAll: state.api.groupsAll,
    getById: state.api.groupById,
  },
  membershipsApi: {
    getSubjectMemberships: state.api.subjectMemberships,
  },
  subjectsApi: {
    getAll: state.api.subjectsAll,
    getById: state.api.subjectById,
  },
  teachingApi: {
    getAssignments: state.api.assignments,
  },
}))

import ProfileView from '@/views/ProfileView.vue'

const UiInputStub = defineComponent({
  name: 'UiInput',
  inheritAttrs: false,
  props: {
    modelValue: { type: String, default: '' },
    label: { type: String, default: '' },
    type: { type: String, default: 'text' },
    error: { type: String, default: '' },
    disabled: { type: Boolean, default: false },
  },
  emits: ['update:modelValue', 'blur'],
  setup(props, { attrs, emit }) {
    return () => h('label', [
      h('span', props.label),
      h('input', {
        ...attrs,
        'data-label': props.label,
        type: props.type,
        value: props.modelValue,
        disabled: props.disabled,
        onInput: (event) => emit('update:modelValue', event.target.value),
        onBlur: () => emit('blur'),
      }),
      props.error ? h('span', { class: 'field-error' }, props.error) : null,
    ])
  },
})

const UiButtonStub = defineComponent({
  name: 'UiButton',
  inheritAttrs: false,
  props: {
    type: { type: String, default: 'button' },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
  },
  emits: ['click'],
  setup(props, { slots, emit, attrs }) {
    return () => h(
      'button',
      {
        ...attrs,
        type: props.type,
        disabled: props.disabled || props.loading,
        onClick: (event) => emit('click', event),
      },
      slots.default?.()
    )
  },
})

const UiCardStub = defineComponent({
  name: 'UiCard',
  props: {
    title: { type: String, default: '' },
    description: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () => h('section', { class: 'ui-card-stub' }, [
      props.title ? h('h2', props.title) : null,
      props.description ? h('p', props.description) : null,
      slots.default?.(),
    ])
  },
})

const UiDialogStub = defineComponent({
  name: 'UiDialog',
  props: {
    modelValue: { type: Boolean, default: false },
    title: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  setup(props, { slots }) {
    return () => props.modelValue
      ? h('section', { class: 'dialog-stub' }, [
          h('h2', props.title),
          slots.default?.(),
          h('footer', slots.footer?.()),
        ])
      : null
  },
})

const UiAlertStub = defineComponent({
  name: 'UiAlert',
  props: {
    message: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () => h('div', { class: 'alert-stub' }, slots.default?.() ?? props.message)
  },
})

const UiEmptyStateStub = defineComponent({
  name: 'UiEmptyState',
  props: {
    description: { type: String, default: '' },
  },
  setup(props) {
    return () => h('div', { class: 'empty-stub' }, props.description)
  },
})

const RouterLinkStub = defineComponent({
  name: 'RouterLink',
  props: ['to'],
  setup(_, { slots }) {
    return () => h('a', slots.default?.())
  },
})

function baseUser() {
  return {
    userId: 11,
    login: 'ivanov',
    personId: 7,
    person: {
      id: 7,
      firstName: 'Иван',
      lastName: 'Иванов',
      dateOfBirth: '2003-04-18',
      email: 'ivan@example.test',
      phone: '+79990000000',
    },
    roles: ['STUDENT'],
  }
}

function mountProfile() {
  return mount(ProfileView, {
    global: {
      stubs: {
        UiInput: UiInputStub,
        UiButton: UiButtonStub,
        UiCard: UiCardStub,
        UiDialog: UiDialogStub,
        UiAlert: UiAlertStub,
        UiEmptyState: UiEmptyStateStub,
        RouterLink: RouterLinkStub,
      },
    },
  })
}

describe('ProfileView', () => {
  beforeEach(() => {
    state.workspaceAccess = true
    state.router.replace.mockReset()

    state.authStore.user = baseUser()
    state.authStore.personId = 7
    state.authStore.roles = ['STUDENT']
    state.authStore.workspaceRole = 'STUDENT'
    state.authStore.fullName = 'Иван Иванов'
    state.authStore.isStudent = true
    state.authStore.isTeacher = false
    state.authStore.isAuthenticated = true
    state.authStore.changingPassword = false
    state.authStore.passwordError = null
    state.authStore.loadCurrentUser.mockReset()
    state.authStore.loadCurrentUser.mockResolvedValue(state.authStore.user)
    state.authStore.refreshIdentity.mockReset()
    state.authStore.refreshIdentity.mockResolvedValue(state.authStore.user)
    state.authStore.changePassword.mockReset()
    state.authStore.changePassword.mockResolvedValue({
      requiresReauthentication: true,
    })
    state.authStore.clearError.mockReset()

    state.studentContext.mockReset()
    state.studentContext.mockResolvedValue({
      memberships: [{ id: 91 }],
      groups: [{ id: 21, name: 'КБ-23' }],
      faculties: [{ id: 31, name: 'ФКБ' }],
      subjects: [{ id: 41, name: 'Базы данных' }],
    })

    state.api.subjectMemberships.mockReset()
    state.api.subjectsAll.mockReset()
    state.api.subjectById.mockReset()
    state.api.assignments.mockReset()
    state.api.groupsAll.mockReset()
    state.api.groupById.mockReset()
  })

  it('separates profile data, shows date of birth and hides technical identifiers', async () => {
    const wrapper = mountProfile()
    await flushPromises()

    expect(wrapper.text()).toContain('Личные данные')
    expect(wrapper.text()).toContain('Учётная запись')
    expect(wrapper.text()).toContain('Безопасность')
    expect(wrapper.text()).toContain('18.04.2003')
    expect(wrapper.text()).toContain('Базы данных')

    expect(wrapper.text()).not.toContain('Person ID')
    expect(wrapper.text()).not.toContain('GroupMembership')
    expect(wrapper.text()).not.toContain('#41')
    expect(wrapper.text()).not.toContain('#21')

    wrapper.unmount()
  })

  it('moves to account-pending when an explicit identity refresh loses workspace access', async () => {
    state.authStore.refreshIdentity.mockImplementationOnce(async () => {
      state.workspaceAccess = false
      return state.authStore.user
    })

    const wrapper = mountProfile()
    await flushPromises()

    const refresh = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Обновить данные')

    await refresh.trigger('click')
    await flushPromises()

    expect(state.authStore.refreshIdentity).toHaveBeenCalledTimes(1)
    expect(state.router.replace).toHaveBeenCalledWith({
      name: 'account-pending',
    })

    wrapper.unmount()
  })

  it('uses bulk reference collections for teacher context instead of subject/group getById calls', async () => {
    state.authStore.user = {
      ...baseUser(),
      roles: ['TEACHER'],
    }
    state.authStore.roles = ['TEACHER']
    state.authStore.workspaceRole = 'TEACHER'
    state.authStore.isStudent = false
    state.authStore.isTeacher = true

    state.api.subjectMemberships.mockResolvedValue({
      data: [
        { id: 51, role: 1, subjectId: 61 },
      ],
    })
    state.api.subjectsAll.mockResolvedValue({
      data: [
        { id: 61, name: 'Алгоритмы' },
        { id: 62, name: 'Лишний предмет' },
      ],
    })
    state.api.assignments.mockResolvedValue({
      data: [
        { id: 71, groupId: 81 },
      ],
    })
    state.api.groupsAll.mockResolvedValue({
      data: [
        { id: 81, name: 'ПИ-23' },
        { id: 82, name: 'Лишняя группа' },
      ],
    })

    const wrapper = mountProfile()
    await flushPromises()

    expect(wrapper.text()).toContain('Алгоритмы')
    expect(wrapper.text()).toContain('ПИ-23')
    expect(wrapper.text()).not.toContain('Лишний предмет')
    expect(wrapper.text()).not.toContain('Лишняя группа')
    expect(state.api.subjectById).not.toHaveBeenCalled()
    expect(state.api.groupById).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('validates change-password locally and redirects to Login after success', async () => {
    const wrapper = mountProfile()
    await flushPromises()

    const open = wrapper
      .findAll('button')
      .find((button) => button.text() === 'Изменить пароль')

    await open.trigger('click')

    const dialog = wrapper.find('.dialog-stub')
    const current = dialog.find('input[data-label="Текущий пароль"]')
    const next = dialog.find('input[data-label="Новый пароль"]')
    const confirm = dialog.find('input[data-label="Повторите новый пароль"]')

    await current.setValue('secret1')
    await next.setValue('secret1')
    await confirm.setValue('secret1')
    await next.trigger('blur')

    expect(dialog.text()).toContain('Новый пароль должен отличаться от текущего')
    expect(state.authStore.changePassword).not.toHaveBeenCalled()

    await next.setValue('secret2')
    await confirm.setValue('secret2')

    const submit = dialog
      .findAll('button')
      .find((button) => button.text() === 'Изменить пароль')

    await submit.trigger('click')
    await flushPromises()

    expect(state.authStore.changePassword).toHaveBeenCalledWith('secret1', 'secret2')
    expect(state.router.replace).toHaveBeenCalledWith({
      name: 'login',
      query: {
        passwordChanged: '1',
        redirect: '/profile',
      },
    })

    wrapper.unmount()
  })
})
