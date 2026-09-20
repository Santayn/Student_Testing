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

describe('student public-learning router contract', () => {
  it('keeps public-learning routes student-only', () => {
    const roles = source('../router/roles.js')

    expect(roles).toContain(
      'export const STUDENT_LEARNING_ROLES'
    )

    expect(roles).toMatch(
      /STUDENT_LEARNING_ROLES[\s\S]*?'STUDENT'[\s\S]*?\]\)/
    )

    expect(roles).toMatch(
      /TEST_TAKER_ROLES[\s\S]*?'STUDENT'[\s\S]*?\]\)/
    )

    const studentLearningBlock =
      roles.match(
        /STUDENT_LEARNING_ROLES[\s\S]*?\]\)/
      )?.[0] ?? ''

    const testTakerBlock =
      roles.match(
        /TEST_TAKER_ROLES[\s\S]*?\]\)/
      )?.[0] ?? ''

    expect(studentLearningBlock)
      .not.toContain("'TEACHER'")
    expect(studentLearningBlock)
      .not.toContain("'ADMIN'")
    expect(testTakerBlock)
      .not.toContain("'TEACHER'")
    expect(testTakerBlock)
      .not.toContain("'ADMIN'")
  })

  it('uses the student-only meta for lecture browsing', () => {
    const routes = source(
      '../router/routes/student.js'
    )

    expect(routes).toMatch(
      /path: '\/subjects\/:subjectId\/lectures'[\s\S]*?meta: studentLearningMeta/
    )

    expect(routes).toMatch(
      /path: '\/lectures\/:lectureId'[\s\S]*?meta: studentLearningMeta/
    )

    expect(routes).toMatch(
      /path: '\/tests\/:testId'[\s\S]*?meta: testTakingMeta/
    )
  })

  it('keeps shared subjects and results available to role-aware views', () => {
    const routes = source(
      '../router/routes/student.js'
    )

    expect(routes).toMatch(
      /path: '\/subjects'[\s\S]*?meta: learningMeta/
    )

    expect(routes).toMatch(
      /path: '\/subjects\/:subjectId'[\s\S]*?meta: learningMeta/
    )

    expect(routes).toMatch(
      /path: '\/results'[\s\S]*?meta: learningMeta/
    )
  })
})
