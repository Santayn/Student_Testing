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
  mount,
} from '@vue/test-utils'

const state = vi.hoisted(() => ({
  route: {
    query: {},
  },
  router: {
    replace: vi.fn(),
  },
  authStore: {
    loggingIn: false,
    loginError: null,
    login: vi.fn(),
    clearError: vi.fn(),
  },
  workspaceAccess: true,
}))

vi.mock('vue-router', () => ({
  useRoute: () => state.route,
  useRouter: () => state.router,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.authStore,
}))

vi.mock('@/utils/accountAccess', () => ({
  hasWorkspaceAccess: () => state.workspaceAccess,
}))

vi.mock('@/config/features', () => ({
  publicRegistrationEnabled: true,
}))

import LoginView from '@/views/auth/LoginView.vue'

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
        type: props.type,
        value: props.modelValue,
        disabled: props.disabled,
        'data-label': props.label,
        onInput: (event) => emit('update:modelValue', event.target.value),
        onBlur: () => emit('blur'),
      }),
      props.error
        ? h('span', { class: 'field-error' }, props.error)
        : null,
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
  setup(props, { slots, emit }) {
    return () => h(
      'button',
      {
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
  setup(_, { slots }) {
    return () => h('div', { class: 'ui-card-stub' }, slots.default?.())
  },
})

const UiAlertStub = defineComponent({
  name: 'UiAlert',
  props: {
    message: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () => h('div', { class: 'ui-alert-stub' }, slots.default?.() ?? props.message)
  },
})

function mountLogin() {
  return mount(LoginView, {
    global: {
      stubs: {
        UiInput: UiInputStub,
        UiButton: UiButtonStub,
        UiCard: UiCardStub,
        UiAlert: UiAlertStub,
        RouterLink: defineComponent({
          props: ['to'],
          setup(_, { slots }) {
            return () => h('a', slots.default?.())
          },
        }),
      },
    },
  })
}

async function fillValidCredentials(wrapper) {
  const login = wrapper.find('input[data-label="Логин"]')
  const password = wrapper.find('input[data-label="Пароль"]')

  await login.setValue('student')
  await password.setValue('secret1')
}

describe('LoginView', () => {
  beforeEach(() => {
    state.route.query = {}
    state.workspaceAccess = true
    state.authStore.loggingIn = false
    state.authStore.loginError = null
    state.authStore.login.mockReset()
    state.authStore.login.mockResolvedValue({})
    state.authStore.clearError.mockReset()
    state.router.replace.mockReset()
  })

  it('matches the login contract and shows field-level validation', async () => {
    const wrapper = mountLogin()
    const login = wrapper.find('input[data-label="Логин"]')
    const password = wrapper.find('input[data-label="Пароль"]')

    expect(login.attributes('maxlength')).toBe('100')
    expect(password.attributes('minlength')).toBe('6')
    expect(password.attributes('maxlength')).toBe('200')

    await password.setValue('123')
    await password.trigger('blur')

    expect(wrapper.text()).toContain('минимум 6 символов')
    expect(state.authStore.login).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('submits valid credentials and preserves redirect', async () => {
    state.route.query = {
      redirect: '/subjects',
    }

    const wrapper = mountLogin()
    await fillValidCredentials(wrapper)
    await wrapper.find('form').trigger('submit')

    expect(state.authStore.login).toHaveBeenCalledWith('student', 'secret1')
    expect(state.router.replace).toHaveBeenCalledWith('/subjects')

    wrapper.unmount()
  })

  it('redirects an authenticated account without workspace access to account-pending', async () => {
    state.workspaceAccess = false

    const wrapper = mountLogin()
    await fillValidCredentials(wrapper)
    await wrapper.find('form').trigger('submit')

    expect(state.router.replace).toHaveBeenCalledWith({
      name: 'account-pending',
    })

    wrapper.unmount()
  })

  it('toggles password visibility with accessible pressed state', async () => {
    const wrapper = mountLogin()
    const toggle = wrapper.find('.auth-password-toggle')

    expect(wrapper.find('input[data-label="Пароль"]').attributes('type')).toBe('password')
    expect(toggle.attributes('aria-pressed')).toBe('false')

    await toggle.trigger('click')

    expect(wrapper.find('input[data-label="Пароль"]').attributes('type')).toBe('text')
    expect(toggle.attributes('aria-pressed')).toBe('true')

    wrapper.unmount()
  })

  it('shows the explicit password-changed reauthentication notice', () => {
    state.route.query = {
      passwordChanged: '1',
    }

    const wrapper = mountLogin()

    expect(wrapper.text()).toContain('Пароль изменён. Войдите снова с новым паролем.')

    wrapper.unmount()
  })
})
