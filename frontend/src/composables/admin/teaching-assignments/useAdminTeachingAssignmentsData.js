import {
  computed,
  reactive,
  ref,
} from 'vue'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
  membershipsApi,
  teachingApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const TEACHER_ROLE = 1

export const ADMIN_TEACHING_STATUS_LABELS = {
  1: 'Активно',
  2: 'Черновик',
  3: 'Закрыто',
  4: 'В паузе',
}

export const ADMIN_TEACHING_STATUS_OPTIONS = Object.entries(
  ADMIN_TEACHING_STATUS_LABELS
).map(([value, label]) => ({
  value: String(value),
  label,
}))

export const ADMIN_TEACHING_COURSE_OPTIONS = Array.from(
  { length: 6 },
  (_, index) => ({
    value: String(index + 1),
    label: String(index + 1),
  })
)

export const ADMIN_TEACHING_SEMESTER_OPTIONS = [
  { value: '1', label: '1' },
  { value: '2', label: '2' },
]

export const ADMIN_TEACHING_SORT_OPTIONS = [
  { value: 'subject-asc', label: 'Предмет А–Я' },
  { value: 'teacher-asc', label: 'Преподаватель А–Я' },
  { value: 'group-asc', label: 'Группа А–Я' },
  { value: 'hours-desc', label: 'Часы: больше → меньше' },
]

/**
 * Owns the read-side of the admin teaching workload workspace:
 * reference dictionaries, faculty/period context, assignment list,
 * filters/presentation helpers and stale-request protection.
 * Assignment/load-type mutations deliberately stay in TeachingAssignmentsView.
 */
export function useAdminTeachingAssignmentsData() {
  const faculties = ref([])
  const facultySubjects = ref([])
  const groups = ref([])
  const people = ref([])
  const teacherMemberships = ref([])
  const assignments = ref([])
  const loadTypes = ref([])

  const loadingBase = ref(false)
  const loadingContext = ref(false)
  const loadingAssignments = ref(false)
  const initialized = ref(false)

  const baseRequest = createLatestRequestGuard()
  const contextRequest = createLatestRequestGuard()
  const assignmentsRequest = createLatestRequestGuard()

  const context = reactive({
    facultyId: '',
    studyCourse: '1',
    semester: '1',
    academicYear: String(new Date().getFullYear()),
  })

  const searchQuery = ref('')
  const subjectFilter = ref('all')
  const teacherFilter = ref('all')
  const groupFilter = ref('all')
  const loadTypeFilter = ref('all')
  const statusFilter = ref('all')
  const sortMode = ref('subject-asc')

  const notice = ref({
    type: 'info',
    message: '',
  })

  const loading = computed(() => (
    loadingBase.value ||
    loadingContext.value ||
    loadingAssignments.value
  ))

  const selectedFaculty = computed(() => {
    return faculties.value.find(
      (item) =>
        Number(item.id) === Number(context.facultyId)
    ) ?? null
  })

  const facultyOptions = computed(() => {
    return faculties.value.map((faculty) => ({
      value: String(faculty.id),
      label: faculty.name,
    }))
  })

  const subjectOptions = computed(() => {
    return facultySubjects.value.map((subject) => ({
      value: String(subject.id),
      label: subject.name,
    }))
  })

  const groupOptions = computed(() => {
    return groups.value.map((group) => ({
      value: String(group.id),
      label: group.code
        ? `${group.name} · ${group.code}`
        : group.name,
    }))
  })

  const loadTypeOptions = computed(() => {
    return loadTypes.value.map((loadType) => ({
      value: String(loadType.id),
      label: loadType.name,
    }))
  })

  const assignmentSubjectFilterOptions = computed(() => [
    { value: 'all', label: 'Все предметы' },
    ...subjectOptions.value,
  ])

  const assignmentGroupFilterOptions = computed(() => [
    { value: 'all', label: 'Все группы' },
    ...groupOptions.value,
  ])

  const assignmentLoadTypeFilterOptions = computed(() => [
    { value: 'all', label: 'Все типы' },
    ...loadTypeOptions.value,
  ])

  const assignmentStatusFilterOptions = [
    { value: 'all', label: 'Все статусы' },
    ...ADMIN_TEACHING_STATUS_OPTIONS,
  ]

  function personById(personId) {
    return people.value.find(
      (item) => Number(item.id) === Number(personId)
    ) ?? null
  }

  function personLabel(personId) {
    const person = personById(personId)

    if (!person) {
      return 'Преподаватель'
    }

    return (
      [
        person.lastName,
        person.firstName,
        person.middleName,
      ]
        .filter(Boolean)
        .join(' ')
        .trim() ||
      person.fullName ||
      'Преподаватель'
    )
  }

  function teacherMembershipLabel(membership) {
    const person = personById(membership?.personId)
    const name = personLabel(membership?.personId)
    const email = String(person?.email ?? '').trim()

    return email ? `${name} · ${email}` : name
  }

  function subjectName(subjectId) {
    return (
      facultySubjects.value.find(
        (item) => Number(item.id) === Number(subjectId)
      )?.name ?? 'Предмет'
    )
  }

  function groupName(groupId) {
    const group = groups.value.find(
      (item) => Number(item.id) === Number(groupId)
    )

    if (!group) {
      return 'Группа'
    }

    return group.code
      ? `${group.name} · ${group.code}`
      : group.name
  }

  function membershipById(id) {
    return teacherMemberships.value.find(
      (item) => Number(item.id) === Number(id)
    ) ?? null
  }

  function loadTypeName(loadTypeId) {
    return (
      loadTypes.value.find(
        (item) => Number(item.id) === Number(loadTypeId)
      )?.name ?? 'Тип нагрузки'
    )
  }

  function assignmentSubjectName(assignment) {
    const membership = membershipById(
      assignment?.subjectMembershipId
    )

    return subjectName(membership?.subjectId)
  }

  function teacherNameForAssignment(assignment) {
    const membership = membershipById(
      assignment?.subjectMembershipId
    )

    return personLabel(membership?.personId)
  }

  const assignmentTeacherFilterOptions = computed(() => {
    const seenPeople = new Set()
    const result = []

    assignments.value.forEach((assignment) => {
      const membership = membershipById(
        assignment.subjectMembershipId
      )
      const personId = Number(membership?.personId)

      if (!personId || seenPeople.has(personId)) {
        return
      }

      seenPeople.add(personId)
      result.push({
        value: String(personId),
        label: personLabel(personId),
      })
    })

    result.sort((left, right) =>
      left.label.localeCompare(right.label, 'ru')
    )

    return [
      { value: 'all', label: 'Все преподаватели' },
      ...result,
    ]
  })

  const activeAssignments = computed(() => {
    return assignments.value.filter(
      (item) => Number(item.status) === 1
    )
  })

  const summary = computed(() => ({
    assignments: assignments.value.length,
    active: activeAssignments.value.length,
    hours: activeAssignments.value.reduce(
      (sum, item) => sum + Number(item.hoursPerWeek ?? 0),
      0
    ),
    teachers: new Set(
      activeAssignments.value
        .map((item) =>
          membershipById(item.subjectMembershipId)?.personId
        )
        .filter(Boolean)
        .map(Number)
    ).size,
  }))

  const hasActiveFilters = computed(() => {
    return (
      Boolean(searchQuery.value.trim()) ||
      subjectFilter.value !== 'all' ||
      teacherFilter.value !== 'all' ||
      groupFilter.value !== 'all' ||
      loadTypeFilter.value !== 'all' ||
      statusFilter.value !== 'all' ||
      sortMode.value !== 'subject-asc'
    )
  })

  const filteredAssignments = computed(() => {
    const query = searchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    const result = assignments.value.filter((assignment) => {
      const membership = membershipById(
        assignment.subjectMembershipId
      )
      const subjectId = Number(membership?.subjectId)
      const personId = Number(membership?.personId)

      if (
        subjectFilter.value !== 'all' &&
        String(subjectId) !== subjectFilter.value
      ) {
        return false
      }

      if (
        teacherFilter.value !== 'all' &&
        String(personId) !== teacherFilter.value
      ) {
        return false
      }

      if (
        groupFilter.value !== 'all' &&
        String(assignment.groupId) !== groupFilter.value
      ) {
        return false
      }

      if (
        loadTypeFilter.value !== 'all' &&
        String(assignment.loadTypeId) !== loadTypeFilter.value
      ) {
        return false
      }

      if (
        statusFilter.value !== 'all' &&
        String(assignment.status) !== statusFilter.value
      ) {
        return false
      }

      if (!query) {
        return true
      }

      return [
        subjectName(subjectId),
        personLabel(personId),
        groupName(assignment.groupId),
        loadTypeName(assignment.loadTypeId),
        ADMIN_TEACHING_STATUS_LABELS[Number(assignment.status)],
        assignment.notes,
        assignment.hoursPerWeek,
      ]
        .filter((value) => value !== null && value !== undefined)
        .join(' ')
        .toLocaleLowerCase('ru-RU')
        .includes(query)
    })

    return [...result].sort((left, right) => {
      if (sortMode.value === 'hours-desc') {
        return (
          Number(right.hoursPerWeek ?? 0) -
          Number(left.hoursPerWeek ?? 0)
        )
      }

      if (sortMode.value === 'teacher-asc') {
        return teacherNameForAssignment(left).localeCompare(
          teacherNameForAssignment(right),
          'ru'
        )
      }

      if (sortMode.value === 'group-asc') {
        return groupName(left.groupId).localeCompare(
          groupName(right.groupId),
          'ru'
        )
      }

      return assignmentSubjectName(left).localeCompare(
        assignmentSubjectName(right),
        'ru'
      )
    })
  })

  const filterResultText = computed(() => {
    return `Показано: ${filteredAssignments.value.length} из ${assignments.value.length}`
  })

  function statusVariant(status) {
    const normalized = Number(status)

    if (normalized === 1) {
      return 'success'
    }

    if (normalized === 4) {
      return 'warning'
    }

    if (normalized === 3) {
      return 'secondary'
    }

    return 'info'
  }

  function formatHours(value) {
    const number = Number(value ?? 0)

    if (!Number.isFinite(number)) {
      return '0'
    }

    return number.toLocaleString('ru-RU', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    })
  }

  function assignmentPeriodLabel(assignment) {
    return (
      `${assignment.academicYear}, ` +
      `${assignment.studyCourse} курс, ` +
      `${assignment.semester} семестр`
    )
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

  function resetFilters() {
    searchQuery.value = ''
    subjectFilter.value = 'all'
    teacherFilter.value = 'all'
    groupFilter.value = 'all'
    loadTypeFilter.value = 'all'
    statusFilter.value = 'all'
    sortMode.value = 'subject-asc'
  }

  function groupSortLabel(group) {
    return `${group?.name ?? ''} ${group?.code ?? ''}`
  }

  async function reloadLoadTypes() {
    const response = await teachingApi.getLoadTypes()

    loadTypes.value = listFromResponse(response).sort((left, right) =>
      String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    )
  }

  async function loadBaseData() {
    const requestId = baseRequest.begin()
    loadingBase.value = true

    try {
      const [
        facultiesResponse,
        peopleResponse,
        membershipsResponse,
        loadTypesResponse,
      ] = await Promise.all([
        facultiesApi.getAll(),
        usersApi.getPeople(),
        membershipsApi.getSubjectMemberships({
          activeOnly: false,
        }),
        teachingApi.getLoadTypes(),
      ])

      if (!baseRequest.isCurrent(requestId)) {
        return false
      }

      faculties.value = listFromResponse(facultiesResponse).sort(
        (left, right) =>
          String(left.name ?? '').localeCompare(
            String(right.name ?? ''),
            'ru'
          )
      )

      people.value = listFromResponse(peopleResponse)

      teacherMemberships.value = listFromResponse(
        membershipsResponse
      ).filter((item) => Number(item.role) === TEACHER_ROLE)

      loadTypes.value = listFromResponse(loadTypesResponse).sort(
        (left, right) =>
          String(left.name ?? '').localeCompare(
            String(right.name ?? ''),
            'ru'
          )
      )

      if (
        !faculties.value.some(
          (item) => String(item.id) === String(context.facultyId)
        )
      ) {
        context.facultyId = faculties.value[0]?.id
          ? String(faculties.value[0].id)
          : ''
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
          'Не удалось загрузить справочники учебной нагрузки.'
        )
      )
      return false
    } finally {
      if (baseRequest.isCurrent(requestId)) {
        loadingBase.value = false
      }
    }
  }

  async function loadFacultyContext() {
    const requestId = contextRequest.begin()
    assignmentsRequest.invalidate()

    const facultyId = Number(context.facultyId)

    facultySubjects.value = []
    groups.value = []
    assignments.value = []

    if (!facultyId) {
      loadingContext.value = false
      return false
    }

    loadingContext.value = true

    try {
      const [subjectsResponse, groupsResponse] = await Promise.all([
        facultiesApi.getSubjects(facultyId),
        groupsApi.getAll({ facultyId }),
      ])

      if (!contextRequest.isCurrent(requestId)) {
        return false
      }

      facultySubjects.value = listFromResponse(subjectsResponse).sort(
        (left, right) =>
          String(left.name ?? '').localeCompare(
            String(right.name ?? ''),
            'ru'
          )
      )

      groups.value = listFromResponse(groupsResponse).sort(
        (left, right) =>
          groupSortLabel(left).localeCompare(
            groupSortLabel(right),
            'ru'
          )
      )

      resetFilters()
      await refreshAssignments()
      return true
    } catch (error) {
      if (!contextRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить данные выбранного факультета.'
        )
      )
      return false
    } finally {
      if (contextRequest.isCurrent(requestId)) {
        loadingContext.value = false
      }
    }
  }

  async function refreshAssignments() {
    const requestId = assignmentsRequest.begin()
    const facultyId = Number(context.facultyId)
    const academicYear = Number(context.academicYear)

    if (!facultyId || !academicYear || academicYear < 2000) {
      assignments.value = []
      loadingAssignments.value = false
      return false
    }

    loadingAssignments.value = true

    try {
      const response = await teachingApi.getAssignments({
        facultyId,
        studyCourse: Number(context.studyCourse),
        semester: Number(context.semester),
        academicYear,
      })

      if (!assignmentsRequest.isCurrent(requestId)) {
        return false
      }

      assignments.value = listFromResponse(response)
      return true
    } catch (error) {
      if (!assignmentsRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить учебную нагрузку.'
        )
      )
      return false
    } finally {
      if (assignmentsRequest.isCurrent(requestId)) {
        loadingAssignments.value = false
      }
    }
  }

  async function reloadAll() {
    clearNotice()
    await loadBaseData()

    if (context.facultyId) {
      await loadFacultyContext()
    }
  }

  return {
    statusLabels: ADMIN_TEACHING_STATUS_LABELS,
    statusOptions: ADMIN_TEACHING_STATUS_OPTIONS,
    courseOptions: ADMIN_TEACHING_COURSE_OPTIONS,
    semesterOptions: ADMIN_TEACHING_SEMESTER_OPTIONS,
    sortOptions: ADMIN_TEACHING_SORT_OPTIONS,
    faculties,
    facultySubjects,
    groups,
    people,
    teacherMemberships,
    assignments,
    loadTypes,
    loadingBase,
    loadingContext,
    loadingAssignments,
    loading,
    initialized,
    context,
    searchQuery,
    subjectFilter,
    teacherFilter,
    groupFilter,
    loadTypeFilter,
    statusFilter,
    sortMode,
    notice,
    selectedFaculty,
    facultyOptions,
    subjectOptions,
    groupOptions,
    loadTypeOptions,
    assignmentSubjectFilterOptions,
    assignmentTeacherFilterOptions,
    assignmentGroupFilterOptions,
    assignmentLoadTypeFilterOptions,
    assignmentStatusFilterOptions,
    activeAssignments,
    summary,
    hasActiveFilters,
    filteredAssignments,
    filterResultText,
    showNotice,
    clearNotice,
    resetFilters,
    personById,
    personLabel,
    teacherMembershipLabel,
    subjectName,
    groupName,
    membershipById,
    loadTypeName,
    assignmentSubjectName,
    teacherNameForAssignment,
    statusVariant,
    formatHours,
    assignmentPeriodLabel,
    reloadLoadTypes,
    loadBaseData,
    loadFacultyContext,
    refreshAssignments,
    reloadAll,
  }
}
