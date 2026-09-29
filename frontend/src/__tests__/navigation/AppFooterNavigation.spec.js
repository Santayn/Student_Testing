// @vitest-environment node

import {
  describe,
  expect,
  it,
} from 'vitest'

import footerSource from '@/components/layout/AppFooter.vue?raw'

describe('AppFooter navigation responsibility', () => {
  it('does not duplicate global workspace navigation', () => {
    expect(footerSource).not.toContain('Главная')
    expect(footerSource).not.toContain("name: 'home'")
    expect(footerSource).not.toContain('homeRoute')
  })

  it('does not depend on authentication state for navigation', () => {
    expect(footerSource).not.toContain('useAuthStore')
    expect(footerSource).not.toContain('hasWorkspaceAccess')
  })

  it('keeps only service-level navigation', () => {
    expect(footerSource).toContain('aria-label="Служебные ссылки"')
    expect(footerSource).toContain("name: 'about'")
    expect(footerSource).toContain('О системе')
  })
})
