import {
  readFileSync,
} from 'node:fs'
import { resolve } from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    resolve(process.cwd(), 'src', relativePath),
    'utf8'
  )
}

describe('auth/profile audit closure contracts', () => {
  it('keeps Login aligned with the backend field contract and design tokens', () => {
    const login = source('views/auth/LoginView.vue')

    expect(login).toContain('maxlength="100"')
    expect(login).toContain('minlength="6"')
    expect(login).toContain('maxlength="200"')
    expect(login).toContain('aria-pressed')
    expect(login).toContain('var(--st-primary)')
    expect(login).not.toContain('var(--brand)')
    expect(login).not.toContain('var(--surface)')
    expect(login).not.toContain('var(--text)')
  })

  it('keeps Profile user-facing and removes technical identifier surfaces', () => {
    const profile = source('views/ProfileView.vue')
    const profileContext = source('composables/profile/useProfileContext.js')

    expect(profile).toContain('Дата рождения')
    expect(profile).toContain('Безопасность')
    expect(profile).toContain('Изменить пароль')
    expect(profile).toContain('useProfileContext({')
    expect(profileContext).toContain('authStore.refreshIdentity()')
    expect(profile).toContain('var(--st-surface)')

    expect(profile).not.toContain('Person ID')
    expect(profile).not.toContain('GroupMembership')
    expect(profile).not.toContain('aria-selected')
    expect(profile).not.toContain('var(--brand)')
    expect(profile).not.toContain('var(--surface)')
    expect(profile).not.toContain('var(--text)')
  })

  it('uses shared teacher catalogs without per-entity lookups in Profile', () => {
    const profile = source('views/ProfileView.vue')
    const profileContext = source('composables/profile/useProfileContext.js')
    const teacherContext = source('utils/teacherSubjectContext.js')

    // Profile delegates learning-context aggregation to the composable.
    // Shared catalog deduplication is covered separately by
    // StudentTeacherSharedContext.spec.js.
    expect(profile).toContain('useProfileContext({')
    expect(profileContext).toContain('loadTeacherSubjectContext({')
    expect(teacherContext).toContain('subjectsApi.getAll()')
    expect(profileContext).toContain('groupsApi.getAll()')
    expect(profileContext).toContain("cache.load('groups:catalog'")
    expect(profileContext).not.toContain('subjectsApi.getById(')
    expect(profileContext).not.toContain('groupsApi.getById(')
  })
})
