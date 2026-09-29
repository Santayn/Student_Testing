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
  nextTick,
} from 'vue'
import {
  mount,
} from '@vue/test-utils'

import AppSidebar from '@/components/layout/AppSidebar.vue'
import appSidebarSource from '@/components/layout/AppSidebar.vue?raw'
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
    attachTo: document.body,
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

    expect(labels).toEqual([
      'Главная',
      'Мои предметы',
      'Результаты',
      'Темы предмета',
      'Вопросы',
      'Лекции',
      'Шаблоны курса',
      'Моя нагрузка',
      'Профиль',
    ])

    expect(labels).not.toContain('Создать тест')
    expect(labels).not.toContain('Факультеты')

    wrapper.unmount()
  })

  it('keeps questions active while test creation is open', () => {
    mockState.authStore.workspaceRole = 'TEACHER'
    mockState.route.fullPath = '/teacher/tests/create'
    mockState.route.meta.navKey =
      NAV_KEYS.TEACHER_QUESTIONS

    const wrapper = mountSidebar()

    const active = wrapper.find(
      '.app-sidebar__link--active'
    )

    expect(active.exists()).toBe(true)
    expect(active.text()).toBe('Вопросы')

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

    expect(labels).toEqual([
      'Главная',
      'Результаты',
      'Факультеты',
      'Группы',
      'Справочник предметов',
      'Предметы факультетов',
      'Преподаватели и предметы',
      'Учебная нагрузка',
      'Доступные предметы',
      'Темы предмета',
      'Вопросы',
      'Лекции',
      'Шаблоны курса',
      'Пользователи',
      'Роли и права',
      'Профиль',
    ])

    expect(labels).not.toContain('Создать тест')

    wrapper.unmount()
  })

  it('opens and closes the mobile drawer without leaving the page scroll locked', async () => {
    const wrapper = mountSidebar()
    const toggle = wrapper.find(
      '.sidebar-mobile-toggle'
    )

    await toggle.trigger('click')
    await nextTick()

    expect(
      wrapper.find('.app-sidebar').classes()
    ).toContain('app-sidebar--open')
    expect(toggle.attributes('aria-expanded')).toBe('true')
    expect(
      document.body.classList.contains(
        'sidebar-mobile-open'
      )
    ).toBe(true)
    expect(
      document.activeElement
    ).toBe(
      wrapper.find(
        '.app-sidebar__close'
      ).element
    )

    window.dispatchEvent(
      new KeyboardEvent('keydown', {
        key: 'Escape',
      })
    )
    await nextTick()

    expect(
      wrapper.find('.app-sidebar').classes()
    ).not.toContain('app-sidebar--open')
    expect(toggle.attributes('aria-expanded')).toBe('false')
    expect(
      document.body.classList.contains(
        'sidebar-mobile-open'
      )
    ).toBe(false)
    expect(document.activeElement).toBe(
      toggle.element
    )

    wrapper.unmount()
  })

  it('closes the drawer immediately after choosing a destination', async () => {
    const wrapper = mountSidebar()

    await wrapper
      .find('.sidebar-mobile-toggle')
      .trigger('click')

    await wrapper
      .find('.app-sidebar__link')
      .trigger('click')

    expect(
      wrapper.find('.app-sidebar').classes()
    ).not.toContain('app-sidebar--open')
    expect(
      document.body.classList.contains(
        'sidebar-mobile-open'
      )
    ).toBe(false)
    expect(document.activeElement).toBe(
      wrapper.find(
        '.sidebar-mobile-toggle'
      ).element
    )

    wrapper.unmount()
  })


  it('keeps the closed mobile drawer non-interactive until it opens', () => {
    expect(appSidebarSource).toContain(
      'visibility: hidden;'
    )
    expect(appSidebarSource).toContain(
      'pointer-events: none;'
    )
    expect(appSidebarSource).toContain(
      '.app-sidebar--open'
    )
    expect(appSidebarSource).toContain(
      'visibility: visible;'
    )
  })

  it('keeps keyboard focus inside an open mobile drawer', async () => {
    const wrapper = mountSidebar()

    await wrapper
      .find('.sidebar-mobile-toggle')
      .trigger('click')

    const links = wrapper.findAll(
      '.app-sidebar__link'
    )
    const lastLink = links[links.length - 1]

    lastLink.element.focus()

    window.dispatchEvent(
      new KeyboardEvent('keydown', {
        key: 'Tab',
        bubbles: true,
        cancelable: true,
      })
    )
    await nextTick()

    expect(document.activeElement).toBe(
      wrapper.find(
        '.app-sidebar__close'
      ).element
    )

    wrapper.unmount()
  })

})
