import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const DESCRIPTION_OPTIONS = [
  { value: 'all', label: 'Все' },
  { value: 'with-description', label: 'С описанием' },
  { value: 'without-description', label: 'Без описания' },
]

const SORT_OPTIONS = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
]

/**
 * Owns the read-only subjects workspace state: loading, notice,
 * local filters/sorting and stale-refresh protection.
 * Subject validation/create/update/delete deliberately stay in SubjectsAdminView.
 */
export function useAdminSubjectsData() {
  const subjects = ref([])
  const loading = ref(false)

  const notice = ref({
    type: 'info',
    message: '',
  })

  const searchQuery = ref('')
  const descriptionFilter = ref('all')
  const sortMode = ref('name-asc')

  const subjectsRequest = createLatestRequestGuard()

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim()) ||
      descriptionFilter.value !== 'all' ||
      sortMode.value !== 'name-asc'
  })

  const filteredSubjects = computed(() => {
    const query = searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = subjects.value.filter((subject) => {
      const description = String(
        subject.description ?? ''
      ).trim()

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
        subject.name,
        subject.description,
      ]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLocaleLowerCase('ru-RU')

      return haystack.includes(query)
    })

    return [...result].sort((left, right) => {
      if (sortMode.value === 'name-desc') {
        return String(right.name ?? '').localeCompare(
          String(left.name ?? ''),
          'ru'
        )
      }

      return String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    })
  })

  const filterResultText = computed(() => {
    return `Показано: ${filteredSubjects.value.length} из ${subjects.value.length}`
  })

  function showNotice(type, message) {
    notice.value = {
      type,
      message,
    }
  }

  function clearNotice() {
    notice.value.message = ''
  }

  function resetFilters() {
    searchQuery.value = ''
    descriptionFilter.value = 'all'
    sortMode.value = 'name-asc'
  }

  async function loadSubjects() {
    const requestId = subjectsRequest.begin()
    loading.value = true

    try {
      const response = await subjectsApi.getAll()
      const nextSubjects = listFromResponse(response)

      if (!subjectsRequest.isCurrent(requestId)) {
        return false
      }

      subjects.value = nextSubjects
      return true
    } catch (error) {
      if (!subjectsRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить предметы'
        )
      )
      return false
    } finally {
      if (subjectsRequest.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  return {
    descriptionOptions: DESCRIPTION_OPTIONS,
    sortOptions: SORT_OPTIONS,
    subjects,
    loading,
    notice,
    searchQuery,
    descriptionFilter,
    sortMode,
    hasActiveFilters,
    filteredSubjects,
    filterResultText,
    showNotice,
    clearNotice,
    resetFilters,
    loadSubjects,
  }
}
