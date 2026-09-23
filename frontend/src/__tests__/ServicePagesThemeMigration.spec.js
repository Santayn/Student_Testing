// @vitest-environment node

import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { describe, expect, it } from 'vitest'

const srcDir = resolve(dirname(fileURLToPath(import.meta.url)), '..')

function source(relativePath) {
  return readFileSync(resolve(srcDir, relativePath), 'utf8')
}

const serviceViews = [
  'views/auth/RegisterView.vue',
  'views/auth/AccountPendingView.vue',
  'views/auth/RequireAuthView.vue',
  'views/ForbiddenView.vue',
  'views/NotFoundView.vue',
  'views/AboutView.vue',
]

const tokens = source('theme/tokens.css')
const declaredTokens = new Set(
  Array.from(tokens.matchAll(/(--st-[\w-]+)\s*:/g), ([, token]) => token),
)

describe('service pages theme migration', () => {
  it.each(serviceViews)('uses only defined Student Testing tokens in %s', (view) => {
    const content = source(view)
    const references = Array.from(
      content.matchAll(/var\((--[\w-]+)/g),
      ([, token]) => token,
    )

    expect(references.length).toBeGreaterThan(0)
    for (const token of references) {
      expect(token, `${view} still uses a legacy CSS variable`).toMatch(/^--st-/)
      expect(declaredTokens.has(token), `${view} uses undeclared ${token}`).toBe(true)
    }
  })

  it('keeps Register in the Login card family without changing auth routing', () => {
    const register = source('views/auth/RegisterView.vue')
    const login = source('views/auth/LoginView.vue')

    expect(register).toContain('UiCard class="auth-card"')
    expect(register).toContain('class="auth-page"')
    expect(login).toContain('UiCard class="auth-card"')
    expect(register).toContain("name: 'account-pending'")
    expect(register).toContain('hasWorkspaceAccess(authStore)')
    expect(register).toContain('@submit.prevent="submit"')
    expect(register).toContain(':disabled="authStore.registering"')
  })

  it('has explicit dark palette definitions for service-page semantic colors', () => {
    const dark = tokens.split("html[data-theme='dark']")[1]
    expect(dark).toBeTruthy()

    for (const token of [
      '--st-primary', '--st-primary-soft', '--st-primary-soft-text',
      '--st-surface', '--st-surface-muted', '--st-text', '--st-text-secondary',
      '--st-border', '--st-warning', '--st-warning-text', '--st-warning-soft',
      '--st-danger',
    ]) {
      expect(dark, `missing dark definition for ${token}`)
        .toMatch(new RegExp(`${token}\\s*:`))
    }
  })
})
