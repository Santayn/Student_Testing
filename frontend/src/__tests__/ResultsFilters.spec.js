import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  useResultsFilters,
} from '@/composables/useResultsFilters'

describe('useResultsFilters', () => {
  it('keeps cascade state isolated for each ResultsView instance', () => {
    const first = useResultsFilters()
    const second = useResultsFilters()

    first.subjectId.value = 17
    first.subjects.value = [{ id: 17, name: 'Математика' }]
    first.testId.value = 31

    expect(second.subjectId.value).toBe('')
    expect(second.subjects.value).toEqual([])
    expect(second.testId.value).toBe('')
  })

  it('clears every downstream teacher selection when its parent changes', () => {
    const filters = useResultsFilters()
    filters.subjectId.value = 17
    filters.subjects.value = [{ id: 17, name: 'Математика' }]
    filters.lectureId.value = 23
    filters.testId.value = 31
    filters.groupId.value = 47
    filters.studentId.value = 59
    filters.lectures.value = [{ id: 23 }]
    filters.tests.value = [{ id: 31 }]
    filters.groups.value = [{ id: 47 }]
    filters.students.value = [{ id: 59 }]

    filters.resetAfterSubject()

    expect(filters.subjectId.value).toBe(17)
    expect(filters.subjects.value).toEqual([{ id: 17, name: 'Математика' }])
    expect([
      filters.lectureId.value,
      filters.testId.value,
      filters.groupId.value,
      filters.studentId.value,
    ]).toEqual(['', '', '', ''])
    expect([
      filters.lectures.value,
      filters.tests.value,
      filters.groups.value,
      filters.students.value,
    ]).toEqual([[], [], [], []])
  })

  it('resets only descendants of lecture, test and group', () => {
    const filters = useResultsFilters()
    filters.subjectId.value = 17
    filters.lectureId.value = 23
    filters.testId.value = 31
    filters.groupId.value = 47
    filters.studentId.value = 59
    filters.lectures.value = [{ id: 23 }]
    filters.tests.value = [{ id: 31 }]
    filters.groups.value = [{ id: 47 }]
    filters.students.value = [{ id: 59 }]

    filters.resetAfterGroup()
    expect(filters.groupId.value).toBe(47)
    expect(filters.studentId.value).toBe('')
    expect(filters.students.value).toEqual([])

    filters.studentId.value = 59
    filters.resetAfterTest()
    expect(filters.testId.value).toBe(31)
    expect(filters.groupId.value).toBe('')
    expect(filters.studentId.value).toBe('')
    expect(filters.groups.value).toEqual([])

    filters.groupId.value = 47
    filters.testId.value = 31
    filters.resetAfterLecture()
    expect(filters.lectureId.value).toBe(23)
    expect(filters.testId.value).toBe('')
    expect(filters.groupId.value).toBe('')
    expect(filters.tests.value).toEqual([])
    expect(filters.subjectId.value).toBe(17)
    expect(filters.lectures.value).toEqual([{ id: 23 }])
  })

  it('derives teacher params from selected values but never adds teacher-only filters for student', () => {
    const filters = useResultsFilters()
    expect(filters.teacherParams()).toEqual({})
    expect(filters.studentParams()).toEqual({})

    filters.subjectId.value = 17
    filters.lectureId.value = 23
    filters.testId.value = 31
    filters.groupId.value = 47
    filters.studentId.value = 59

    expect(filters.teacherParams()).toEqual({
      subjectId: 17,
      lectureId: 23,
      testId: 31,
      groupId: 47,
      studentId: 59,
    })
    expect(filters.studentParams()).toEqual({ subjectId: 17, testId: 31 })
  })

  it('derives student test options only from that student result context', () => {
    const filters = useResultsFilters()
    filters.subjectId.value = 17

    filters.setStudentTestOptions({
      attempts: [
        { testId: 31, testName: 'Язык' },
        { testId: 31, testName: 'Другая попытка' },
        { testId: 42, testName: 'Алгебра' },
        { testId: 0, testName: 'Недопустимый ID' },
        { testId: 'not-a-number', testName: 'Недопустимый ID' },
      ],
    })
    expect(filters.tests.value).toEqual([
      { id: 42, title: 'Алгебра' },
      { id: 31, title: 'Язык' },
    ])

    filters.testId.value = 31
    expect(filters.selectedStudentTestIsValid()).toBe(true)
    filters.testId.value = 999
    expect(filters.selectedStudentTestIsValid()).toBe(false)
    filters.resetInvalidStudentTest()
    expect(filters.testId.value).toBe('')
    expect(filters.selectedStudentTestIsValid()).toBe(true)

    filters.testId.value = 42
    filters.resetStudentSubject()
    expect(filters.subjectId.value).toBe(17)
    expect(filters.testId.value).toBe('')
    expect(filters.tests.value).toEqual([])
  })
})
