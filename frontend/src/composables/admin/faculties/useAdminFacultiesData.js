import {
  computed,
  ref,
} from 'vue'

import {
  facultiesApi,
  getApiErrorMessage,
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
  { value: 'code-asc', label: 'Код А–Я' },
]

/**
 * Owns the read-only faculties workspace state: loading, notice,
 * local filters/sorting and stale-refresh protection.
 * Faculty create/update/delete deliberately stay in FacultiesView.
 */
export function useAdminFacultiesData() {
  const faculties = ref([])
  const loading = ref(false)

  const notice = ref({
    type: 'info',
    message: '',
  })

  const searchQuery = ref('')
  const descriptionFilter = ref('all')
  const sortMode = ref('name-asc')

  const facultiesRequest = createLatestRequestGuard()

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim()) ||
      descriptionFilter.value !== 'all' ||
      sortMode.value !== 'name-asc'
  })

  const filteredFaculties = computed(() => {
    const query = searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = faculties.value.filter((faculty) => {
      const description = String(
        faculty.description ?? ''
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
        faculty.name,
        faculty.code,
        faculty.description,
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

      if (sortMode.value === 'code-asc') {
        return String(left.code ?? '').localeCompare(
          String(right.code ?? ''),
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
    return `Показано: ${filteredFaculties.value.length} из ${faculties.value.length}`
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

  async function loadFaculties() {
    const requestId = facultiesRequest.begin()
    loading.value = true

    try {
      const response = await facultiesApi.getAll()
      const nextFaculties = listFromResponse(response)

      if (!facultiesRequest.isCurrent(requestId)) {
        return false
      }

      faculties.value = nextFaculties
      return true
    } catch (error) {
      if (!facultiesRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить факультеты'
        )
      )
      return false
    } finally {
      if (facultiesRequest.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  return {
    descriptionOptions: DESCRIPTION_OPTIONS,
    sortOptions: SORT_OPTIONS,
    faculties,
    loading,
    notice,
    searchQuery,
    descriptionFilter,
    sortMode,
    hasActiveFilters,
    filteredFaculties,
    filterResultText,
    showNotice,
    clearNotice,
    resetFilters,
    loadFaculties,
  }
}
