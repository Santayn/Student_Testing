import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  membershipsApi,
  subjectsApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  isAssignableTeacherMembership,
  isReactivatableTeacherMembership,
} from '@/utils/teacherMembershipEligibility'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const TEACHER_ROLE = 1
const TEACHER_APP_ROLE = 'TEACHER'
const ADMIN_APP_ROLE = 'ADMIN'

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
 * Owns read-side state for the admin teacher/subject workspace.
 * Assignment mutations deliberately remain in TeacherSubjectsView.
 */
export function useAdminTeacherSubjectsData() {
  const teachers = ref([])
  const subjects = ref([])
  const memberships = ref([])

  const teacherId = ref('')
  const searchQuery = ref('')
  const sortMode = ref('name-asc')

  const loadingBase = ref(false)
  const loadingMemberships = ref(false)

  const notice = ref({
    type: 'info',
    message: '',
  })

  const baseRequest = createLatestRequestGuard()
  const membershipsRequest = createLatestRequestGuard()

  const loading = computed(() => {
    return (
      loadingBase.value ||
      loadingMemberships.value
    )
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

  function personName(person) {
    return (
      [
        person?.lastName,
        person?.firstName,
        person?.middleName,
      ]
        .filter(Boolean)
        .join(' ')
        .trim() ||
      person?.fullName ||
      'Преподаватель'
    )
  }

  function mergePeopleById(...peopleLists) {
    const peopleById = new Map()

    peopleLists
      .flat()
      .forEach((person) => {
        if (person?.id) {
          peopleById.set(
            Number(person.id),
            person
          )
        }
      })

    return [
      ...peopleById.values(),
    ]
  }

  const teacherOptions = computed(() => {
    return teachers.value.map(
      (teacher) => {
        const email = String(
          teacher?.email ?? ''
        ).trim()

        return {
          value: String(teacher.id),
          label: email
            ? `${personName(teacher)} · ${email}`
            : personName(teacher),
        }
      }
    )
  })

  const selectedTeacher = computed(() => {
    return teachers.value.find(
      (item) =>
        Number(item.id) ===
        Number(teacherId.value)
    ) ?? null
  })

  const teacherMemberships = computed(() => {
    const selectedId =
      Number(teacherId.value)

    if (!selectedId) {
      return []
    }

    return memberships.value.filter(
      (item) =>
        Number(item.personId) === selectedId &&
        isAssignableTeacherMembership(item)
    )
  })

  const pausedTeacherMemberships = computed(() => {
    const selectedId =
      Number(teacherId.value)

    if (!selectedId) {
      return []
    }

    return memberships.value.filter(
      (item) =>
        Number(item.personId) === selectedId &&
        isReactivatableTeacherMembership(item)
    )
  })

  const activeMembershipBySubjectId = computed(() => {
    return new Map(
      teacherMemberships.value.map(
        (membership) => [
          Number(membership.subjectId),
          membership,
        ]
      )
    )
  })

  const pausedMembershipBySubjectId = computed(() => {
    return new Map(
      pausedTeacherMemberships.value.map(
        (membership) => [
          Number(membership.subjectId),
          membership,
        ]
      )
    )
  })

  const assignedSubjects = computed(() => {
    return teacherMemberships.value
      .map((membership) => {
        const subject = subjects.value.find(
          (item) =>
            Number(item.id) ===
            Number(membership.subjectId)
        )

        return subject
          ? {
              ...subject,
              membership,
            }
          : null
      })
      .filter(Boolean)
  })

  const availableSubjects = computed(() => {
    return subjects.value
      .filter(
        (subject) =>
          !activeMembershipBySubjectId.value.has(
            Number(subject.id)
          )
      )
      .map((subject) => ({
        ...subject,
        pausedMembership:
          pausedMembershipBySubjectId.value.get(
            Number(subject.id)
          ) ?? null,
      }))
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
      subject?.membership?.notes,
      subject?.pausedMembership?.notes,
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

  async function loadBaseData() {
    const requestId = baseRequest.begin()
    loadingBase.value = true

    try {
      const [
        teachersResponse,
        adminsResponse,
        subjectsResponse,
      ] = await Promise.all([
        usersApi.getPeople({
          role: TEACHER_APP_ROLE,
        }),
        usersApi.getPeople({
          role: ADMIN_APP_ROLE,
        }),
        subjectsApi.getAll(),
      ])

      if (!baseRequest.isCurrent(requestId)) {
        return false
      }

      teachers.value =
        mergePeopleById(
          listFromResponse(
            teachersResponse
          ),
          listFromResponse(
            adminsResponse
          )
        ).sort(
          (left, right) =>
            personName(left).localeCompare(
              personName(right),
              'ru'
            )
        )

      subjects.value =
        listFromResponse(
          subjectsResponse
        ).sort(
          (left, right) =>
            String(
              left?.name ?? ''
            ).localeCompare(
              String(
                right?.name ?? ''
              ),
              'ru'
            )
        )

      if (
        !teacherId.value &&
        teachers.value.length
      ) {
        teacherId.value = String(
          teachers.value[0].id
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
          'Не удалось загрузить преподавателей и предметы.'
        )
      )
      return false
    } finally {
      if (baseRequest.isCurrent(requestId)) {
        loadingBase.value = false
      }
    }
  }

  async function loadMemberships() {
    const requestId =
      membershipsRequest.begin()

    loadingMemberships.value = true

    try {
      const response =
        await membershipsApi
          .getSubjectMemberships({
            activeOnly: true,
          })

      if (
        !membershipsRequest.isCurrent(
          requestId
        )
      ) {
        return false
      }

      memberships.value =
        listFromResponse(response)
          .filter(
            (item) =>
              Number(item.role) ===
              TEACHER_ROLE
          )
      return true
    } catch (error) {
      if (
        !membershipsRequest.isCurrent(
          requestId
        )
      ) {
        return false
      }

      memberships.value = []

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить назначения преподавателей.'
        )
      )
      return false
    } finally {
      if (
        membershipsRequest.isCurrent(
          requestId
        )
      ) {
        loadingMemberships.value = false
      }
    }
  }

  return {
    sortOptions: SORT_OPTIONS,
    teachers,
    subjects,
    memberships,
    teacherId,
    searchQuery,
    sortMode,
    loadingBase,
    loadingMemberships,
    loading,
    notice,
    teacherOptions,
    selectedTeacher,
    teacherMemberships,
    pausedTeacherMemberships,
    assignedSubjects,
    availableSubjects,
    hasActiveFilters,
    filteredAssignedSubjects,
    filteredAvailableSubjects,
    filterResultText,
    showNotice,
    clearNotice,
    personName,
    resetFilters,
    loadBaseData,
    loadMemberships,
  }
}
