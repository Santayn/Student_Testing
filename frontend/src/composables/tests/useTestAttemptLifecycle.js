import {
  computed,
  nextTick,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  learningApi,
} from '@/api'

import {
  clearCompletedTestSession,
  readCompletedTestSession,
  saveCompletedTestSession,
} from '@/utils/completedTestSession'

import {
  sanitizeStudentSubmitResult,
} from '@/utils/resultContracts'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

function assertStartAttemptContext(data, context) {
  const responseAssignmentId =
    Number(data?.assignmentId)

  const responseTestId =
    Number(data?.test?.id)

  const nestedAssignmentId =
    data?.test?.assignmentId == null
      ? null
      : Number(data.test.assignmentId)

  const responseAttemptId =
    Number(data?.attemptId)

  if (
    !Number.isFinite(responseAttemptId) ||
    responseAttemptId <= 0 ||
    responseAssignmentId !== context.assignmentId ||
    responseTestId !== context.testId ||
    (
      nestedAssignmentId !== null &&
      nestedAssignmentId !== responseAssignmentId
    )
  ) {
    throw new Error(
      'Backend вернул попытку, которая не соответствует открытому тесту.'
    )
  }
}

function hasUnknownRequestOutcome(errorValue) {
  if (!errorValue) {
    return false
  }

  if (
    errorValue.code === 'ECONNABORTED' ||
    errorValue.code === 'ERR_NETWORK'
  ) {
    return true
  }

  const looksLikeAxiosError =
    errorValue.isAxiosError === true ||
    Boolean(errorValue.config)

  return (
    looksLikeAxiosError &&
    !errorValue.response
  )
}

function defaultScrollToResult() {
  document
    .getElementById('test-result')
    ?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
}

export function useTestAttemptLifecycle({
  getRouteContext,
  isSameRouteContext,
  resetAnswers,
  initializeAttemptAnswers,
  buildSubmission,
  beforeSubmit = () => {},
  clearAttemptDraft = () => {},
  cancelDraftPersistence = () => {},
  api = learningApi,
  resolveApiError = getApiErrorMessage,
  scrollToResult = defaultScrollToResult,
} = {}) {
  const loading = ref(false)
  const submitting = ref(false)
  const submitOutcomeUnknown = ref(false)
  const error = ref('')

  const test = ref(null)
  const questions = ref([])
  const attemptId = ref(null)
  const resultData = ref(null)

  const loadRequest = createLatestRequestGuard()
  const submitRequest = createLatestRequestGuard()

  const submitted = computed(() => {
    return resultData.value !== null
  })

  function resetAttemptState() {
    submitOutcomeUnknown.value = false
    test.value = null
    questions.value = []
    attemptId.value = null
    resultData.value = null
    resetAnswers?.()
  }

  async function loadTest() {
    const context =
      getRouteContext?.()

    const requestId =
      loadRequest.begin()

    submitRequest.invalidate()
    submitting.value = false

    if (!context?.assignmentId || !context?.testId) {
      resetAttemptState()
      loading.value = false
      error.value =
        'Не указано назначение теста. Откройте тест со страницы лекции.'

      return
    }

    loading.value = true
    error.value = ''
    resetAttemptState()

    const completedSession =
      readCompletedTestSession(
        context.testId,
        context.assignmentId
      )

    if (completedSession) {
      if (
        !loadRequest.isCurrent(requestId) ||
        !isSameRouteContext?.(context)
      ) {
        return
      }

      clearAttemptDraft(context)

      attemptId.value =
        completedSession.attemptId

      test.value =
        completedSession.test ??
        null

      questions.value = []
      resultData.value =
        sanitizeStudentSubmitResult(
          completedSession.resultData
        )

      resetAnswers?.()
      loading.value = false
      return
    }

    try {
      const response =
        await api.startAttempt(
          context.assignmentId
        )

      if (
        !loadRequest.isCurrent(requestId) ||
        !isSameRouteContext?.(context)
      ) {
        return
      }

      const data =
        response.data ?? {}

      assertStartAttemptContext(
        data,
        context
      )

      attemptId.value =
        Number(data.attemptId)

      test.value =
        data.test ??
        null

      questions.value =
        Array.isArray(data.questions)
          ? data.questions
          : []

      initializeAttemptAnswers?.(context)
    } catch (requestError) {
      if (
        !loadRequest.isCurrent(requestId) ||
        !isSameRouteContext?.(context)
      ) {
        return
      }

      resetAttemptState()

      error.value =
        resolveApiError(
          requestError,
          'Не удалось загрузить тест.'
        )
    } finally {
      if (
        loadRequest.isCurrent(requestId) &&
        isSameRouteContext?.(context)
      ) {
        loading.value = false
      }
    }
  }

  async function submitTest() {
    if (
      submitting.value ||
      submitOutcomeUnknown.value ||
      submitted.value ||
      !questions.value.length ||
      !attemptId.value
    ) {
      return
    }

    beforeSubmit()

    const context = {
      ...getRouteContext?.(),
      attemptId: Number(attemptId.value),
    }

    if (
      !context.testId ||
      !context.assignmentId ||
      !Number.isFinite(context.attemptId) ||
      context.attemptId <= 0
    ) {
      error.value =
        'Контекст попытки устарел. Откройте тест заново со страницы лекции.'
      return
    }

    const requestId =
      submitRequest.begin()

    submitting.value = true
    error.value = ''

    try {
      const payload =
        buildSubmission?.()

      const response =
        await api.submitAttempt(
          context.attemptId,
          payload
        )

      if (
        !submitRequest.isCurrent(requestId) ||
        !isSameRouteContext?.(context) ||
        Number(attemptId.value) !== context.attemptId
      ) {
        return
      }

      resultData.value =
        sanitizeStudentSubmitResult(
          response.data
        )

      clearAttemptDraft(context)
      cancelDraftPersistence()

      saveCompletedTestSession({
        testId: context.testId,
        assignmentId: context.assignmentId,
        attemptId: context.attemptId,
        test: test.value,
        resultData: resultData.value,
      })

      await nextTick()

      if (
        !submitRequest.isCurrent(requestId) ||
        !isSameRouteContext?.(context) ||
        Number(attemptId.value) !== context.attemptId
      ) {
        return
      }

      scrollToResult?.()
    } catch (requestError) {
      if (
        !submitRequest.isCurrent(requestId) ||
        !isSameRouteContext?.(context) ||
        Number(attemptId.value) !== context.attemptId
      ) {
        return
      }

      if (hasUnknownRequestOutcome(requestError)) {
        submitOutcomeUnknown.value = true
        error.value =
          'Связь с сервером прервалась во время отправки. ' +
          'Результат попытки неизвестен, поэтому повторная отправка заблокирована. ' +
          'Откройте результаты или вернитесь к тесту позже, чтобы не отправить попытку повторно.'
        return
      }

      error.value =
        resolveApiError(
          requestError,
          'Не удалось отправить ответы на тест.'
        )
    } finally {
      if (
        submitRequest.isCurrent(requestId) &&
        isSameRouteContext?.(context) &&
        Number(attemptId.value) === context.attemptId
      ) {
        submitting.value = false
      }
    }
  }

  function leaveCurrentAttempt() {
    loadRequest.invalidate()
    submitRequest.invalidate()

    const context =
      getRouteContext?.()

    if (context?.testId && context?.assignmentId) {
      clearCompletedTestSession(
        context.testId,
        context.assignmentId
      )
    }
  }

  function disposeAttemptLifecycle() {
    loadRequest.invalidate()
    submitRequest.invalidate()
  }

  return {
    loading,
    submitting,
    submitOutcomeUnknown,
    error,
    test,
    questions,
    attemptId,
    resultData,
    submitted,
    loadTest,
    submitTest,
    leaveCurrentAttempt,
    disposeAttemptLifecycle,
  }
}
