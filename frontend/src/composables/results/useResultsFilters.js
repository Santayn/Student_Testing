import { ref } from 'vue'

/**
 * Filter state for ResultsView. The view still owns option reads, result reads,
 * and the AbortController guards; this composable only manages the cascading
 * choices and the params derived from those choices.
 */
export function useResultsFilters() {
  const subjects = ref([])
  const lectures = ref([])
  const tests = ref([])
  const groups = ref([])
  const students = ref([])

  const subjectId = ref('')
  const lectureId = ref('')
  const testId = ref('')
  const groupId = ref('')
  const studentId = ref('')

  function subjectLabel(subject) {
    return (
      subject.name ||
      `Предмет #${subject.id}`
    )
  }

  function lectureLabel(lecture) {
    const ordinal =
      lecture.ordinal ??
      '—'

    const title =
      lecture.title ||
      `Лекция #${lecture.id}`

    if (
      !lecture.courseName &&
      lecture.versionNumber == null
    ) {
      return `${ordinal}. ${title}`
    }

    const course =
      lecture.courseName ||
      'курс не указан'

    const version =
      lecture.versionNumber ??
      '—'

    return (
      `${ordinal}. ${title} ` +
      `(${course}, v${version})`
    )
  }

  function testLabel(test) {
    return (
      test.title ||
      `Тест #${test.id}`
    )
  }

  function groupLabel(group) {
    return (
      group.name ||
      group.code ||
      `Группа #${group.id}`
    )
  }

  function studentLabel(student) {
    return (
      student.fullName ||
      `Студент #${student.id}`
    )
  }

  function studentTestOptionsFromData(data) {
    const source =
      Array.isArray(data?.attempts)
        ? data.attempts
        : []

    const unique = new Map()

    source.forEach((attempt) => {
      const id =
        Number(attempt.testId)

      if (
        !Number.isInteger(id) ||
        id <= 0 ||
        unique.has(id)
      ) {
        return
      }

      unique.set(id, {
        id,
        title:
          attempt.testName ||
          `Тест #${id}`,
      })
    })

    return Array.from(
      unique.values()
    ).sort(
      (left, right) =>
        String(left.title).localeCompare(
          String(right.title),
          'ru',
          {
            sensitivity: 'base',
          }
        )
    )
  }

  function selectedStudentTestIsValid() {
    if (!testId.value) {
      return true
    }

    return tests.value.some(
      (test) =>
        String(test.id) ===
        String(testId.value)
    )
  }

  function resetAfterSubject() {
    lectureId.value = ''
    testId.value = ''
    groupId.value = ''
    studentId.value = ''

    lectures.value = []
    tests.value = []
    groups.value = []
    students.value = []
  }

  function resetAfterLecture() {
    testId.value = ''
    groupId.value = ''
    studentId.value = ''

    tests.value = []
    groups.value = []
    students.value = []
  }

  function resetAfterTest() {
    groupId.value = ''
    studentId.value = ''

    groups.value = []
    students.value = []
  }

  function resetAfterGroup() {
    studentId.value = ''
    students.value = []
  }

  function teacherParams() {
    const params = {}

    if (subjectId.value) {
      params.subjectId =
        subjectId.value
    }

    if (lectureId.value) {
      params.lectureId =
        lectureId.value
    }

    if (testId.value) {
      params.testId =
        testId.value
    }

    if (groupId.value) {
      params.groupId =
        groupId.value
    }

    if (studentId.value) {
      params.studentId =
        studentId.value
    }

    return params
  }

  function studentParams() {
    const params = {}

    if (subjectId.value) {
      params.subjectId =
        subjectId.value
    }

    if (testId.value) {
      params.testId =
        testId.value
    }

    return params
  }

  function resetStudentSubject() {
    testId.value = ''
    tests.value = []
  }

  function setStudentTestOptions(data) {
    tests.value = studentTestOptionsFromData(data)
  }

  function resetInvalidStudentTest() {
    testId.value = ''
  }

  return {
    subjects,
    lectures,
    tests,
    groups,
    students,
    subjectId,
    lectureId,
    testId,
    groupId,
    studentId,
    subjectLabel,
    lectureLabel,
    testLabel,
    groupLabel,
    studentLabel,
    resetAfterSubject,
    resetAfterLecture,
    resetAfterTest,
    resetAfterGroup,
    resetStudentSubject,
    resetInvalidStudentTest,
    setStudentTestOptions,
    selectedStudentTestIsValid,
    teacherParams,
    studentParams,
  }
}
