import {
  computed,
  ref,
} from 'vue'

import {
  facultiesApi,
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const SORT_OPTIONS = [
  {
    value: 'name-asc',
    label: 'Название А–Я',
  },
  {
    value: 'name-desc',
    label: 'Название Я–А',
  },
]

/**
 * Owns read-side state for the admin faculty/subject workspace.
 * Faculty-subject mutations deliberately remain in FacultySubjectsView.
 */
export function useAdminFacultySubjectsData() {
  const faculties = ref([])
  const subjects = ref([])
  const assignedSubjects = ref([])

  const facultyId = ref('')
  const searchQuery = ref('')
  const sortMode = ref('name-asc')

  const loadingBase = ref(false)
  const loadingAssigned = ref(false)

  const notice = ref({
    type: 'info',
    message: '',
  })

  const baseRequest = createLatestRequestGuard()
  const assignedSubjectsRequest =
    createLatestRequestGuard()

  const loading = computed(() => {
    return (
      loadingBase.value ||
      loadingAssigned.value
    )
  })

  const facultyOptions = computed(() => {
    return faculties.value
      .map((faculty) => ({
        value: String(faculty.id),
        label: faculty.code
          ? `${faculty.name} (${faculty.code})`
          : faculty.name,
      }))
      .sort((left, right) =>
        left.label.localeCompare(right.label, 'ru')
      )
  })

  const selectedFaculty = computed(() => {
    return faculties.value.find(
      (item) =>
        Number(item.id) ===
        Number(facultyId.value)
    ) ?? null
  })

  const assignedIds = computed(() => {
    return new Set(
      assignedSubjects.value.map(
        (item) => Number(item.id)
      )
    )
  })

  const availableSubjects = computed(() => {
    return subjects.value.filter(
      (subject) =>
        !assignedIds.value.has(
          Number(subject.id)
        )
    )
  })

  const hasActiveFilters = computed(() => {
    return Boolean(searchQuery.value.trim()) ||
      sortMode.value !== 'name-asc'
  })

  const filteredAssignedSubjects = computed(() => {
    return filterAndSortSubjects(
      assignedSubjects.value
    )
  })

  const filteredAvailableSubjects = computed(() => {
    return filterAndSortSubjects(
      availableSubjects.value
    )
  })

  const filterResultText = computed(() => {
    return (
      `Назначено: ${filteredAssignedSubjects.value.length} из ${assignedSubjects.value.length}. ` +
      `Доступно: ${filteredAvailableSubjects.value.length} из ${availableSubjects.value.length}.`
    )
  })

  function normalizedSearch() {
    return searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')
  }

  function subjectMatchesSearch(subject, query) {
    if (!query) {
      return true
    }

    return [
      subject?.name,
      subject?.description,
    ]
      .filter(
        (value) =>
          value !== null &&
          value !== undefined
      )
      .join(' ')
      .toLocaleLowerCase('ru-RU')
      .includes(query)
  }

  function filterAndSortSubjects(source) {
    const query = normalizedSearch()

    const result = source.filter(
      (subject) =>
        subjectMatchesSearch(
          subject,
          query
        )
    )

    return [...result].sort(
      (left, right) => {
        const comparison = String(
          left?.name ?? ''
        ).localeCompare(
          String(right?.name ?? ''),
          'ru'
        )

        return sortMode.value ===
          'name-desc'
          ? -comparison
          : comparison
      }
    )
  }

  function resetFilters() {
    searchQuery.value = ''
    sortMode.value = 'name-asc'
  }

  function showNotice(type, message) {
    notice.value = {
      type,
      message,
    }
  }

  function clearNotice() {
    notice.value.message = ''
  }

  async function loadBaseData() {
    const requestId = baseRequest.begin()
    loadingBase.value = true

    try {
      const [
        facultiesResponse,
        subjectsResponse,
      ] = await Promise.all([
        facultiesApi.getAll(),
        subjectsApi.getAll(),
      ])

      if (!baseRequest.isCurrent(requestId)) {
        return false
      }

      faculties.value = listFromResponse(
        facultiesResponse
      ).sort((left, right) =>
        String(left?.name ?? '').localeCompare(
          String(right?.name ?? ''),
          'ru'
        )
      )

      subjects.value = listFromResponse(
        subjectsResponse
      ).sort((left, right) =>
        String(left?.name ?? '').localeCompare(
          String(right?.name ?? ''),
          'ru'
        )
      )

      if (
        !facultyId.value &&
        faculties.value.length
      ) {
        facultyId.value = String(
          faculties.value[0].id
        )
      }

      return true
    } catch (error) {
      if (!baseRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить факультеты и предметы.'
        )
      )

      return false
    } finally {
      if (baseRequest.isCurrent(requestId)) {
        loadingBase.value = false
      }
    }
  }

  async function loadAssignedSubjects() {
    const requestId =
      assignedSubjectsRequest.begin()

    const requestedFacultyId =
      Number(facultyId.value)

    if (
      !Number.isInteger(requestedFacultyId) ||
      requestedFacultyId <= 0
    ) {
      assignedSubjects.value = []
      loadingAssigned.value = false
      return false
    }

    loadingAssigned.value = true

    try {
      const response =
        await facultiesApi.getSubjects(
          requestedFacultyId
        )

      if (
        !assignedSubjectsRequest.isCurrent(
          requestId
        )
      ) {
        return false
      }

      assignedSubjects.value =
        listFromResponse(response)

      return true
    } catch (error) {
      if (
        !assignedSubjectsRequest.isCurrent(
          requestId
        )
      ) {
        return false
      }

      assignedSubjects.value = []

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить предметы факультета.'
        )
      )

      return false
    } finally {
      if (
        assignedSubjectsRequest.isCurrent(
          requestId
        )
      ) {
        loadingAssigned.value = false
      }
    }
  }

  return {
    sortOptions: SORT_OPTIONS,
    faculties,
    subjects,
    assignedSubjects,
    facultyId,
    searchQuery,
    sortMode,
    loadingBase,
    loadingAssigned,
    loading,
    notice,
    facultyOptions,
    selectedFaculty,
    availableSubjects,
    hasActiveFilters,
    filteredAssignedSubjects,
    filteredAvailableSubjects,
    filterResultText,
    resetFilters,
    showNotice,
    clearNotice,
    loadBaseData,
    loadAssignedSubjects,
  }
}
