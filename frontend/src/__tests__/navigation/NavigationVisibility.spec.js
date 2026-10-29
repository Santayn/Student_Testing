import { beforeEach, describe, expect, it } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useNavigationUiStore } from '@/stores/navigationUi'

beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
})

describe('navigation visibility preferences', () => {
  it('initializes both large-screen surfaces as visible by default', () => {
    const ui = useNavigationUiStore()
    ui.init()
    expect(ui.sidebarVisible).toBe(true)
    expect(ui.railVisible).toBe(true)
  })

  it('keeps independent persisted sidebar and rail visibility', () => {
    const ui = useNavigationUiStore()
    ui.init()
    ui.hideSidebar()
    expect(ui.sidebarVisible).toBe(false)
    expect(ui.railVisible).toBe(true)
    ui.hideRail()
    ui.showSidebar()
    expect(localStorage.getItem('st:navigation:sidebar-visible')).toBe('true')
    expect(localStorage.getItem('st:navigation:rail-visible')).toBe('false')

    setActivePinia(createPinia())
    const restored = useNavigationUiStore()
    restored.init()
    expect(restored.sidebarVisible).toBe(true)
    expect(restored.railVisible).toBe(false)
  })

  it('migrates the old icon-only sidebar preference to a fully hidden state', () => {
    localStorage.setItem('st:navigation:sidebar-collapsed', 'true')
    const ui = useNavigationUiStore()
    ui.init()
    expect(ui.sidebarVisible).toBe(false)
    expect(ui.railVisible).toBe(true)
    ui.showSidebar()
    expect(localStorage.getItem('st:navigation:sidebar-visible')).toBe('true')
  })

  it('prefers the new visibility key over the deprecated collapsed flag', () => {
    localStorage.setItem('st:navigation:sidebar-collapsed', 'true')
    localStorage.setItem('st:navigation:sidebar-visible', 'true')
    localStorage.setItem('st:navigation:rail-visible', 'false')
    const ui = useNavigationUiStore()
    ui.init()
    expect(ui.sidebarVisible).toBe(true)
    expect(ui.railVisible).toBe(false)
    localStorage.setItem('st:navigation:rail-visible', 'true')
    ui.init()
    expect(ui.railVisible).toBe(false)
  })
})
