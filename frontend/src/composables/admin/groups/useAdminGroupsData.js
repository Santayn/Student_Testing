import {
  computed,
  ref,
} from 'vue'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const SORT_OPTIONS = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'code-asc', label: 'Код А–Я' },
  { value: 'faculty-asc', label: 'Факультет А–Я' },
]

/**
 * Owns the read-only admin groups workspace state: faculties, groups,
 * presentation helpers, filters and stale-request protection.
 * Group and membership mutations deliberately stay in GroupsView so their
 * existing CRUD and member-management flows remain explicit.
 */
export function useAdminGroupsData() {
  const faculties = ref([])
  const groups = ref([])
  const loading = ref(false)

  const notice = ref({
    type: 'info',
    message: '',
  })

  const searchQuery = ref('')
  const facultyFilter = ref('all')
  const sortMode = ref('name-asc')

  const dataRequest = createLatestRequestGuard()

  function showNotice(type, message) {
    notice.value = {
      type,
      message,
    }
  }

  function clearNotice() {
    notice.value.message = ''
  }

  function facultyName(facultyId) {
    const faculty = faculties.value.find(
      (item) => Number(item.id) === Number(facultyId)
    )

    return faculty?.name ?? 'Факультет не найден'
  }

  const facultyOptions = computed(() => {
    return faculties.value
      .map((faculty) => ({
        value: String(faculty.id),
        label: faculty.name,
      }))
      .sort((left, right) =>
        left.label.localeCompare(right.label, 'ru')
      )
  })

  const facultyFilterOptions = computed(() => [
    { value: 'all', label: 'Все факультеты' },
    ...facultyOptions.value,
  ])

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim()) ||
      facultyFilter.value !== 'all' ||
      sortMode.value !== 'name-asc'
  })

  const filteredGroups = computed(() => {
    const query = searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = groups.value.filter((group) => {
      if (
        facultyFilter.value !== 'all' &&
        String(group.facultyId) !== facultyFilter.value
      ) {
        return false
      }

      if (!query) {
        return true
      }

      const haystack = [
        group.name,
        group.code,
        facultyName(group.facultyId),
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

      if (sortMode.value === 'faculty-asc') {
        return facultyName(left.facultyId).localeCompare(
          facultyName(right.facultyId),
          'ru'
        ) || String(left.name ?? '').localeCompare(
          String(right.name ?? ''),
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
    return `Показано: ${filteredGroups.value.length} из ${groups.value.length}`
  })

  function resetFilters() {
    searchQuery.value = ''
    facultyFilter.value = 'all'
    sortMode.value = 'name-asc'
  }

  async function loadData({ clearMessage = false } = {}) {
    const requestId = dataRequest.begin()
    loading.value = true

    if (clearMessage) {
      clearNotice()
    }

    try {
      const [
        facultiesResponse,
        groupsResponse,
      ] = await Promise.all([
        facultiesApi.getAll(),
        groupsApi.getAll(),
      ])

      const nextFaculties = listFromResponse(facultiesResponse)
      const nextGroups = listFromResponse(groupsResponse)

      if (!dataRequest.isCurrent(requestId)) {
        return false
      }

      faculties.value = nextFaculties
      groups.value = nextGroups
      return true
    } catch (error) {
      if (!dataRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить группы'
        )
      )
      return false
    } finally {
      if (dataRequest.isCurrent(requestId)) {
        loading.value = false
      }
    }
  }

  return {
    sortOptions: SORT_OPTIONS,
    faculties,
    groups,
    loading,
    notice,
    searchQuery,
    facultyFilter,
    sortMode,
    facultyOptions,
    facultyFilterOptions,
    hasActiveFilters,
    filteredGroups,
    filterResultText,
    showNotice,
    clearNotice,
    facultyName,
    resetFilters,
    loadData,
  }
}
