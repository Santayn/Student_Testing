import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const state = vi.hoisted(() => ({
  auth: null,
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => state.auth,
}))

vi.mock('@/config/features', () => ({
  publicRegistrationEnabled: false,
}))

import {
  ADMIN_ROLES,
  STUDENT_LEARNING_ROLES,
  TEACHER_ROLES,
} from '@/router/roles'
import {
  authGuard,
} from '@/router/guards/auth'
import {
  adminRoutes,
} from '@/router/routes/admin'
import {
  studentRoutes,
} from '@/router/routes/student'
import {
  teacherRoutes,
} from '@/router/routes/teacher'

function authState({
  roles = [],
  workspaceRole = null,
} = {}) {
  return {
    initialized: true,
    isAuthenticated: true,
    personId: 17,
    canRefresh: false,
    workspaceRole,
    init: vi.fn(),
    refreshSession: vi.fn(),
    loadCurrentUser: vi.fn(),
    hasAnyRole(...requiredRoles) {
      return requiredRoles.some(
        (role) => roles.includes(role)
      )
    },
  }
}

function routeMeta(routes, name) {
  const route = routes.find(
    (item) => item.name === name
  )

  expect(route).toBeDefined()
  return route.meta
}

function target(meta, fullPath) {
  return {
    fullPath,
    matched: [
      {
        meta,
      },
    ],
  }
}

function forbidden(fullPath) {
  return {
    name: 'forbidden',
    query: {
      from: fullPath,
    },
  }
}

describe('workspace-aware route guard', () => {
  beforeEach(() => {
    state.auth = authState()
  })

  it('blocks teacher routes while a STUDENT + TEACHER account is in student mode', async () => {
    const meta = routeMeta(
      teacherRoutes,
      'teacher-questions'
    )

    state.auth = authState({
      roles: ['STUDENT', 'TEACHER'],
      workspaceRole: 'STUDENT',
    })

    await expect(
      authGuard(
        target(
          meta,
          '/teacher/questions'
        )
      )
    ).resolves.toEqual(
      forbidden('/teacher/questions')
    )
  })

  it('allows the same teacher route after switching to teacher mode', async () => {
    const meta = routeMeta(
      teacherRoutes,
      'teacher-questions'
    )

    state.auth = authState({
      roles: ['STUDENT', 'TEACHER'],
      workspaceRole: 'TEACHER',
    })

    await expect(
      authGuard(
        target(
          meta,
          '/teacher/questions'
        )
      )
    ).resolves.toBe(true)
  })

  it('keeps teacher screens available in ADMIN mode where the route already allows ADMIN', async () => {
    const meta = routeMeta(
      teacherRoutes,
      'teacher-courses'
    )

    expect(meta.roles).toBe(TEACHER_ROLES)
    expect(meta.workspaceRoles).toBe(
      TEACHER_ROLES
    )

    state.auth = authState({
      roles: ['ADMIN'],
      workspaceRole: 'ADMIN',
    })

    await expect(
      authGuard(
        target(
          meta,
          '/teacher/courses'
        )
      )
    ).resolves.toBe(true)
  })

  it('blocks admin routes while an ADMIN + STUDENT account is in student mode', async () => {
    const meta = routeMeta(
      adminRoutes,
      'admin-users'
    )

    expect(meta.workspaceRoles).toBe(
      ADMIN_ROLES
    )

    state.auth = authState({
      roles: ['ADMIN', 'STUDENT'],
      workspaceRole: 'STUDENT',
    })

    await expect(
      authGuard(
        target(
          meta,
          '/admin/users'
        )
      )
    ).resolves.toEqual(
      forbidden('/admin/users')
    )
  })

  it('blocks student-only learning routes while a STUDENT + TEACHER account is in teacher mode', async () => {
    const meta = routeMeta(
      studentRoutes,
      'lecture-details'
    )

    expect(meta.workspaceRoles).toBe(
      STUDENT_LEARNING_ROLES
    )

    state.auth = authState({
      roles: ['STUDENT', 'TEACHER'],
      workspaceRole: 'TEACHER',
    })

    await expect(
      authGuard(
        target(
          meta,
          '/lectures/11'
        )
      )
    ).resolves.toEqual(
      forbidden('/lectures/11')
    )
  })

  it('does not add a workspace-mode restriction to shared results routes', async () => {
    const meta = routeMeta(
      studentRoutes,
      'results'
    )

    expect(meta.workspaceRoles).toBeUndefined()

    state.auth = authState({
      roles: ['STUDENT', 'TEACHER'],
      workspaceRole: 'STUDENT',
    })

    await expect(
      authGuard(
        target(
          meta,
          '/results'
        )
      )
    ).resolves.toBe(true)

    state.auth.workspaceRole = 'TEACHER'

    await expect(
      authGuard(
        target(
          meta,
          '/results'
        )
      )
    ).resolves.toBe(true)
  })
})
