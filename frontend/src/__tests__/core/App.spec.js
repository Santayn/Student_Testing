import { describe, it, expect, vi } from 'vitest'
import { nextTick } from 'vue'

import { mount } from '@vue/test-utils'
import App from '@/App.vue'
import { createPinia, setActivePinia } from 'pinia'
import { useLayoutStore } from '@/stores/layout'
import { useNavigationUiStore } from '@/stores/navigationUi'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    name: 'home',
    path: '/',
    fullPath: '/',
    meta: {
      navKey: 'home',
    },
  }),
}))

describe('App', () => {
  it('renders the routed application shell', () => {
    setActivePinia(createPinia())
    useLayoutStore().width = 1280
    const wrapper = mount(App, {
      global: {
        stubs: {
          AppHeader: true,
          AppSidebar: true,
          AppNavigationRail: true,
          AppBottomNavigation: true,
          AppBreadcrumb: true,
          AppFooter: true,
          RouterView: true,
        },
      },
    })

    expect(wrapper.find('.app-shell').exists()).toBe(true)
    expect(wrapper.find('.app-body').classes()).toContain(
      'app-body--with-sidebar'
    )
    expect(wrapper.find('app-sidebar-stub').exists()).toBe(true)
    expect(wrapper.find('app-breadcrumb-stub').exists()).toBe(true)
    expect(wrapper.find('.app-route-content').exists()).toBe(true)
    expect(wrapper.find('router-view-stub').exists()).toBe(true)
    wrapper.unmount()
  })

  it('switches navigation surfaces without rerouting the page', async () => {
    setActivePinia(createPinia())
    const layout = useLayoutStore()
    layout.width = 1200
    const wrapper = mount(App, {
      global: { stubs: {
        AppHeader: true, AppSidebar: true, AppNavigationRail: true,
        AppBottomNavigation: true, AppBreadcrumb: true,
        AppFooter: true, RouterView: true,
      } },
    })
    expect(wrapper.find('app-sidebar-stub').exists()).toBe(true)
    layout.width = 800
    await nextTick()
    expect(wrapper.find('app-navigation-rail-stub').exists()).toBe(true)
    expect(wrapper.find('app-sidebar-stub').exists()).toBe(false)
    layout.width = 500
    await nextTick()
    expect(wrapper.find('app-bottom-navigation-stub').exists()).toBe(true)
    expect(wrapper.find('app-navigation-rail-stub').exists()).toBe(false)
    wrapper.unmount()
  })

  it('releases desktop and tablet width while keeping the routed page mounted', async () => {
    setActivePinia(createPinia())
    const layout = useLayoutStore()
    const navigation = useNavigationUiStore()
    layout.width = 1200
    navigation.hideSidebar()
    const wrapper = mount(App, {
      global: { stubs: {
        AppHeader: true, AppSidebar: true, AppNavigationRail: true,
        AppBottomNavigation: true, AppBreadcrumb: true,
        AppFooter: true, RouterView: true,
      } },
    })
    const routeContent = wrapper.find('router-view-stub').element
    expect(wrapper.find('app-sidebar-stub').exists()).toBe(false)
    expect(wrapper.find('.app-body--with-sidebar').exists()).toBe(false)
    expect(wrapper.find('.app-navigation-reveal').attributes('aria-label')).toBe('Показать боковую панель')

    await wrapper.find('.app-navigation-reveal').trigger('click')
    await nextTick()
    expect(navigation.sidebarVisible).toBe(true)
    expect(wrapper.find('app-sidebar-stub').exists()).toBe(true)
    expect(wrapper.find('.app-navigation-reveal').exists()).toBe(false)
    expect(wrapper.find('router-view-stub').element).toBe(routeContent)

    layout.width = 800
    navigation.hideRail()
    await nextTick()
    expect(wrapper.find('app-navigation-rail-stub').exists()).toBe(false)
    expect(wrapper.find('.app-navigation-reveal').attributes('aria-label')).toBe('Показать панель навигации')
    expect(wrapper.find('.app-body--with-sidebar').exists()).toBe(false)
    await wrapper.find('.app-navigation-reveal').trigger('click')
    await nextTick()
    expect(navigation.railVisible).toBe(true)
    expect(wrapper.find('app-navigation-rail-stub').exists()).toBe(true)
    expect(wrapper.find('router-view-stub').element).toBe(routeContent)
    wrapper.unmount()
  })
})
