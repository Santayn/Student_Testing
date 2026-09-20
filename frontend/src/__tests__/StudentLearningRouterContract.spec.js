import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  LEARNING_ROLES,
  STUDENT_LEARNING_ROLES,
  TEST_TAKER_ROLES,
  WORKSPACE_ROLES,
} from '@/router/roles'
import {
  studentRoutes,
} from '@/router/routes/student'

function route(name) {
  const value = studentRoutes.find(
    (item) => item.name === name
  )

  expect(value).toBeDefined()
  return value
}

describe('student public-learning router contract', () => {
  it('keeps public-learning and test-taking roles student-only', () => {
    expect(STUDENT_LEARNING_ROLES).toEqual([
      'STUDENT',
    ])
    expect(TEST_TAKER_ROLES).toEqual([
      'STUDENT',
    ])
  })

  it.each([
    'subject-lectures',
    'lecture-details',
  ])('protects %s with the student-only learning roles', (name) => {
    const value = route(name)

    expect(value.meta).toMatchObject({
      requiresAuth: true,
      roles: STUDENT_LEARNING_ROLES,
    })
  })

  it('protects the test route with student-only test-taking roles', () => {
    const value = route('test')

    expect(value.meta).toMatchObject({
      requiresAuth: true,
      roles: TEST_TAKER_ROLES,
    })
  })

  it.each([
    'subjects',
    'subject-details',
    'results',
  ])('keeps %s available to role-aware learning views', (name) => {
    const value = route(name)

    expect(value.meta).toMatchObject({
      requiresAuth: true,
      roles: LEARNING_ROLES,
    })
  })

  it.each([
    'home',
    'profile',
  ])('requires workspace roles for %s', (name) => {
    const value = route(name)

    expect(value.meta).toMatchObject({
      requiresAuth: true,
      roles: WORKSPACE_ROLES,
    })
  })
})
