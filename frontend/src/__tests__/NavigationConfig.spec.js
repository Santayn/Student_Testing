import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  adminRoutes,
} from '@/router/routes/admin'
import {
  studentRoutes,
} from '@/router/routes/student'
import {
  teacherRoutes,
} from '@/router/routes/teacher'

import {
  NAV_KEYS,
} from '@/navigation/navigation.config'
import {
  getActiveNavigationKey,
  getWorkspaceNavigation,
  resolveBreadcrumbs,
} from '@/navigation/navigation'

function routeNames() {
  return new Set(
    [
      ...studentRoutes,
      ...teacherRoutes,
      ...adminRoutes,
    ].map((route) => route.name)
  )
}

describe('workspace navigation config', () => {
  it('builds student navigation from the shared config', () => {
    const sections =
      getWorkspaceNavigation('STUDENT')

    expect(
      sections.map((section) => section.key)
    ).toEqual([
      'main',
      'learning',
      'account',
    ])

    expect(
      sections.flatMap(
        (section) =>
          section.items.map(
            (item) => item.key
          )
      )
    ).toEqual([
      NAV_KEYS.HOME,
      NAV_KEYS.SUBJECTS,
      NAV_KEYS.RESULTS,
      NAV_KEYS.PROFILE,
    ])
  })

  it('supports workspace-specific labels without duplicating destinations', () => {
    const teacherSubjects =
      getWorkspaceNavigation('TEACHER')
        .flatMap(
          (section) => section.items
        )
        .find(
          (item) =>
            item.key ===
            NAV_KEYS.SUBJECTS
        )

    const adminSubjects =
      getWorkspaceNavigation('ADMIN')
        .flatMap(
          (section) => section.items
        )
        .find(
          (item) =>
            item.key ===
            NAV_KEYS.SUBJECTS
        )

    expect(
      teacherSubjects?.label
    ).toBe('Мои предметы')

    expect(
      adminSubjects?.label
    ).toBe('Доступные предметы')
  })

  it('references only existing workspace routes', () => {
    const names = routeNames()

    for (const role of [
      'STUDENT',
      'TEACHER',
      'ADMIN',
    ]) {
      const items =
        getWorkspaceNavigation(role)
          .flatMap(
            (section) =>
              section.items
          )

      for (const item of items) {
        expect(
          names.has(item.route.name)
        ).toBe(true)
      }
    }
  })
})

describe('route navigation metadata', () => {
  it('uses navKey as the single sidebar participation marker', () => {
    const routes = [
      ...studentRoutes,
      ...teacherRoutes,
      ...adminRoutes,
    ]

    for (const route of routes) {
      expect(route.meta.sidebar).toBeUndefined()
      expect(route.meta.navKey).toBeTruthy()
    }
  })

  it('keeps the whole student learning flow under the subjects nav key', () => {
    const learningRouteNames = [
      'subjects',
      'subject-details',
      'subject-lectures',
      'lecture-details',
      'test',
    ]

    for (
      const routeName of
      learningRouteNames
    ) {
      const route =
        studentRoutes.find(
          (candidate) =>
            candidate.name ===
            routeName
        )

      expect(route?.meta.navKey).toBe(
        NAV_KEYS.SUBJECTS
      )

      expect(
        route?.meta.breadcrumbKey
      ).toBe(routeName)
    }
  })

  it('exposes active nav key through a single helper', () => {
    expect(
      getActiveNavigationKey({
        meta: {
          navKey: NAV_KEYS.RESULTS,
        },
      })
    ).toBe(NAV_KEYS.RESULTS)
  })
})

describe('breadcrumb config', () => {
  it('builds a subject breadcrumb with a safe id fallback', () => {
    const crumbs = resolveBreadcrumbs({
      name: 'subject-details',
      params: {
        subjectId: '42',
      },
      query: {
        facultyId: '3',
      },
      meta: {
        breadcrumbKey:
          'subject-details',
      },
    })

    expect(
      crumbs.map((crumb) => crumb.label)
    ).toEqual([
      'Предметы',
      'Предмет #42',
    ])

    expect(crumbs[0].to).toEqual({
      name: 'subjects',
      query: {
        facultyId: '3',
      },
    })

    expect(crumbs[1].current).toBe(true)
    expect(crumbs[1].to).toBeNull()
  })

  it('accepts loaded entity names for a deep lecture breadcrumb', () => {
    const crumbs = resolveBreadcrumbs(
      {
        name: 'lecture-details',
        params: {
          lectureId: '9',
        },
        query: {
          subjectId: '42',
          facultyId: '3',
        },
        meta: {
          breadcrumbKey:
            'lecture-details',
        },
      },
      {
        subjectName: 'Java',
        lectureTitle: 'ООП',
      }
    )

    expect(
      crumbs.map((crumb) => crumb.label)
    ).toEqual([
      'Предметы',
      'Java',
      'Лекции',
      'ООП',
    ])

    expect(crumbs[1].to).toEqual({
      name: 'subject-details',
      params: {
        subjectId: '42',
      },
      query: {
        facultyId: '3',
      },
    })

    expect(crumbs[2].to).toEqual({
      name: 'subject-lectures',
      params: {
        subjectId: '42',
      },
      query: {
        facultyId: '3',
      },
    })
  })

  it('keeps test breadcrumbs useful even without lecture context', () => {
    const crumbs = resolveBreadcrumbs({
      name: 'test',
      params: {
        testId: '17',
      },
      query: {
        assignmentId: '5',
      },
      meta: {
        breadcrumbKey: 'test',
      },
    })

    expect(
      crumbs.map((crumb) => crumb.label)
    ).toEqual([
      'Предметы',
      'Тест #17',
    ])
  })

  it('supports a richer test breadcrumb when page context is available', () => {
    const crumbs = resolveBreadcrumbs(
      {
        name: 'test',
        params: {
          testId: '17',
        },
        query: {},
        meta: {
          breadcrumbKey: 'test',
        },
      },
      {
        subjectId: '42',
        subjectName: 'Java',
        lectureId: '9',
        lectureTitle: 'ООП',
        testTitle: 'Наследование',
      }
    )

    expect(
      crumbs.map((crumb) => crumb.label)
    ).toEqual([
      'Предметы',
      'Java',
      'Лекции',
      'ООП',
      'Наследование',
    ])
  })

  it('can compose static teacher breadcrumbs from the same destination catalog', () => {
    const crumbs = resolveBreadcrumbs({
      name: 'teacher-test-create',
      params: {},
      query: {},
      meta: {
        breadcrumbKey:
          'teacher-test-create',
      },
    })

    expect(
      crumbs.map((crumb) => crumb.label)
    ).toEqual([
      'Вопросы',
      'Создать тест',
    ])

    expect(crumbs[0].to).toEqual({
      name: 'teacher-questions',
    })
  })
})
