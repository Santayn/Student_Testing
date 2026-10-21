import {
  getCurrentScope,
  onScopeDispose,
  ref,
} from 'vue'

import { getApiErrorMessage } from '@/api/error'
import { aiApi } from '@/api/ai.api'

import { createAbortableRequestGuard } from '@/utils/latestRequest'

function isoDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/** Расход ИИ за период (по умолчанию — текущий месяц): всего, по типам, по преподавателям, по группам. */
export function useAiUsageReport() {
  const today = new Date()
  const from = ref(isoDate(new Date(today.getFullYear(), today.getMonth(), 1)))
  const to = ref(isoDate(today))
  const report = ref(null)
  const loading = ref(false)
  const error = ref('')
  const request = createAbortableRequestGuard()

  async function load() {
    const { requestId, signal } = request.begin()
    loading.value = true
    error.value = ''

    try {
      const response = await aiApi.getUsageReport({ from: from.value, to: to.value }, { signal })
      if (request.isCurrent(requestId)) {
        report.value = response?.data ?? null
      }
    } catch (requestError) {
      if (request.isCurrent(requestId)) {
        report.value = null
        error.value = getApiErrorMessage(requestError, 'Не удалось загрузить расход ИИ.')
      }
    } finally {
      if (request.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  if (getCurrentScope()) {
    onScopeDispose(() => request.invalidate())
  }

  return {
    from,
    to,
    report,
    loading,
    error,
    load,
  }
}
