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

describe('versioned lecture content contract', () => {
  it('sends semantic content metadata from teacher lecture management', () => {
    const teacherView =
      source(
        '../views/teacher/LectureManagementView.vue'
      )

    expect(teacherView)
      .toContain('contentSource:')
    expect(teacherView)
      .toContain('contentFormat:')
    expect(teacherView)
      .toContain('contentSchemaVersion:')
  })

  it('shows the student content source without injecting raw html', () => {
    const studentView =
      source(
        '../views/lectures/LectureDetailsView.vue'
      )

    expect(studentView)
      .toContain('{{ lecture.contentSource }}')
    expect(studentView)
      .toContain('lecture.contentFormat')
    expect(studentView)
      .not.toContain('v-html="lecture.contentSource"')
  })
})
