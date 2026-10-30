// @vitest-environment node
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'
import {
  NAVIGATION_MOTION_PRESETS,
  navigationMotionClass,
  navigationTransitionName,
} from '@/config/motionPresets'

const source = (path) => readFileSync(resolve(process.cwd(), `src/${path}`), 'utf8')
const css = source('theme/navigation-motion.css')

const navigationFiles = {
  shell: source('App.vue'),
  header: source('components/layout/AppHeader.vue'),
  sidebar: source('components/layout/AppSidebar.vue'),
  rail: source('components/layout/AppNavigationRail.vue'),
  bottom: source('components/layout/AppBottomNavigation.vue'),
  disclosure: source('components/layout/NavigationDisclosureGroup.vue'),
  flyout: source('components/layout/NavigationRailFlyout.vue'),
  sheet: source('components/layout/NavigationMoreSheet.vue'),
  root: source('components/layout/NavigationRootLink.vue'),
}

describe('central navigation motion API', () => {
  it('keeps a whitelisted preset for every navigation surface', () => {
    expect(Object.keys(NAVIGATION_MOTION_PRESETS)).toEqual([
      'surface', 'header', 'disclosure', 'flyout', 'sheet', 'bottom', 'item',
    ])
    expect(navigationMotionClass('bottom')).toBe('st-nav-motion--bottom-slide')
    expect(navigationMotionClass('item')).toBe('st-nav-motion--item-highlight')
    expect(navigationMotionClass('sheet')).toBe('st-nav-motion--sheet-slide-up')
    expect(navigationMotionClass('unknown')).toBe('')
    expect(navigationTransitionName('surface')).toBe('st-nav-surface')
    expect(navigationTransitionName('disclosure')).toBe('st-nav-disclosure')
    expect(navigationTransitionName('flyout')).toBe('st-nav-flyout')
    expect(navigationTransitionName('item')).toBe('')
  })

  it('loads the shared navigation motion CSS after the general motion layer', () => {
    const main = source('main.js')
    expect(main).toContain("import '@/theme/navigation-motion.css'")
    expect(main.indexOf("import '@/theme/motion.css'")).toBeLessThan(
      main.indexOf("import '@/theme/navigation-motion.css'")
    )
    for (const name of ['surface', 'disclosure', 'flyout']) {
      expect(css).toContain(`.st-nav-${name}-enter-active`)
      expect(css).toContain(`.st-nav-${name}-leave-active`)
    }
  })

  it('applies shared presets to each shell surface and navigational item', () => {
    expect(navigationFiles.shell).toContain("navigationTransitionName('surface')")
    expect(navigationFiles.header).toContain("navigationMotionClass('header')")
    expect(navigationFiles.sidebar).toContain("navigationMotionClass('item')")
    expect(navigationFiles.rail).toContain("navigationMotionClass('item')")
    expect(navigationFiles.bottom).toContain("navigationMotionClass('bottom')")
    expect(navigationFiles.bottom).toContain("navigationMotionClass('item')")
    expect(navigationFiles.disclosure).toContain("navigationTransitionName('disclosure')")
    expect(navigationFiles.flyout).toContain("navigationTransitionName('flyout')")
    expect(navigationFiles.sheet).toContain("navigationMotionClass('sheet')")
    expect(navigationFiles.root).toContain("navigationMotionClass('item')")
  })

  it('keeps mobile scroll auto-hide, tablet flyout focus and modal scroll lock', () => {
    expect(navigationFiles.bottom).toContain('useScrollNavigation(route)')
    expect(navigationFiles.bottom).toContain("'app-bottom-navigation--hidden': !visible && !moreOpen")
    expect(navigationFiles.rail).toContain('flyout.value?.focusFirstLink()')
    expect(navigationFiles.rail).toContain('closeFlyout({ restoreFocus: true })')
    expect(navigationFiles.sheet).toContain("document.documentElement.classList.add('navigation-sheet-open')")
    expect(navigationFiles.sheet).toContain("document.documentElement.classList.remove('navigation-sheet-open')")
    expect(navigationFiles.shell).toContain('v-if="wideNavigationShown"')
    expect(navigationFiles.shell).toContain('v-if="mediumNavigationShown"')
  })

  it('suppresses decorative motion under reduced motion settings', () => {
    expect(css).toContain('@media (prefers-reduced-motion: reduce)')
    expect(css).toContain('.st-nav-motion--header-expand .app-header__content')
    expect(css).toContain('.st-nav-motion--bottom-slide')
    expect(css).toContain('.st-nav-motion--sheet-slide-up { animation: none; }')
  })
})
