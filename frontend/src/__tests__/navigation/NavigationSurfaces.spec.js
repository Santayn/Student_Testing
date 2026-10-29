import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, reactive } from 'vue'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useNavigationUiStore } from '@/stores/navigationUi'
import AppBottomNavigation from '@/components/layout/AppBottomNavigation.vue'
import AppNavigationRail from '@/components/layout/AppNavigationRail.vue'
import { NAV_KEYS } from '@/navigation/navigation.config'

const state = vi.hoisted(() => ({
  route: { fullPath: '/teacher/questions', meta: { navKey: 'teacher-questions' } },
  auth: { workspaceRole: 'TEACHER' },
}))
vi.mock('vue-router', () => ({ useRoute: () => state.route }))
vi.mock('@/stores/auth', () => ({ useAuthStore: () => state.auth }))

const RouterLinkStub = defineComponent({
  name: 'RouterLink',
  inheritAttrs: false,
  props: { to: { type: [String, Object], required: true } },
  setup(props, { attrs, slots }) {
    return () => h('a', { ...attrs, href: `/${props.to.name}` }, slots.default?.())
  },
})

const wrappers = []
function create(Component) {
  const wrapper = mount(Component, {
    attachTo: document.body,
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
  wrappers.push(wrapper)
  return wrapper
}

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  state.auth = reactive({ workspaceRole: 'TEACHER' })
  state.route = reactive({ fullPath: '/teacher/questions', meta: { navKey: NAV_KEYS.TEACHER_QUESTIONS } })
})
afterEach(() => {
  while (wrappers.length) wrappers.pop().unmount()
  document.body.classList.remove('navigation-sheet-open')
  document.documentElement.classList.remove('navigation-sheet-open')
})

describe('responsive navigation surfaces', () => {
  it('keeps the primary route and four destinations in a student bottom bar', () => {
    state.auth.workspaceRole = 'STUDENT'
    state.route.meta.navKey = NAV_KEYS.SUBJECTS
    const wrapper = create(AppBottomNavigation)
    expect(wrapper.findAll('.app-bottom-navigation__item')).toHaveLength(4)
    expect(wrapper.findAll('.app-bottom-navigation__item--active')).toHaveLength(1)
    expect(wrapper.find('.app-bottom-navigation__item--active').text()).toBe('Предметы')
    expect(wrapper.find('[aria-label="Все разделы"]').exists()).toBe(false)
  })

  it('opens and closes role-scoped More navigation without changing the route', async () => {
    const wrapper = create(AppBottomNavigation)
    expect(wrapper.findAll('.app-bottom-navigation__item')).toHaveLength(5)
    await wrapper.find('button[aria-label="Все разделы"]').trigger('click')
    await nextTick()
    expect(document.querySelector('[role="dialog"][aria-modal="true"]')).not.toBeNull()
    expect(document.body.classList.contains('navigation-sheet-open')).toBe(true)
    expect(document.documentElement.classList.contains('navigation-sheet-open')).toBe(true)
    expect(document.querySelector('[aria-label="Все разделы приложения"]')?.textContent).toContain('Шаблоны курса')

    document.querySelector('button[aria-label="Закрыть разделы"]').click()
    await nextTick()
    expect(document.querySelector('[role="dialog"][aria-modal="true"]')).toBeNull()
    expect(document.body.classList.contains('navigation-sheet-open')).toBe(false)
    expect(document.documentElement.classList.contains('navigation-sheet-open')).toBe(false)
  })

  it('renders role-scoped root groups and direct links on tablet without More', () => {
    const wrapper = create(AppNavigationRail)
    expect(wrapper.findAll('.app-navigation-rail__item')).toHaveLength(4)
    expect(wrapper.findAll('button.app-navigation-rail__item')).toHaveLength(2)
    expect(wrapper.findAll('a.app-navigation-rail__item')).toHaveLength(2)
    expect(wrapper.find('[aria-label="Все разделы"]').exists()).toBe(false)
    expect(wrapper.find('.app-navigation-rail__item--active').text()).toBe('Учебный контент')
    expect(wrapper.find('a[href="/profile"]').exists()).toBe(true)
  })

  it('hides the entire rail and dismisses its flyout instead of collapsing to icons', async () => {
    const wrapper = create(AppNavigationRail)
    await wrapper.find('button[aria-label="Учебный контент"]').trigger('click')
    await nextTick()
    expect(document.querySelector('#navigation-rail-flyout')).not.toBeNull()

    await wrapper.find('.app-navigation-rail__collapse').trigger('click')
    await nextTick()
    expect(useNavigationUiStore().railVisible).toBe(false)
    expect(wrapper.emitted('hide')).toHaveLength(1)
    expect(document.querySelector('#navigation-rail-flyout')).toBeNull()
    expect(localStorage.getItem('st:navigation:rail-visible')).toBe('false')
  })

  it('opens a horizontal flyout and switches groups without changing the page', async () => {
    const wrapper = create(AppNavigationRail)
    const content = wrapper.find('button[aria-label="Учебный контент"]')
    await content.trigger('click')
    await nextTick()
    expect(content.attributes('aria-expanded')).toBe('true')
    expect(document.querySelector('#navigation-rail-flyout')?.textContent).toContain('Вопросы')
    expect(document.querySelector('#navigation-rail-flyout a[href="/teacher-questions"]')?.getAttribute('aria-current')).toBe('page')
    expect(document.querySelectorAll('#navigation-rail-flyout')).toHaveLength(1)

    await wrapper.find('button[aria-label="Обучение"]').trigger('click')
    await nextTick()
    expect(content.attributes('aria-expanded')).toBe('false')
    expect(document.querySelector('#navigation-rail-flyout')?.textContent).toContain('Главная')
    expect(document.querySelector('#navigation-rail-flyout')?.textContent).not.toContain('Шаблоны курса')
  })

  it('closes on Escape, outside pointer, route change and role change', async () => {
    const wrapper = create(AppNavigationRail)
    const trigger = wrapper.find('button[aria-label="Учебный контент"]')
    await trigger.trigger('click')
    await nextTick()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await nextTick()
    expect(trigger.attributes('aria-expanded')).toBe('false')
    expect(document.activeElement).toBe(trigger.element)

    await trigger.trigger('click')
    await nextTick()
    document.body.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await nextTick()
    expect(trigger.attributes('aria-expanded')).toBe('false')

    await trigger.trigger('click')
    await nextTick()
    state.route.fullPath = '/profile'
    state.route.meta.navKey = NAV_KEYS.PROFILE
    await nextTick()
    expect(trigger.attributes('aria-expanded')).toBe('false')

    await trigger.trigger('click')
    await nextTick()
    state.auth.workspaceRole = 'STUDENT'
    await nextTick()
    expect(wrapper.findAll('button.app-navigation-rail__item')).toHaveLength(1)
  })
})
