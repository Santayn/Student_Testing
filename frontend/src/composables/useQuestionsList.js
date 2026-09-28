import { ref } from 'vue'

import {
  getApiErrorMessage,
  questionsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

/**
 * Owns the read-only question collection for the currently selected topic.
 * CRUD operations stay in QuestionsView; after a mutation they refresh this
 * list through the same loadQuestions() boundary.
 */
export function useQuestionsList({
  selectedTopicId,
  notice,
}) {
  const questions = ref([])
  const loading = ref(false)
  const questionsRequest = createLatestRequestGuard()

  function resetQuestions() {
    questionsRequest.invalidate()
    questions.value = []
    loading.value = false
  }

  async function loadQuestions() {
    const requestId = questionsRequest.begin()
    const topicId = Number(selectedTopicId.value || 0)

    questions.value = []

    if (!topicId) {
      loading.value = false
      return
    }

    loading.value = true

    try {
      const response = await questionsApi.getAll({
        topicId,
      })

      if (!questionsRequest.isCurrent(requestId)) {
        return
      }

      questions.value = listFromResponse(response).sort(
        (left, right) =>
          Number(left.ordinal ?? 0) -
          Number(right.ordinal ?? 0)
      )
    } catch (error) {
      if (!questionsRequest.isCurrent(requestId)) {
        return
      }

      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось загрузить вопросы'
        ),
      }
    } finally {
      if (questionsRequest.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  return {
    questions,
    loading,
    loadQuestions,
    resetQuestions,
  }
}
