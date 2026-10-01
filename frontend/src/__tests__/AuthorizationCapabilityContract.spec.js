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

describe('frontend authorization capability contract', () => {
  it('supports permission requirements in the route guard', () => {
    const guard = source(
      '../router/guards/auth.js'
    )

    expect(guard)
      .toContain('meta.permissions')
    expect(guard)
      .toContain('hasAnyPermission(')
  })

  it('uses functional permissions for ordinary teacher and admin routes', () => {
    const teacherRoutes = source(
      '../router/routes/teacher.js'
    )
    const adminRoutes = source(
      '../router/routes/admin.js'
    )
    const studentRoutes = source(
      '../router/routes/student.js'
    )

    expect(teacherRoutes)
      .toContain("['tests.manage']")
    expect(teacherRoutes)
      .toContain("['questions.manage']")
    expect(teacherRoutes)
      .toContain("['courses.manage']")
    expect(teacherRoutes)
      .toContain("['teaching.manage']")

    expect(adminRoutes)
      .toContain("['users.read']")
    expect(adminRoutes)
      .toContain("['academic.manage']")

    expect(adminRoutes)
      .toContain("roles: ['ADMIN']")
    expect(adminRoutes)
      .toContain("name: 'admin-database-backups'")

    expect(studentRoutes)
      .toContain("'lectures.read'")
    expect(studentRoutes)
      .toContain("'tests.manage'")
    expect(studentRoutes)
      .toContain("roles: ['STUDENT']")
  })

  it('filters navigation by permissions instead of broad role names', () => {
    const sidebar = source(
      '../components/layout/AppSidebar.vue'
    )
    const home = source(
      '../views/HomeView.vue'
    )

    expect(sidebar)
      .toContain('authStore.hasPermission(')
    expect(home)
      .toContain('visibleTeacherActions')
    expect(home)
      .toContain('visibleAdminActions')
  })
})
