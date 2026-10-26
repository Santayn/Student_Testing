import {
  describe,
  expect,
  it,
} from 'vitest'

import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, h } from 'vue'
import {
  createMemoryHistory,
  createRouter,
  RouterView,
} from 'vue-router'

import { useOverlayForm } from '@/composables/shared/useOverlayForm'
import { useUnsavedNavigationGuard } from '@/composables/shared/useUnsavedNavigationGuard'

let overlay = null

const EditorPage = defineComponent({
  setup() {
    overlay = useOverlayForm({ createDefault: () => ({ title: '' }) })
    return () => h('div', 'editor')
  },
})

const OtherPage = defineComponent({ render: () => h('div', 'other') })

async function mountWithRouter(page = EditorPage) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/editor', component: page },
      { path: '/other', component: OtherPage },
    ],
  })
  router.push('/editor')
  await router.isReady()
  const wrapper = mount(defineComponent({ render: () => h(RouterView) }), {
    global: { plugins: [router] },
  })
  await flushPromises()
  return { router, wrapper }
}

describe('unsaved changes on navigation', () => {
  it('asks before leaving the page with an edited overlay form', async () => {
    const { router } = await mountWithRouter()
    overlay.openEdit({ title: 'Лекция' })
    overlay.form.title = 'Лекция 2'

    router.push('/other')
    await flushPromises()
    expect(overlay.confirmCloseVisible.value).toBe(true)
    expect(router.currentRoute.value.path).toBe('/editor')

    overlay.continueEditing()
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/editor')
    expect(overlay.form.title).toBe('Лекция 2')

    router.push('/other')
    await flushPromises()
    overlay.discardAndClose()
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/other')
  })

  it('leaves freely when the form is unchanged or closed', async () => {
    const { router } = await mountWithRouter()
    overlay.openEdit({ title: 'Лекция' })

    await router.push('/other')
    expect(router.currentRoute.value.path).toBe('/other')
    expect(overlay.confirmCloseVisible.value).toBe(false)
  })

  it('page guard blocks leaving while dirty', async () => {
    let guard = null
    const Page = defineComponent({
      setup() {
        guard = useUnsavedNavigationGuard(true)
        return () => h('div')
      },
    })
    const { router } = await mountWithRouter(Page)

    router.push('/other')
    await flushPromises()
    expect(guard.confirmVisible.value).toBe(true)
    guard.discardAndNavigate()
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/other')
  })
})
