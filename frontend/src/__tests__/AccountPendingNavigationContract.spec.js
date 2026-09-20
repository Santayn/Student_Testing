import {
  readFileSync,
} from 'node:fs'

import {
  describe,
  expect,
  it,
} from 'vitest'

function source(relativePath) {
  return readFileSync(
    new URL(
      relativePath,
      import.meta.url
    ),
    'utf8'
  )
}

describe('pending account navigation contract', () => {
  it('protects home and profile with workspace roles instead of the base USER role', () => {
    const roles = source('../router/roles.js')
    const routes = source('../router/routes/student.js')

    const workspaceBlock =
      roles.match(
        /WORKSPACE_ROLES[\s\S]*?\]\)/
      )?.[0] ?? ''

    expect(workspaceBlock)
      .toContain("'STUDENT'")
    expect(workspaceBlock)
      .toContain("'TEACHER'")
    expect(workspaceBlock)
      .toContain("'ADMIN'")
    expect(workspaceBlock)
      .not.toContain("'USER'")

    expect(routes)
      .toContain(
        'roles: WORKSPACE_ROLES'
      )
  })

  it('keeps incomplete authenticated users on account-pending', () => {
    const guard = source(
      '../router/guards/auth.js'
    )

    expect(guard)
      .toContain(
        '!hasWorkspaceAccess(authStore)'
      )

    expect(guard)
      .toContain(
        "name: 'account-pending'"
      )
  })

  it('routes header and footer home links through workspace readiness', () => {
    const header = source(
      '../components/layout/AppHeader.vue'
    )

    const footer = source(
      '../components/layout/AppFooter.vue'
    )

    expect(header)
      .toContain(
        'hasWorkspaceAccess('
      )

    expect(header)
      .toContain(
        'authStore.isAuthenticated && accountReady'
      )

    expect(footer)
      .toContain(
        'hasWorkspaceAccess(authStore)'
      )

    expect(footer)
      .toContain(
        "name: 'account-pending'"
      )
  })
})
