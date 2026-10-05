import {
  beforeEach,
  describe,
  expect,
  it,
} from 'vitest'
import {
  createPinia,
  setActivePinia,
} from 'pinia'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  getSharedLearningContextCache,
} from '@/utils/learningContextCache'

describe('workspace mode', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('discards shared learning context on mode switch, identity change and logout', async () => {
    const auth = useAuthStore()
    auth.setUser({ id: 1, personId: 17, roles: ['STUDENT', 'TEACHER'] })

    const first = getSharedLearningContextCache(auth)
    await first.load('student:17', () => ({ owner: 1 }))

    auth.setWorkspaceRole('STUDENT')
    const student = getSharedLearningContextCache(auth)
    expect(student).not.toBe(first)
    expect(first.isActive()).toBe(false)

    auth.setUser({ id: 1, personId: 18, roles: ['STUDENT', 'TEACHER'] })
    const otherPerson = getSharedLearningContextCache(auth)
    expect(otherPerson).not.toBe(student)

    auth.clearSession()
    expect(otherPerson.isActive()).toBe(false)
    expect(getSharedLearningContextCache(auth)).toBeNull()
  })

  it('uses one consistent mode for a multi-role account', () => {
    const auth = useAuthStore()

    auth.setUser({
      personId: 17,
      roles: ['STUDENT', 'TEACHER'],
    })

    expect(auth.workspaceRoles).toEqual([
      'TEACHER',
      'STUDENT',
    ])
    expect(auth.workspaceRole).toBe('TEACHER')
    expect(auth.isTeacherMode).toBe(true)
    expect(auth.isStudentMode).toBe(false)

    auth.setWorkspaceRole('STUDENT')

    expect(auth.workspaceRole).toBe('STUDENT')
    expect(auth.isStudentMode).toBe(true)
    expect(auth.isTeacherMode).toBe(false)
  })

  it('keeps a valid selected mode after /me refresh', () => {
    const auth = useAuthStore()

    auth.setUser({
      personId: 17,
      roles: ['STUDENT', 'TEACHER'],
    })
    auth.setWorkspaceRole('STUDENT')

    auth.setUser({
      personId: 17,
      roles: ['STUDENT', 'TEACHER'],
    })

    expect(auth.workspaceRole).toBe('STUDENT')
  })

  it('falls back when the selected role is removed', () => {
    const auth = useAuthStore()

    auth.setUser({
      personId: 17,
      roles: ['STUDENT', 'TEACHER'],
    })
    auth.setWorkspaceRole('STUDENT')

    auth.setUser({
      personId: 17,
      roles: ['TEACHER'],
    })

    expect(auth.workspaceRole).toBe('TEACHER')
    expect(auth.activeWorkspaceRole).toBe('TEACHER')
  })

  it('rejects switching to a role the account does not have', () => {
    const auth = useAuthStore()

    auth.setUser({
      personId: 17,
      roles: ['STUDENT'],
    })

    expect(() => {
      auth.setWorkspaceRole('ADMIN')
    }).toThrow(/недоступна/)
  })
})
