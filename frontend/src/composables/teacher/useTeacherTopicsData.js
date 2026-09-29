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
  createAbortableRequestGuard,
} from '@/utils/latestRequest'

export function useTeacherTopicsData({
  route,
  selectedMembership,
  selectedSubjectId,
  selectedSubject,
  onOpenRouteTopic,
}) {
  const topics = ref([])
  const loading = ref(false)

  const searchQuery = ref('')
  const descriptionFilter = ref('all')
  const sortMode = ref('ordinal')
  const handledRouteTopicKey = ref('')

  const topicsRequest = createAbortableRequestGuard()

  const notice = ref({
    type: 'info',
    message: '',
  })

  const descriptionOptions = [
    { value: 'all', label: 'Все темы' },
    { value: 'with-description', label: 'С описанием' },
    { value: 'without-description', label: 'Без описания' },
  ]

  const sortOptions = [
    { value: 'ordinal', label: 'По порядку' },
    { value: 'name-asc', label: 'Название А–Я' },
    { value: 'name-desc', label: 'Название Я–А' },
  ]

  const canEdit = computed(() => {
    return Boolean(selectedMembership.value)
  })

  const contextHint = computed(() => {
    if (!selectedMembership.value) {
      return 'Выберите предмет преподавателя. Темы группируют вопросы банка и используются в правилах формирования тестов.'
    }

    if (!selectedSubject.value) {
      return `Выбрано назначение #${selectedMembership.value.id}.`
    }

    if (!topics.value.length) {
      return `У предмета «${selectedSubject.value.name}» в выбранном назначении пока нет тем.`
    }

    return `Предмет «${selectedSubject.value.name}». Тем в выбранном назначении: ${topics.value.length}.`
  })

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim()) ||
      descriptionFilter.value !== 'all' ||
      sortMode.value !== 'ordinal'
  })

  const filteredTopics = computed(() => {
    const query = searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = topics.value.filter((topic) => {
      const description = String(topic.description ?? '').trim()

      if (
        descriptionFilter.value === 'with-description' &&
        !description
      ) {
        return false
      }

      if (
        descriptionFilter.value === 'without-description' &&
        description
      ) {
        return false
      }

      if (!query) {
        return true
      }

      const haystack = [
        topic.ordinal,
        topic.name,
        topic.description,
      ]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLocaleLowerCase('ru-RU')

      return haystack.includes(query)
    })

    return [...result].sort((left, right) => {
      if (sortMode.value === 'name-asc') {
        return String(left.name ?? '').localeCompare(
          String(right.name ?? ''),
          'ru'
        )
      }

      if (sortMode.value === 'name-desc') {
        return String(right.name ?? '').localeCompare(
          String(left.name ?? ''),
          'ru'
        )
      }

      return Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
    })
  })

  const filterResultText = computed(() => {
    if (!selectedMembership.value) {
      return 'Сначала выберите предмет преподавателя.'
    }

    return `Показано: ${filteredTopics.value.length} из ${topics.value.length}`
  })

  function routeQuery(topicId = null) {
    const query = {}

    if (selectedSubjectId.value) {
      query.subjectId = selectedSubjectId.value
    }

    if (selectedMembership.value) {
      query.subjectMembershipId = selectedMembership.value.id
    }

    if (topicId) {
      query.topicId = topicId
    }

    return query
  }

  function nextOrdinal() {
    return topics.value.reduce(
      (max, topic) =>
        Math.max(max, Number(topic.ordinal ?? 0)),
      0
    ) + 1
  }

  function resetFilters() {
    searchQuery.value = ''
    descriptionFilter.value = 'all'
    sortMode.value = 'ordinal'
  }

  async function resolveRouteTopic(membershipId, nextTopics, signal) {
    const topicId = route.query.topicId

    if (!topicId) {
      return null
    }

    let topic = nextTopics.find(
      (item) => String(item.id) === String(topicId)
    )

    if (topic) {
      return topic
    }

    try {
      const topicResponse = await topicsApi.getOne(topicId, { signal })
      const candidate = topicResponse.data

      if (
        candidate &&
        String(candidate.subjectMembershipId) === String(membershipId)
      ) {
        topic = candidate
      }
    } catch {
      /*
       * GET /topics/{id} возвращает 400, если темы нет.
       * Для workspace достаточно показать предупреждение.
       */
    }

    return topic
  }

  async function loadTopics({ openRouteTopic = false } = {}) {
    const { requestId, signal } = topicsRequest.begin()
    const membershipId = Number(
      selectedMembership.value?.id ?? 0
    )

    if (!membershipId) {
      topics.value = []
      loading.value = false
      return
    }

    loading.value = true

    try {
      const response = await topicsApi.getAll(
        { subjectMembershipId: membershipId },
        { signal }
      )

      const nextTopics = listFromResponse(response).sort(
        (left, right) =>
          Number(left.ordinal ?? 0) - Number(right.ordinal ?? 0)
      )

      const routeTopicKey = route.query.topicId
        ? `${membershipId}:${route.query.topicId}`
        : ''
      const shouldHandleRouteTopic = Boolean(
        openRouteTopic &&
        routeTopicKey &&
        handledRouteTopicKey.value !== routeTopicKey
      )
      let routeTopic = null

      if (shouldHandleRouteTopic) {
        routeTopic = await resolveRouteTopic(
          membershipId,
          nextTopics,
          signal
        )
      }

      if (!topicsRequest.isCurrent(requestId)) {
        return
      }

      topics.value = nextTopics

      if (shouldHandleRouteTopic) {
        handledRouteTopicKey.value = routeTopicKey

        if (routeTopic) {
          onOpenRouteTopic?.(routeTopic)
        } else {
          notice.value = {
            type: 'warning',
            message:
              'Тема из ссылки не относится к выбранному назначению преподавателя или больше не существует.',
          }
        }
      }
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
    } finally {
      if (topicsRequest.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  function dispose() {
    topicsRequest.invalidate()
  }

  return {
    descriptionOptions,
    sortOptions,
    topics,
    loading,
    notice,
    searchQuery,
    descriptionFilter,
    sortMode,
    canEdit,
    contextHint,
    hasActiveFilters,
    filteredTopics,
    filterResultText,
    routeQuery,
    nextOrdinal,
    resetFilters,
    loadTopics,
    dispose,
  }
}
