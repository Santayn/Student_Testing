// @vitest-environment node

import {
  readFileSync,
  readdirSync,
} from 'node:fs'
import {
  resolve,
} from 'node:path'

import {
  describe,
  expect,
  it,
} from 'vitest'

function readView(relativePath) {
  return readFileSync(
    resolve(
      process.cwd(),
      'src/views',
      relativePath
    ),
    'utf8'
  )
}

function readAllViews(directory) {
  return readdirSync(directory, {
    withFileTypes: true,
  }).flatMap((entry) => {
    const path = resolve(
      directory,
      entry.name
    )

    if (entry.isDirectory()) {
      return readAllViews(path)
    }

    return entry.name.endsWith('.vue')
      ? [readFileSync(path, 'utf8')]
      : []
  })
}

const subjectDetails = readView(
  'subjects/SubjectDetailsView.vue'
)
const subjectLectures = readView(
  'lectures/SubjectLecturesView.vue'
)
const lectureDetails = readView(
  'lectures/LectureDetailsView.vue'
)
const testView = readView(
  'tests/TestView.vue'
)
const notFoundView = readView(
  'NotFoundView.vue'
)
const forbiddenView = readView(
  'ForbiddenView.vue'
)
const teacherWorkload = readView(
  'teacher/TeacherWorkloadView.vue'
)

describe('contextual navigation responsibility', () => {
  it('does not keep nondeterministic browser-back actions in application views', () => {
    const views = readAllViews(
      resolve(
        process.cwd(),
        'src/views'
      )
    )

    for (const view of views) {
      expect(view).not.toContain('router.back()')
      expect(view).not.toContain('$router.back()')
      expect(view).not.toContain('history.back()')
    }
  })

  it('leaves student hierarchy navigation to breadcrumbs', () => {
    expect(subjectDetails).not.toContain('К предметам')
    expect(subjectDetails).not.toContain('Вернуться к списку')
    expect(subjectLectures).not.toContain('К предмету')
    expect(lectureDetails).not.toContain('К списку лекций')
    expect(testView).not.toContain('@click="goBack"')
    expect(testView).not.toContain('function goBack')
    expect(testView).not.toContain('useRouter')
    expect(teacherWorkload).not.toContain("name: 'teacher-lectures'")
  })

  it('keeps useful actions tied to the current entity', () => {
    expect(subjectDetails).toContain('studentLecturesRoute')
    expect(subjectDetails).toContain('teacherLecturesRoute')
    expect(subjectDetails).toContain(':to="teacherTopicsRoute"')

    expect(subjectLectures).toContain('@click="loadLectures"')
    expect(lectureDetails).toContain('@click="loadLecture"')
    expect(lectureDetails).toContain(':to="testRoute(testItem)"')
    expect(testView).toContain('@click="submitTest"')
    expect(teacherWorkload).toContain('@click="assignLectures"')
  })

  it('keeps explicit safe exits on error pages', () => {
    expect(notFoundView).toContain(':to="primaryRoute"')
    expect(forbiddenView).toContain(':to="{ name: \'home\' }"')
    expect(forbiddenView).toContain(':to="{ name: \'profile\' }"')
  })
})
