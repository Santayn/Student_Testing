import {
  onBeforeUnmount,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  lecturesApi,
  resultsApi,
  subjectsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createAbortableRequestGuard,
} from '@/utils/latestRequest'

export function useResultsContextOptions({
  authStore,
  teacherMode,
  subjects,
  lectures,
  tests,
  groups,
  students,
  subjectId,
  lectureId,
  testId,
  groupId,
  resetAfterSubject,
  resetAfterLecture,
  resetAfterTest,
  resetAfterGroup,
  resetStudentSubject,
  invalidateResults,
  loadStudentContextResults,
  disposeResultsData,
  error,
}) {
  const loadingInitial = ref(false)
  const loadingOptions = ref(false)

  // These option reads belong to this view only. Shared context requests must
  // not be aborted when one of their consumers leaves a page.
  const initialRequest = createAbortableRequestGuard()
  const optionsRequest = createAbortableRequestGuard()

  async function loadTeacherSubjects(signal) {
    const response = authStore.isAdminMode
      ? await subjectsApi.getAll({ signal })
      : await resultsApi.getTeacherSubjects({ signal })

    return listFromResponse(response)
  }

  async function loadStudentSubjects(signal) {
    const response = await resultsApi.getStudentSubjects({ signal })
    return listFromResponse(response)
  }

  async function onSubjectChange() {
    invalidateResults()
    error.value = ''

    if (!teacherMode.value) {
      optionsRequest.invalidate()
      loadingOptions.value = false
      resetStudentSubject()
      await loadStudentContextResults()
      return
    }

    const { requestId, signal } = optionsRequest.begin()
    resetAfterSubject()

    const currentSubjectId = subjectId.value
    if (!currentSubjectId) {
      loadingOptions.value = false
      return
    }

    loadingOptions.value = true

    try {
      const response = authStore.isAdminMode
        ? await lecturesApi.getAll({ subjectId: currentSubjectId }, { signal })
        : await resultsApi.getTeacherLectures(currentSubjectId, { signal })

      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      lectures.value = listFromResponse(response)
    } catch (requestError) {
      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      error.value = getApiErrorMessage(
        requestError,
        'Не удалось загрузить лекции.'
      )
    } finally {
      if (optionsRequest.isCurrent(requestId)) {
        loadingOptions.value = false
      }
    }
  }

  async function onLectureChange() {
    invalidateResults()
    const { requestId, signal } = optionsRequest.begin()
    resetAfterLecture()
    error.value = ''

    const currentLectureId = lectureId.value
    if (!currentLectureId) {
      loadingOptions.value = false
      return
    }

    loadingOptions.value = true

    try {
      const response = authStore.isAdminMode
        ? await lecturesApi.getTests(currentLectureId, { signal })
        : await resultsApi.getTeacherTests(currentLectureId, { signal })

      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      tests.value = listFromResponse(response)
    } catch (requestError) {
      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      error.value = getApiErrorMessage(
        requestError,
        'Не удалось загрузить тесты.'
      )
    } finally {
      if (optionsRequest.isCurrent(requestId)) {
        loadingOptions.value = false
      }
    }
  }

  async function onTestChange() {
    invalidateResults()
    const { requestId, signal } = optionsRequest.begin()
    resetAfterTest()
    error.value = ''

    const currentTestId = testId.value
    if (!currentTestId) {
      loadingOptions.value = false
      return
    }

    loadingOptions.value = true

    try {
      const response = await resultsApi.getTeacherGroups(currentTestId, { signal })

      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      groups.value = listFromResponse(response)
    } catch (requestError) {
      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      error.value = getApiErrorMessage(
        requestError,
        'Не удалось загрузить группы.'
      )
    } finally {
      if (optionsRequest.isCurrent(requestId)) {
        loadingOptions.value = false
      }
    }
  }

  async function onGroupChange() {
    invalidateResults()
    const { requestId, signal } = optionsRequest.begin()
    resetAfterGroup()
    error.value = ''

    const currentGroupId = groupId.value
    if (!currentGroupId) {
      loadingOptions.value = false
      return
    }

    loadingOptions.value = true

    try {
      const response = await resultsApi.getTeacherStudents(currentGroupId, { signal })

      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      students.value = listFromResponse(response)
    } catch (requestError) {
      if (!optionsRequest.isCurrent(requestId)) {
        return
      }

      error.value = getApiErrorMessage(
        requestError,
        'Не удалось загрузить студентов.'
      )
    } finally {
      if (optionsRequest.isCurrent(requestId)) {
        loadingOptions.value = false
      }
    }
  }

  async function init() {
    const { requestId, signal } = initialRequest.begin()
    optionsRequest.invalidate()
    invalidateResults()
    loadingOptions.value = false
    loadingInitial.value = true
    error.value = ''

    try {
      if (teacherMode.value) {
        const freshSubjects = await loadTeacherSubjects(signal)
        if (initialRequest.isCurrent(requestId)) {
          subjects.value = freshSubjects
        }
      } else {
        const freshSubjects = await loadStudentSubjects(signal)
        if (initialRequest.isCurrent(requestId)) {
          subjects.value = freshSubjects
          await loadStudentContextResults()
        }
      }
    } catch (requestError) {
      if (initialRequest.isCurrent(requestId)) {
        error.value = getApiErrorMessage(
          requestError,
          'Не удалось инициализировать страницу результатов.'
        )
      }
    } finally {
      if (initialRequest.isCurrent(requestId)) {
        loadingInitial.value = false
      }
    }
  }

  function dispose() {
    initialRequest.invalidate()
    optionsRequest.invalidate()
    disposeResultsData()
  }

  onBeforeUnmount(dispose)

  return {
    loadingInitial,
    loadingOptions,
    init,
    onSubjectChange,
    onLectureChange,
    onTestChange,
    onGroupChange,
    dispose,
  }
}
