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

import AppSidebar from '@/components/layout/AppSidebar.vue'
import {
  NAV_KEYS,
} from '@/navigation/navigation.config'

const mockState = vi.hoisted(() => ({
  route: {
    fullPath: '/subjects/42',
    meta: {
      navKey: 'subjects',
    },
  },
  authStore: {
    workspaceRole: 'STUDENT',
  },
}))

vi.mock('vue-router', () => ({
  useRoute: () => mockState.route,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mockState.authStore,
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

function mountSidebar() {
  return mount(AppSidebar, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

describe('AppSidebar workspace navigation', () => {
  beforeEach(() => {
    mockState.authStore.workspaceRole = 'STUDENT'
    mockState.route.fullPath = '/subjects/42'
    mockState.route.meta.navKey = NAV_KEYS.SUBJECTS
  })

  it('renders student navigation from the shared workspace config', () => {
    const wrapper = mountSidebar()

    const labels = wrapper
      .findAll('.app-sidebar__link')
      .map((link) => link.text())

    expect(labels).toEqual([
      'Главная',
      'Предметы',
      'Результаты',
      'Профиль',
    ])

    wrapper.unmount()
  })

  it('uses navKey instead of exact route matching for the active destination', () => {
    const wrapper = mountSidebar()

    const active = wrapper.find(
      '.app-sidebar__link--active'
    )

    expect(active.exists()).toBe(true)
    expect(active.text()).toBe('Предметы')
    expect(active.attributes('aria-current')).toBe('page')

    wrapper.unmount()
  })

  it('renders teacher sections from the same config', () => {
    mockState.authStore.workspaceRole = 'TEACHER'
    mockState.route.fullPath = '/teacher/questions'
    mockState.route.meta.navKey =
      NAV_KEYS.TEACHER_QUESTIONS

    const wrapper = mountSidebar()

    const labels = wrapper
      .findAll('.app-sidebar__link')
      .map((link) => link.text())

    expect(labels).toContain('Мои предметы')
    expect(labels).toContain('Вопросы')
    expect(labels).toContain('Создать тест')
    expect(labels).not.toContain('Факультеты')

    wrapper.unmount()
  })

  it('renders admin sections from the same config', () => {
    mockState.authStore.workspaceRole = 'ADMIN'
    mockState.route.fullPath = '/admin/faculties'
    mockState.route.meta.navKey =
      NAV_KEYS.ADMIN_FACULTIES

    const wrapper = mountSidebar()

    const labels = wrapper
      .findAll('.app-sidebar__link')
      .map((link) => link.text())

    expect(labels).toContain('Факультеты')
    expect(labels).toContain('Шаблоны нагрузки')
    expect(labels).toContain('Роли пользователей')

    wrapper.unmount()
  })
})
