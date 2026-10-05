import {
  onBeforeUnmount,
  onMounted,
  watch,
} from 'vue'

import {
  clearTestAttemptDraft,
  readTestAttemptDraft,
  saveTestAttemptDraft,
} from '@/utils/testAttemptDraft'

export const ATTEMPT_DRAFT_PERSIST_DELAY_MS = 250

export function useAttemptDraft({
  getRouteContext,
  getAttemptId,
  getQuestions,
  isSubmitted,
  singleAnswers,
  multipleAnswers,
  textAnswers,
  matchingAnswers,
  initializeAnswers,
  persistDelayMs = ATTEMPT_DRAFT_PERSIST_DELAY_MS,
  readDraft = readTestAttemptDraft,
  saveDraft = saveTestAttemptDraft,
  clearDraft = clearTestAttemptDraft,
} = {}) {
  let persistencePaused = false
  let persistenceTimer = null
  let pendingDraftContext = null

  function questions() {
    const value = getQuestions?.()

    return Array.isArray(value)
      ? value
      : []
  }

  function currentDraftContext() {
    const context =
      getRouteContext?.()

    const attemptId =
      Number(getAttemptId?.())

    if (
      !context?.testId ||
      !context?.assignmentId ||
      !Number.isFinite(attemptId) ||
      attemptId <= 0
    ) {
      return null
    }

    return {
      testId: context.testId,
      assignmentId:
        context.assignmentId,
      attemptId,
    }
  }

  function sameDraftContext(
    left,
    right
  ) {
    return (
      left?.testId === right?.testId &&
      left?.assignmentId ===
        right?.assignmentId &&
      left?.attemptId ===
        right?.attemptId
    )
  }

  function applyDraftAnswers(draft) {
    if (!draft) {
      return
    }

    Object.entries(
      draft.singleAnswers ?? {}
    ).forEach(([id, value]) => {
      singleAnswers[id] = value
    })

    Object.entries(
      draft.multipleAnswers ?? {}
    ).forEach(([id, value]) => {
      multipleAnswers[id] =
        Array.isArray(value)
          ? [...value]
          : []
    })

    Object.entries(
      draft.textAnswers ?? {}
    ).forEach(([id, value]) => {
      textAnswers[id] =
        String(value ?? '')
    })

    Object.entries(
      draft.matchingAnswers ?? {}
    ).forEach(([id, value]) => {
      matchingAnswers[id] =
        Array.isArray(value)
          ? [...value]
          : []
    })
  }

  function restoreCurrentAttemptDraft(
    context
  ) {
    const draft = readDraft({
      testId: context.testId,
      assignmentId:
        context.assignmentId,
      attemptId:
        Number(getAttemptId?.()),
      questions: questions(),
    })

    applyDraftAnswers(draft)
  }

  function initializeAttemptAnswers(
    context
  ) {
    persistencePaused = true

    try {
      initializeAnswers?.()
      restoreCurrentAttemptDraft(
        context
      )
    } finally {
      persistencePaused = false
    }
  }

  function clearDraftForContext(
    context
  ) {
    clearDraft(
      context?.testId,
      context?.assignmentId
    )
  }

  function clearPersistenceTimer() {
    if (persistenceTimer === null) {
      return
    }

    window.clearTimeout(
      persistenceTimer
    )

    persistenceTimer = null
  }

  function persistCurrentAttemptDraft(
    context = currentDraftContext()
  ) {
    const currentQuestions =
      questions()

    if (
      persistencePaused ||
      isSubmitted?.() ||
      !context ||
      !currentQuestions.length
    ) {
      return
    }

    saveDraft({
      testId: context.testId,
      assignmentId:
        context.assignmentId,
      attemptId:
        context.attemptId,
      questions:
        currentQuestions,
      singleAnswers,
      multipleAnswers,
      textAnswers,
      matchingAnswers,
    })
  }

  function cancelScheduledDraftPersistence() {
    clearPersistenceTimer()
    pendingDraftContext = null
  }

  function flushScheduledDraftPersistence() {
    if (!pendingDraftContext) {
      return
    }

    const context =
      pendingDraftContext

    clearPersistenceTimer()
    pendingDraftContext = null

    persistCurrentAttemptDraft(
      context
    )
  }

  function scheduleCurrentAttemptDraftPersistence() {
    if (
      persistencePaused ||
      isSubmitted?.() ||
      !questions().length
    ) {
      return
    }

    const context =
      currentDraftContext()

    if (!context) {
      return
    }

    if (
      pendingDraftContext &&
      !sameDraftContext(
        pendingDraftContext,
        context
      )
    ) {
      flushScheduledDraftPersistence()
    }

    pendingDraftContext = context
    clearPersistenceTimer()

    persistenceTimer =
      window.setTimeout(() => {
        flushScheduledDraftPersistence()
      }, persistDelayMs)
  }

  function handlePageHide() {
    flushScheduledDraftPersistence()
  }

  watch(
    [
      singleAnswers,
      multipleAnswers,
      textAnswers,
      matchingAnswers,
    ],
    scheduleCurrentAttemptDraftPersistence,
    { deep: true, flush: 'sync' }
  )

  onMounted(() => {
    window.addEventListener(
      'pagehide',
      handlePageHide
    )
  })

  onBeforeUnmount(() => {
    flushScheduledDraftPersistence()

    window.removeEventListener(
      'pagehide',
      handlePageHide
    )

    cancelScheduledDraftPersistence()
  })

  return {
    initializeAttemptAnswers,
    clearDraftForContext,
    flushScheduledDraftPersistence,
    cancelScheduledDraftPersistence,
  }
}
