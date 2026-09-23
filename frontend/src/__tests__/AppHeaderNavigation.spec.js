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

import AppHeader from '@/components/layout/AppHeader.vue'

const mockState = vi.hoisted(() => ({
  route: {
    name: 'home',
    fullPath: '/',
  },
  router: {
    push: vi.fn(),
    replace: vi.fn(),
  },
  authStore: {
    isAuthenticated: true,
    fullName: 'Иван Иванов',
    loginName: 'ivan',
    email: 'ivan@example.test',
    workspaceRole: 'STUDENT',
    workspaceRoles: ['STUDENT', 'TEACHER'],
    hasMultipleWorkspaceRoles: true,
    loggingOut: false,
    setWorkspaceRole: vi.fn(),
    logout: vi.fn(),
  },
  themeStore: {
    isDark: false,
    toggleTheme: vi.fn(),
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockState.route,
  useRouter: () => mockState.router,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mockState.authStore,
}))

vi.mock('@/stores/theme', () => ({
  useThemeStore: () => mockState.themeStore,
}))

vi.mock('@/utils/accountAccess', () => ({
  hasWorkspaceAccess: () => true,
}))

const RouterLinkStub = defineComponent({
  name: 'RouterLink',
  inheritAttrs: false,
  props: {
    to: {
      type: [String, Object],
      required: true,
    },
  },
  setup(props, { attrs, slots }) {
    return () => h(
      'a',
      {
        ...attrs,
        'data-route-name':
          typeof props.to === 'object'
            ? props.to.name
            : props.to,
      },
      slots.default?.()
    )
  },
})

function mountHeader() {
  return mount(AppHeader, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

describe('AppHeader navigation responsibility', () => {
  beforeEach(() => {
    mockState.route.name = 'home'
    mockState.route.fullPath = '/'
    mockState.authStore.isAuthenticated = true
    mockState.authStore.workspaceRole = 'STUDENT'
    mockState.authStore.loggingOut = false
    mockState.authStore.setWorkspaceRole.mockReset()
    mockState.authStore.logout.mockReset()
    mockState.themeStore.toggleTheme.mockReset()
    mockState.router.push.mockReset()
    mockState.router.replace.mockReset()
  })

  it('does not duplicate workspace destinations from the Sidebar', () => {
    const wrapper = mountHeader()

    expect(wrapper.find('.app-header__nav').exists()).toBe(false)

    const routeNames = wrapper
      .findAll('[data-route-name]')
      .map((link) => link.attributes('data-route-name'))

    expect(routeNames).toEqual(['home'])
    expect(routeNames).not.toContain('subjects')
    expect(routeNames).not.toContain('results')
    expect(routeNames).not.toContain('profile')

    expect(wrapper.find('.user-badge').element.tagName).toBe('DIV')

    wrapper.unmount()
  })

  it('keeps account and workspace controls in the Header', () => {
    const wrapper = mountHeader()

    expect(wrapper.find('.workspace-role-switcher').exists()).toBe(true)
    expect(wrapper.find('.theme-toggle').exists()).toBe(true)
    expect(wrapper.find('.user-badge').text()).toContain('Иван Иванов')
    expect(wrapper.find('.app-header__button').text()).toBe('Выйти')

    wrapper.unmount()
  })

  it('labels the mobile disclosure as an account menu rather than navigation', async () => {
    const wrapper = mountHeader()
    const toggle = wrapper.find('.mobile-menu-button')

    expect(toggle.attributes('aria-controls')).toBe('mobile-account-menu')
    expect(toggle.attributes('aria-label')).toBe('Открыть меню аккаунта')
    expect(toggle.attributes('aria-expanded')).toBe('false')

    await toggle.trigger('click')

    expect(toggle.attributes('aria-expanded')).toBe('true')
    expect(wrapper.find('#mobile-account-menu').classes())
      .toContain('app-header__content--open')

    wrapper.unmount()
  })
})
