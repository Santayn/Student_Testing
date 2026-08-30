import { describe, it, expect, vi } from 'vitest'

import { mount } from '@vue/test-utils'
import App from '../App.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    meta: {
      sidebar: true,
    },
  }),
}))

describe('App', () => {
  it('renders the routed application shell', () => {
    const wrapper = mount(App, {
      global: {
        stubs: {
          AppHeader: true,
          AppSidebar: true,
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
    expect(wrapper.find('router-view-stub').exists()).toBe(true)
  })
})
