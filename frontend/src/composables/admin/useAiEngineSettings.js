import {
  getCurrentScope,
  onScopeDispose,
  ref,
} from 'vue'

import { getApiErrorMessage } from '@/api/error'
import { aiApi } from '@/api/ai.api'

import { createAbortableRequestGuard } from '@/utils/latestRequest'

/** Движки функций ИИ: внешний API или собственная модель. */
export function useAiEngineSettings() {
  const features = ref([])
  const loading = ref(false)
  const savingFeature = ref('')
  const error = ref('')
  const request = createAbortableRequestGuard()

  async function load() {
    const { requestId, signal } = request.begin()
    loading.value = true
    error.value = ''

    try {
      const response = await aiApi.getEngineSettings({ signal })
      if (request.isCurrent(requestId)) {
        features.value = Array.isArray(response?.data) ? response.data : []
      }
    } catch (requestError) {
      if (request.isCurrent(requestId)) {
        error.value = getApiErrorMessage(requestError, 'Не удалось загрузить настройки ИИ.')
      }
    } finally {
      if (request.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  async function setEngine(feature, engine) {
    if (savingFeature.value) {
      return false
    }
    savingFeature.value = feature
    error.value = ''

    try {
      const response = await aiApi.setEngine(feature, engine)
      features.value = Array.isArray(response?.data) ? response.data : features.value
      return true
    } catch (requestError) {
      error.value = getApiErrorMessage(requestError, 'Не удалось сменить движок.')
      return false
    } finally {
      savingFeature.value = ''
    }
  }

  if (getCurrentScope()) {
    onScopeDispose(() => request.invalidate())
  }

  return {
    features,
    loading,
    savingFeature,
    error,
    load,
    setEngine,
  }
}
