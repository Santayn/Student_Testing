import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, defineComponent, h } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { mount } from '@vue/test-utils'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import { NAV_KEYS } from '@/navigation/navigation.config'
import { useNavigationUiStore } from '@/stores/navigationUi'

const state = vi.hoisted(() => ({
  route: { fullPath: '/subjects/42', meta: { navKey: 'subjects' } },
  auth: { workspaceRole: 'STUDENT' },
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

function mountSidebar() {
  return mount(AppSidebar, {
    global: { stubs: { RouterLink: RouterLinkStub } },
  })
}

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
  state.auth.workspaceRole = 'STUDENT'
  state.route = { fullPath: '/subjects/42', meta: { navKey: NAV_KEYS.SUBJECTS } }
})

describe('mixed desktop sidebar navigation', () => {
  it('renders a single-destination group as a direct root link', () => {
    const wrapper = mountSidebar()
    expect(wrapper.findAll('.navigation-group__trigger')).toHaveLength(1)
    expect(wrapper.findAll('.navigation-root-link')).toHaveLength(1)
    expect(wrapper.find('.navigation-root-link').text()).toBe('Профиль')
    expect(wrapper.find('.navigation-root-link').attributes('href')).toBe('/profile')
    expect(wrapper.find('.app-sidebar__link--active').text()).toBe('Предметы')
    expect(wrapper.find('.app-sidebar__link--active').attributes('aria-current')).toBe('page')
    wrapper.unmount()
  })

  it('allows several multi-item groups to remain expanded', async () => {
    state.auth.workspaceRole = 'TEACHER'
    state.route = { fullPath: '/teacher/questions', meta: { navKey: NAV_KEYS.TEACHER_QUESTIONS } }
    const wrapper = mountSidebar()
    const toggles = wrapper.findAll('.navigation-group__trigger')
    expect(toggles).toHaveLength(2)
    expect(toggles[1].attributes('aria-expanded')).toBe('true')
    await toggles[0].trigger('click')
    expect(toggles[0].attributes('aria-expanded')).toBe('true')
    expect(toggles[1].attributes('aria-expanded')).toBe('true')
    expect(wrapper.findAll('.navigation-root-link')).toHaveLength(2)
    wrapper.unmount()
  })

  it('retains expansion only for actual groups and restores them', async () => {
    state.auth.workspaceRole = 'TEACHER'
    state.route = { fullPath: '/teacher/questions', meta: { navKey: NAV_KEYS.TEACHER_QUESTIONS } }
    const wrapper = mountSidebar()
    await wrapper.findAll('.navigation-group__trigger')[0].trigger('click')
    await nextTick()
    wrapper.unmount()
    const restored = mountSidebar()
    expect(restored.findAll('.navigation-group__trigger')[0].attributes('aria-expanded')).toBe('true')
    expect(restored.findAll('.navigation-root-link')).toHaveLength(2)
    restored.unmount()
  })

  it('uses the admin role menu and keeps Profile at the root', () => {
    state.auth.workspaceRole = 'ADMIN'
    state.route = { fullPath: '/admin/faculties', meta: { navKey: NAV_KEYS.ADMIN_FACULTIES } }
    const wrapper = mountSidebar()
    expect(wrapper.findAll('.navigation-group__trigger')).toHaveLength(6)
    expect(wrapper.findAll('.navigation-root-link')).toHaveLength(1)
    expect(wrapper.find('.navigation-root-link').text()).toBe('Профиль')
    expect(wrapper.find('.app-sidebar__link--active').text()).toBe('Факультеты')
    expect(wrapper.text()).not.toContain('Создать тест')
    wrapper.unmount()
  })

  it('highlights direct root links on their own routes', () => {
    state.route = { fullPath: '/profile', meta: { navKey: NAV_KEYS.PROFILE } }
    const wrapper = mountSidebar()
    const profile = wrapper.find('.navigation-root-link--active')
    expect(profile.exists()).toBe(true)
    expect(profile.attributes('aria-current')).toBe('page')
    wrapper.unmount()
  })

  it('offers a full hide action without creating an icon-only navigation mode', async () => {
    const wrapper = mountSidebar()
    const store = useNavigationUiStore()
    expect(wrapper.find('#workspace-sidebar').exists()).toBe(true)
    expect(wrapper.find('.navigation-root-link').text()).toBe('Профиль')

    await wrapper.find('.app-sidebar__collapse').trigger('click')
    expect(store.sidebarVisible).toBe(false)
    expect(wrapper.emitted('hide')).toHaveLength(1)
    expect(localStorage.getItem('st:navigation:sidebar-visible')).toBe('false')
    expect(wrapper.find('.app-sidebar--collapsed').exists()).toBe(false)
    wrapper.unmount()
  })

})
