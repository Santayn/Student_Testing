import { ref, unref } from 'vue'

import {
  getApiErrorMessage,
  resultsApi,
} from '@/api'

import {
  createAbortableRequestGuard,
} from '@/utils/latestRequest'

import {
  sanitizeStudentResultData,
} from '@/utils/resultContracts'

/**
 * Owns ResultsView result reads only. Filter-option reads stay in ResultsView,
 * because they form a separate cascade with their own AbortController guard.
 */
export function useResultsData({
  teacherMode,
  subjectId,
  testId,
  teacherParams,
  studentParams,
  setStudentTestOptions,
  selectedStudentTestIsValid,
  resetInvalidStudentTest,
  error,
}) {
  const loadingResults = ref(false)
  const resultData = ref(null)
  const resultsRequest = createAbortableRequestGuard()

  function clearResultState() {
    resultData.value = null
    error.value = ''
  }

  function invalidateResults() {
    resultsRequest.invalidate()
    loadingResults.value = false
    resultData.value = null
  }

  function applyStudentResultData(data) {
    resultData.value = sanitizeStudentResultData(data)
  }

  async function loadStudentContextResults() {
    const { requestId, signal } = resultsRequest.begin()

    loadingResults.value = true
    clearResultState()

    const currentSubjectId = subjectId.value

    try {
      const params = currentSubjectId
        ? { subjectId: currentSubjectId }
        : {}

      const response = await resultsApi.getStudentData(params, { signal })

      if (!resultsRequest.isCurrent(requestId)) {
        return
      }

      applyStudentResultData(response.data)
      setStudentTestOptions(resultData.value)
    } catch (requestError) {
      if (!resultsRequest.isCurrent(requestId)) {
        return
      }

      resultData.value = null
      setStudentTestOptions(null)
      error.value = getApiErrorMessage(
        requestError,
        'Не удалось загрузить результаты тестирования.'
      )
    } finally {
      if (resultsRequest.isCurrent(requestId)) {
        loadingResults.value = false
      }
    }
  }

  async function loadResults() {
    const { requestId, signal } = resultsRequest.begin()

    loadingResults.value = true
    clearResultState()

    const useTeacherMode = Boolean(unref(teacherMode))
    const params = useTeacherMode
      ? teacherParams()
      : studentParams()

    try {
      if (
        !useTeacherMode &&
        testId.value &&
        !selectedStudentTestIsValid()
      ) {
        throw new Error(
          'Выбранный testId отсутствует в текущем списке результатов студента.'
        )
      }

      const response = useTeacherMode
        ? await resultsApi.getTeacherData(params, { signal })
        : await resultsApi.getStudentData(params, { signal })

      if (!resultsRequest.isCurrent(requestId)) {
        return
      }

      if (useTeacherMode) {
        resultData.value = response.data ?? {
          stats: {
            total: 0,
            right: 0,
            percent: 0,
          },
          attempts: [],
        }
      } else {
        applyStudentResultData(response.data)
      }
    } catch (requestError) {
      if (!resultsRequest.isCurrent(requestId)) {
        return
      }

      resultData.value = null
      error.value = getApiErrorMessage(
        requestError,
        'Не удалось загрузить результаты тестирования.'
      )
    } finally {
      if (resultsRequest.isCurrent(requestId)) {
        loadingResults.value = false
      }
    }
  }

  async function onStudentTestChange() {
    error.value = ''

    if (!testId.value) {
      await loadStudentContextResults()
      return
    }

    /*
     * Backend gives testId priority when subjectId + testId are sent.
     * Only ids derived from this student's current result context are allowed.
     */
    if (!selectedStudentTestIsValid()) {
      invalidateResults()
      error.value =
        'Выбранный тест не относится к текущему списку ваших результатов.'
      resetInvalidStudentTest()
      return
    }

    await loadResults()
  }

  function disposeResultsData() {
    resultsRequest.invalidate()
  }

  return {
    loadingResults,
    resultData,
    invalidateResults,
    loadStudentContextResults,
    loadResults,
    onStudentTestChange,
    disposeResultsData,
  }
}
