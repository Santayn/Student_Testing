import {
  computed,
  onScopeDispose,
  ref,
} from 'vue'

import { aiApi } from '@/api/ai.api'
import { getApiErrorMessage } from '@/api/error'

const ACTIVE_STATUSES = new Set(['queued', 'running'])
const MAX_POLL_ERRORS = 5

/**
 * Запускает фоновую задачу ИИ и опрашивает её, пока она не завершится.
 * track(запрос) возвращает выполненную задачу или null (ошибка, отмена, новая задача).
 */
export function useAiJob({ intervalMs = 2000 } = {}) {
  const job = ref(null)
  const error = ref('')
  const starting = ref(false)
  let generation = 0
  let timer = null

  const running = computed(() =>
    Boolean(job.value && ACTIVE_STATUSES.has(job.value.status))
  )
  const busy = computed(() => starting.value || running.value)
  const progress = computed(() => job.value?.progress || '')

  function stop() {
    generation++
    starting.value = false
    clearTimeout(timer)
    timer = null
  }

  function reset() {
    stop()
    job.value = null
    error.value = ''
  }

  function wait(currentGeneration) {
    return new Promise((resolve) => {
      let failures = 0

      const check = () => {
        if (currentGeneration !== generation) {
          resolve(null)
          return
        }

        const status = job.value?.status

        if (status === 'done') {
          resolve(job.value)
          return
        }

        if (status === 'failed') {
          error.value = job.value?.error || 'Задача ИИ не выполнена.'
          resolve(null)
          return
        }

        timer = setTimeout(async () => {
          try {
            const response = await aiApi.getJob(job.value.id)
            if (currentGeneration === generation) {
              job.value = response.data
              failures = 0
            }
          } catch (requestError) {
            failures++
            if (failures >= MAX_POLL_ERRORS && currentGeneration === generation) {
              error.value = getApiErrorMessage(
                requestError,
                'Не удалось узнать состояние задачи ИИ.'
              )
              resolve(null)
              return
            }
          }
          check()
        }, intervalMs)
      }

      check()
    })
  }

  async function track(startRequest) {
    stop()
    error.value = ''
    job.value = null
    const currentGeneration = generation
    starting.value = true

    try {
      const response = await startRequest
      if (currentGeneration !== generation) {
        return null
      }
      job.value = response.data
    } catch (requestError) {
      if (currentGeneration === generation) {
        error.value = getApiErrorMessage(
          requestError,
          'Не удалось запустить задачу ИИ.'
        )
      }
      return null
    } finally {
      if (currentGeneration === generation) {
        starting.value = false
      }
    }

    return wait(currentGeneration)
  }

  onScopeDispose(stop)

  return {
    job,
    error,
    running,
    busy,
    progress,
    track,
    stop,
    reset,
  }
}
