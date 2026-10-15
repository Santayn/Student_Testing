import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  courseVersionDescription,
  courseVersionLabel,
  hidesAllLectures,
  isLectureAssignmentActive,
  studentLectureSource,
} from '@/utils/courseVersionAccess'

const now = Date.parse('2026-10-11T12:00:00Z')

function lectureAssignment(overrides = {}) {
  return {
    id: 1,
    teachingAssignmentId: 10,
    courseLectureId: 5,
    status: 1,
    availableFromUtc: null,
    closedAtUtc: null,
    ...overrides,
  }
}

describe('course version access (PublicLearningRestController rules)', () => {
  it('explicit lecture assignments win over the course version', () => {
    const source = studentLectureSource({
      courseVersionId: 3,
      lectureAssignments: [lectureAssignment()],
      versionLectureCount: 0,
      now,
    })

    expect(source).toEqual({ kind: 'assigned', count: 1 })
    expect(hidesAllLectures(source)).toBe(false)
  })

  it('an empty issued version hides all lectures', () => {
    const source = studentLectureSource({ courseVersionId: 3, versionLectureCount: 0, now })

    expect(source).toEqual({ kind: 'version', count: 0 })
    expect(hidesAllLectures(source)).toBe(true)
  })

  it('does not flag versions with lectures or with an unknown count', () => {
    expect(hidesAllLectures(studentLectureSource({ courseVersionId: 3, versionLectureCount: 4, now }))).toBe(false)
    expect(hidesAllLectures(studentLectureSource({ courseVersionId: 3, versionLectureCount: null, now }))).toBe(false)
  })

  it('without a version students see the subject lectures', () => {
    expect(studentLectureSource({ courseVersionId: null, now })).toEqual({ kind: 'subject' })
  })

  it('ignores inactive, closed and not yet started lecture assignments', () => {
    const ignored = [
      lectureAssignment({ status: 2 }),
      lectureAssignment({ closedAtUtc: '2026-10-01T00:00:00Z' }),
      lectureAssignment({ availableFromUtc: '2026-12-01T00:00:00Z' }),
    ]

    ignored.forEach((item) => expect(isLectureAssignmentActive(item, now)).toBe(false))
    expect(studentLectureSource({ courseVersionId: 3, lectureAssignments: ignored, versionLectureCount: 0, now }))
      .toEqual({ kind: 'version', count: 0 })
    expect(isLectureAssignmentActive(
      lectureAssignment({ availableFromUtc: '2026-10-01T00:00:00Z', closedAtUtc: '2026-12-01T00:00:00Z' }),
      now
    )).toBe(true)
  })

  it('labels versions with the template name and publication status', () => {
    const version = { versionNumber: 2, title: 'Осень 2026', published: true }

    expect(courseVersionLabel(version, 'Java')).toBe('Java · v2')
    expect(courseVersionLabel(version)).toBe('v2')
    expect(courseVersionDescription(version)).toBe('Осень 2026 · опубликована')
    expect(courseVersionDescription({ ...version, published: false })).toBe('Осень 2026 · черновик')
  })
})
