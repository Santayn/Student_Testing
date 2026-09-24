import { computed, ref } from 'vue'

/**
 * Read-only presentation state for the teacher workload. The view and the
 * network loader share the same refs; no assignment is mutated or refetched
 * when the user changes the client-side filters.
 */
export function useTeacherWorkloadPresentation({
  subjectMemberships,
  subjects,
  assignments,
  loadTypes,
  studyCourse,
  semester,
  academicYear,
}) {
  const searchQuery = ref('')
  const subjectFilter = ref('all')
  const groupFilter = ref('all')
  const loadTypeFilter = ref('all')
  const statusFilter = ref('all')

  const membershipById = computed(() => {
    return new Map(
      subjectMemberships.value.map(
        (item) => [
          Number(item.id),
          item,
        ]
      )
    )
  })

  const subjectById = computed(() => {
    return new Map(
      subjects.value.map(
        (item) => [
          Number(item.id),
          item,
        ]
      )
    )
  })

  const loadTypeById = computed(() => {
    return new Map(
      loadTypes.value.map(
        (item) => [
          Number(item.id),
          item,
        ]
      )
    )
  })

  const subjectFilterOptions = computed(() => {
    const ids = [
      ...new Set(
        assignments.value
          .map((assignment) => {
            const membership =
              membershipById.value.get(
                Number(
                  assignment.subjectMembershipId
                )
              )

            return Number(
              membership?.subjectId
            )
          })
          .filter(Boolean)
      ),
    ]

    return [
      { value: 'all', label: 'Все предметы' },
      ...ids
        .map((subjectId) => ({
          value: String(subjectId),
          label: subjectName(subjectId),
        }))
        .sort((left, right) =>
          left.label.localeCompare(
            right.label,
            'ru'
          )
        ),
    ]
  })

  const groupFilterOptions = computed(() => {
    const groups = new Map()

    assignments.value.forEach(
      (assignment) => {
        const id = Number(
          assignment.groupId
        )

        if (!id || groups.has(id)) {
          return
        }

        groups.set(
          id,
          groupName(assignment)
        )
      }
    )

    return [
      { value: 'all', label: 'Все группы' },
      ...[...groups.entries()]
        .map(([id, label]) => ({
          value: String(id),
          label,
        }))
        .sort((left, right) =>
          left.label.localeCompare(
            right.label,
            'ru'
          )
        ),
    ]
  })

  const loadTypeFilterOptions = computed(() => {
    const ids = [
      ...new Set(
        assignments.value
          .map((assignment) =>
            Number(
              assignment.loadTypeId
            )
          )
          .filter(Boolean)
      ),
    ]

    return [
      { value: 'all', label: 'Все типы' },
      ...ids
        .map((loadTypeId) => ({
          value: String(loadTypeId),
          label: loadTypeName(
            loadTypeId
          ),
        }))
        .sort((left, right) =>
          left.label.localeCompare(
            right.label,
            'ru'
          )
        ),
    ]
  })

  const statusFilterOptions = computed(() => {
    const statuses = [
      ...new Set(
        assignments.value
          .map((assignment) =>
            Number(assignment.status)
          )
          .filter((value) =>
            Number.isFinite(value)
          )
      ),
    ]

    return [
      { value: 'all', label: 'Все статусы' },
      ...statuses.map((status) => ({
        value: String(status),
        label: statusLabel(status),
      })),
    ]
  })

  const hasActiveWorkspaceFilters = computed(() => {
    return (
      Boolean(
        searchQuery.value.trim()
      ) ||
      subjectFilter.value !== 'all' ||
      groupFilter.value !== 'all' ||
      loadTypeFilter.value !== 'all' ||
      statusFilter.value !== 'all'
    )
  })

  const filteredAssignments = computed(() => {
    const query =
      searchQuery.value
        .trim()
        .toLocaleLowerCase('ru')

    return assignments.value.filter(
      (assignment) => {
        const membership =
          membershipById.value.get(
            Number(
              assignment.subjectMembershipId
            )
          )

        const subjectId = Number(
          membership?.subjectId
        )

        if (
          subjectFilter.value !== 'all' &&
          String(subjectId) !==
            subjectFilter.value
        ) {
          return false
        }

        if (
          groupFilter.value !== 'all' &&
          String(
            assignment.groupId
          ) !== groupFilter.value
        ) {
          return false
        }

        if (
          loadTypeFilter.value !== 'all' &&
          String(
            assignment.loadTypeId
          ) !== loadTypeFilter.value
        ) {
          return false
        }

        if (
          statusFilter.value !== 'all' &&
          String(
            assignment.status
          ) !== statusFilter.value
        ) {
          return false
        }

        if (!query) {
          return true
        }

        const searchable = [
          subjectName(subjectId),
          groupName(assignment),
          loadTypeName(
            assignment.loadTypeId
          ),
          statusLabel(
            assignment.status
          ),
          assignment.notes,
          assignment.courseVersionId
            ? `версия курса ${assignment.courseVersionId}`
            : '',
        ]
          .filter(Boolean)
          .join(' ')
          .toLocaleLowerCase('ru')

        return searchable.includes(query)
      }
    )
  })

  const workloadResultText = computed(() => {
    if (
      filteredAssignments.value.length ===
      assignments.value.length
    ) {
      return `Записей нагрузки: ${assignments.value.length}`
    }

    return (
      `Показано: ${filteredAssignments.value.length} ` +
      `из ${assignments.value.length}`
    )
  })

  const periodLabel = computed(() => {
    return (
      `${studyCourse.value} курс, ` +
      `${semester.value} семестр, ` +
      `${academicYear.value}`
    )
  })

  const subjectCount = computed(() => {
    return new Set(
      filteredAssignments.value
        .map((item) => {
          const membership =
            membershipById.value.get(
              Number(
                item.subjectMembershipId
              )
            )

          return Number(
            membership?.subjectId
          )
        })
        .filter(Boolean)
    ).size
  })

  const groupCount = computed(() => {
    return new Set(
      filteredAssignments.value
        .map((item) => Number(item.groupId))
        .filter(Boolean)
    ).size
  })

  const totalHoursPerWeek = computed(() => {
    return filteredAssignments.value.reduce(
      (sum, item) =>
        sum + Number(item.hoursPerWeek ?? 0),
      0
    )
  })

  const groupedAssignments = computed(() => {
    const groups = new Map()

    filteredAssignments.value.forEach(
      (assignment) => {
        const membershipId = Number(
          assignment.subjectMembershipId
        )

        if (!membershipId) {
          return
        }

        if (!groups.has(membershipId)) {
          groups.set(membershipId, [])
        }

        groups.get(membershipId).push(
          assignment
        )
      }
    )

    return [...groups.entries()]
      .map(([subjectMembershipId, items]) => {
        const membership =
          membershipById.value.get(
            Number(subjectMembershipId)
          )

        const subjectId = Number(
          membership?.subjectId
        )

        return {
          subjectMembershipId,
          subjectId,
          subjectName:
            subjectName(subjectId),
          items: [...items].sort(
            (left, right) =>
              groupName(left).localeCompare(
                groupName(right),
                'ru'
              ) ||
              loadTypeName(left.loadTypeId)
                .localeCompare(
                  loadTypeName(
                    right.loadTypeId
                  ),
                  'ru'
                )
          ),
        }
      })
      .sort(
        (left, right) =>
          left.subjectName.localeCompare(
            right.subjectName,
            'ru'
          )
      )
  })

  function subjectName(subjectId) {
    if (!subjectId) {
      return 'Предмет не определён'
    }

    return (
      subjectById.value.get(
        Number(subjectId)
      )?.name ??
      `Предмет #${subjectId}`
    )
  }

  function groupName(assignment) {
    return (
      assignment.groupName ||
      assignment.groupCode ||
      `Группа #${assignment.groupId}`
    )
  }

  function loadTypeName(loadTypeId) {
    return (
      loadTypeById.value.get(
        Number(loadTypeId)
      )?.name ??
      `Тип нагрузки #${loadTypeId}`
    )
  }

  function statusLabel(status) {
    switch (Number(status)) {
      case 1:
        return 'Активно'
      case 2:
        return 'Черновик'
      case 3:
        return 'Закрыто'
      case 4:
        return 'Приостановлено'
      default:
        return `Статус #${status}`
    }
  }

  function statusClass(status) {
    switch (Number(status)) {
      case 1:
        return 'teacher-status teacher-status--success'
      case 2:
        return 'teacher-status teacher-status--warning'
      case 3:
        return 'teacher-status'
      case 4:
        return 'teacher-status teacher-status--danger'
      default:
        return 'teacher-status'
    }
  }

  function formatHours(value) {
    const numeric = Number(value ?? 0)

    if (!Number.isFinite(numeric)) {
      return '0'
    }

    return numeric.toLocaleString('ru-RU', {
      maximumFractionDigits: 2,
    })
  }

  function resetWorkspaceFilters() {
    searchQuery.value = ''
    subjectFilter.value = 'all'
    groupFilter.value = 'all'
    loadTypeFilter.value = 'all'
    statusFilter.value = 'all'
  }
  return {
    searchQuery,
    subjectFilter,
    groupFilter,
    loadTypeFilter,
    statusFilter,
    subjectFilterOptions,
    groupFilterOptions,
    loadTypeFilterOptions,
    statusFilterOptions,
    hasActiveWorkspaceFilters,
    filteredAssignments,
    workloadResultText,
    periodLabel,
    subjectCount,
    groupCount,
    totalHoursPerWeek,
    groupedAssignments,
    subjectName,
    groupName,
    loadTypeName,
    statusLabel,
    statusClass,
    formatHours,
    resetWorkspaceFilters,
  }
}
