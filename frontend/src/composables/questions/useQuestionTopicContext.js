import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  topicsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

/**
 * Owns topic context for the teacher question bank.
 * Question CRUD stays in QuestionsView; this composable only resolves the
 * selected topic and protects topic loading from stale responses.
 */
export function useQuestionTopicContext({
  route,
  selectedMembership,
  notice,
}) {
  const topics = ref([])
  const selectedTopicId = ref('')
  const selectedImportTopicId = ref('')
  const topicsRequest = createLatestRequestGuard()

  const topicOptions = computed(() => {
    return topics.value.map(
      (topic) => ({
        value: topic.id,
        label: `${topic.ordinal}. ${topic.name}`,
      })
    )
  })

  const currentTopic = computed(() => {
    return topics.value.find(
      (topic) =>
        String(topic.id) ===
        String(selectedTopicId.value)
    ) ?? null
  })

  function resetTopics() {
    topicsRequest.invalidate()
    topics.value = []
    selectedTopicId.value = ''
    selectedImportTopicId.value = ''
  }

  async function loadTopics() {
    const requestId = topicsRequest.begin()
    const membershipId = Number(
      selectedMembership.value?.id ?? 0
    )

    topics.value = []
    selectedTopicId.value = ''
    selectedImportTopicId.value = ''

    if (!membershipId) {
      return
    }

    try {
      const response = await topicsApi.getAll({
        subjectMembershipId: membershipId,
      })

      if (!topicsRequest.isCurrent(requestId)) {
        return
      }

      topics.value = listFromResponse(response).sort(
        (left, right) =>
          Number(left.ordinal ?? 0) -
          Number(right.ordinal ?? 0)
      )

      const preferredTopicId = route.query.topicId

      if (
        preferredTopicId &&
        topics.value.some(
          (item) =>
            String(item.id) ===
            String(preferredTopicId)
        )
      ) {
        selectedTopicId.value = String(preferredTopicId)
      } else if (topics.value.length === 1) {
        selectedTopicId.value = String(topics.value[0].id)
      }

      selectedImportTopicId.value = selectedTopicId.value
    } catch (error) {
      if (!topicsRequest.isCurrent(requestId)) {
        return
      }

      notice.value = {
        type: 'danger',
        message: getApiErrorMessage(
          error,
          'Не удалось загрузить темы'
        ),
      }
    }
  }

  return {
    topics,
    selectedTopicId,
    selectedImportTopicId,
    topicOptions,
    currentTopic,
    loadTopics,
    resetTopics,
  }
}
