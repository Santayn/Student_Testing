import {
  getCurrentScope,
  onScopeDispose,
  reactive,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  resultsApi,
} from '@/api'

import { createAbortableRequestGuard } from '@/utils/latestRequest'

export const REVIEW_STATUS_OPTIONS = Object.freeze([
  { value: 'needs_teacher', label: 'Нужна проверка' },
  { value: 'ai_pending', label: 'Проверяет ИИ' },
  { value: 'ai_graded', label: 'Оценено ИИ' },
  { value: 'teacher_confirmed', label: 'Проверено преподавателем' },
  { value: 'all', label: 'Все' },
])

export const REVIEW_STATUS_LABELS = Object.freeze({
  needs_teacher: 'Нужна проверка',
  ai_pending: 'Проверяет ИИ',
  ai_graded: 'Оценено ИИ',
  teacher_confirmed: 'Проверено',
  none: 'Без проверки',
})

/**
 * Очередь «На проверку»: ответы со свободным текстом, которые ИИ оценил неуверенно
 * или не смог оценить, плюс оценки ИИ для выборочного просмотра. Преподаватель
 * подтверждает оценку, ставит свои баллы или отправляет ответ на повторную проверку.
 */
export function useReviewQueue() {
  const status = ref('needs_teacher')
  const items = ref([])
  const counts = reactive({
    needs_teacher: 0,
    ai_pending: 0,
    ai_graded: 0,
    teacher_confirmed: 0,
  })
  const loading = ref(false)
  const error = ref('')
  const busyResponseId = ref(null)
  const request = createAbortableRequestGuard()

  async function load() {
    const { requestId, signal } = request.begin()
    loading.value = true
    error.value = ''

    try {
      const response = await resultsApi.getReviewQueue({ status: status.value }, { signal })

      if (!request.isCurrent(requestId)) {
        return
      }

      items.value = Array.isArray(response?.data?.items) ? response.data.items : []
      Object.assign(counts, response?.data?.counts ?? {})
    } catch (requestError) {
      if (request.isCurrent(requestId)) {
        items.value = []
        error.value = getApiErrorMessage(requestError, 'Не удалось загрузить ответы на проверку.')
      }
    } finally {
      if (request.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  async function runAction(item, action, successMessage) {
    if (busyResponseId.value) {
      return { ok: false }
    }

    busyResponseId.value = item.responseId

    try {
      const response = await action()
      replaceItem(response?.data)
      await load()
      return { ok: true, message: successMessage }
    } catch (requestError) {
      return {
        ok: false,
        message: getApiErrorMessage(requestError, 'Не удалось сохранить проверку.'),
      }
    } finally {
      busyResponseId.value = null
    }
  }

  function replaceItem(updated) {
    if (!updated?.responseId) {
      return
    }
    items.value = items.value.map((item) => (
      item.responseId === updated.responseId ? updated : item
    ))
  }

  function grade(item, awardedPoints, comment) {
    return runAction(
      item,
      () => resultsApi.gradeResponse(item.responseId, {
        awardedPoints: Number(awardedPoints),
        comment: String(comment ?? '').trim() || null,
      }),
      'Оценка сохранена.'
    )
  }

  function accept(item) {
    return runAction(
      item,
      () => resultsApi.acceptResponse(item.responseId),
      'Оценка подтверждена.'
    )
  }

  function regrade(item) {
    return runAction(
      item,
      () => resultsApi.regradeResponse(item.responseId),
      'Ответ отправлен ИИ на повторную проверку.'
    )
  }

  if (getCurrentScope()) {
    onScopeDispose(() => request.invalidate())
  }

  return {
    status,
    items,
    counts,
    loading,
    error,
    busyResponseId,
    load,
    grade,
    accept,
    regrade,
  }
}
