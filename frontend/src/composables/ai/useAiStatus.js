import {
  readonly,
  ref,
} from 'vue'

import { aiApi } from '@/api/ai.api'

/*
 * Что из ИИ включено на сервере. Настройка общая для всех, поэтому статус
 * загружается один раз за сессию страницы; кнопки ИИ показываются только
 * для доступных функций.
 */
const status = ref(null)
let pending = null

async function loadStatus() {
  try {
    const response = await aiApi.getStatus()
    status.value = response?.data ?? null
  } catch {
    status.value = null
    pending = null
  }
  return status.value
}

export function useAiStatus() {
  if (!pending) {
    pending = loadStatus()
  }

  function reloadAiStatus() {
    pending = loadStatus()
    return pending
  }

  return {
    aiStatus: readonly(status),
    aiStatusReady: pending,
    reloadAiStatus,
  }
}
