import {
  computed,
  ref,
} from 'vue'

import {
  coursesApi,
  getApiErrorMessage,
} from '@/api'

import {
  useOverlayForm,
} from '@/components/ui'

import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

import {
  isAssignableTeacherMembership,
} from '@/utils/teacherMembershipEligibility'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

import {
  ADMIN_TEACHING_STATUS_LABELS as STATUS_LABELS,
} from '@/composables/useAdminTeachingAssignmentsData'

export const ADMIN_TEACHING_MAX_HOURS_PER_WEEK = 9999.99

export function useAdminTeachingAssignmentEditor({
  context,
  assignments,
  groups,
  teacherMemberships,
  loadTypes,
  membershipById,
  teacherMembershipLabel,
} = {}) {
  const courseVersionsRequest = createLatestRequestGuard()

  const assignmentFormError = ref('')
  const groupSearchQuery = ref('')
  const courseVersionChoices = ref([])
  const loadingCourseVersions = ref(false)

  const {
    form: assignmentForm,
    model: assignmentDrawerModel,
    isCreate: assignmentIsCreate,
    saving: assignmentSaving,
    confirmCloseVisible: assignmentCloseConfirmVisible,
    openCreate: openCreateAssignmentForm,
    openEdit: openEditAssignmentForm,
    requestClose: requestCloseAssignmentDrawer,
    discardAndClose: discardAssignmentAndClose,
    continueEditing: continueAssignmentEditing,
    beginSaving: beginAssignmentSaving,
    finishSaving: finishAssignmentSaving,
    failSaving: failAssignmentSaving,
  } = useOverlayForm({
    createDefault: () => ({
      id: null,
      subjectId: '',
      subjectMembershipId: '',
      groupId: '',
      groupIds: [],
      loadTypeId: '',
      courseVersionId: '',
      semester: context.semester,
      studyCourse: context.studyCourse,
      academicYear: context.academicYear,
      hoursPerWeek: '',
      status: '1',
      notes: '',
    }),
    mapEntity: (assignment) => {
      const membership = membershipById(
        assignment?.subjectMembershipId
      )

      return {
        id: assignment?.id ?? null,
        subjectId: membership?.subjectId
          ? String(membership.subjectId)
          : '',
        subjectMembershipId:
          assignment?.subjectMembershipId
            ? String(assignment.subjectMembershipId)
            : '',
        groupId: assignment?.groupId
          ? String(assignment.groupId)
          : '',
        groupIds: [],
        loadTypeId: assignment?.loadTypeId
          ? String(assignment.loadTypeId)
          : '',
        courseVersionId:
          assignment?.courseVersionId
            ? String(assignment.courseVersionId)
            : '',
        semester: String(
          assignment?.semester ?? context.semester
        ),
        studyCourse: String(
          assignment?.studyCourse ?? context.studyCourse
        ),
        academicYear: String(
          assignment?.academicYear ?? context.academicYear
        ),
        hoursPerWeek:
          assignment?.hoursPerWeek ?? '',
        status: String(
          assignment?.status ?? 1
        ),
        notes: assignment?.notes ?? '',
      }
    },
  })

  const assignmentDrawerTitle = computed(() => {
    return assignmentIsCreate.value
      ? 'Новое назначение нагрузки'
      : 'Изменение учебной нагрузки'
  })

  const teacherOptionsForForm = computed(() => {
    const subjectId = Number(
      assignmentForm.subjectId
    )
    const currentMembershipId = Number(
      assignmentForm.subjectMembershipId
    )

    if (!subjectId) {
      return []
    }

    return teacherMemberships.value
      .filter((membership) => (
        Number(membership.subjectId) === subjectId &&
        (
          isAssignableTeacherMembership(
            membership
          ) ||
          Number(membership.id) ===
            currentMembershipId
        )
      ))
      .map((membership) => ({
        value: String(membership.id),
        label: teacherMembershipLabel(
          membership
        ),
        disabled:
          !isAssignableTeacherMembership(
            membership
          ) &&
          Number(membership.id) !==
            currentMembershipId,
      }))
      .sort((left, right) =>
        left.label.localeCompare(
          right.label,
          'ru'
        )
      )
  })

  const filteredGroupsForCreate = computed(() => {
    const query = groupSearchQuery.value
      .trim()
      .toLocaleLowerCase('ru-RU')

    return groups.value.filter((group) => {
      if (!query) {
        return true
      }

      return [
        group.name,
        group.code,
      ]
        .filter(Boolean)
        .join(' ')
        .toLocaleLowerCase('ru-RU')
        .includes(query)
    })
  })

  const courseVersionOptions = computed(() => {
    const options = [
      {
        value: '',
        label: 'Без версии курса',
      },
    ]

    courseVersionChoices.value.forEach(
      (item) => {
        options.push({
          value: String(item.id),
          label: item.label,
        })
      }
    )

    const currentId = String(
      assignmentForm.courseVersionId ?? ''
    )

    if (
      currentId &&
      !options.some(
        (item) => item.value === currentId
      )
    ) {
      options.push({
        value: currentId,
        label: 'Текущая версия курса (не найдена в доступных)',
      })
    }

    return options
  })

  function normalizedAssignmentTerm(form = assignmentForm) {
    return {
      subjectId: Number(form.subjectId),
      studyCourse: Number(form.studyCourse),
      semester: Number(form.semester),
      academicYear: Number(form.academicYear),
    }
  }

  function assignmentConflictForGroup(
    groupId,
    form = assignmentForm
  ) {
    const term = normalizedAssignmentTerm(
      form
    )

    if (
      !term.subjectId ||
      !term.studyCourse ||
      !term.semester ||
      !term.academicYear
    ) {
      return null
    }

    return assignments.value.find(
      (assignment) => {
        if (
          Number(assignment.id) ===
          Number(form.id)
        ) {
          return false
        }

        const membership = membershipById(
          assignment.subjectMembershipId
        )

        return (
          Number(membership?.subjectId) ===
            term.subjectId &&
          Number(assignment.groupId) ===
            Number(groupId) &&
          Number(assignment.studyCourse) ===
            term.studyCourse &&
          Number(assignment.semester) ===
            term.semester &&
          Number(assignment.academicYear) ===
            term.academicYear
        )
      }
    ) ?? null
  }

  function groupHasConflict(groupId) {
    return Boolean(
      assignmentConflictForGroup(groupId)
    )
  }

  function groupConflictDescription(groupId) {
    const conflict = assignmentConflictForGroup(
      groupId
    )

    if (!conflict) {
      return ''
    }

    return `Для этого предмета в выбранном периоде нагрузка уже назначена.`
  }

  function assignmentValidationMessage() {
    const subjectId = Number(
      assignmentForm.subjectId
    )
    const membershipId = Number(
      assignmentForm.subjectMembershipId
    )
    const loadTypeId = Number(
      assignmentForm.loadTypeId
    )
    const studyCourse = Number(
      assignmentForm.studyCourse
    )
    const semester = Number(
      assignmentForm.semester
    )
    const academicYear = Number(
      assignmentForm.academicYear
    )
    const hours = Number(
      assignmentForm.hoursPerWeek
    )
    const status = Number(
      assignmentForm.status
    )
    const notes = String(
      assignmentForm.notes ?? ''
    )

    if (!subjectId) {
      return 'Выберите предмет.'
    }

    if (!membershipId) {
      return 'Выберите преподавателя.'
    }

    const membership = membershipById(
      membershipId
    )

    if (
      !membership ||
      Number(membership.subjectId) !== subjectId
    ) {
      return 'Выбранный преподаватель больше не связан с этим предметом.'
    }

    if (assignmentIsCreate.value) {
      const groupIds = uniqueNumbers(
        assignmentForm.groupIds
      )

      if (!groupIds.length) {
        return 'Выберите хотя бы одну группу.'
      }

      if (
        groupIds.some(
          (groupId) =>
            assignmentConflictForGroup(
              groupId
            )
        )
      ) {
        return 'Для одной из выбранных групп уже существует нагрузка по этому предмету и периоду.'
      }
    } else {
      const groupId = Number(
        assignmentForm.groupId
      )

      if (!groupId) {
        return 'Выберите группу.'
      }

      if (
        assignmentConflictForGroup(
          groupId
        )
      ) {
        return 'Для выбранной группы уже существует другая нагрузка по этому предмету и периоду.'
      }
    }

    if (!loadTypeId) {
      return 'Выберите тип нагрузки.'
    }

    if (!loadTypes.value.some(
      (item) =>
        Number(item.id) === loadTypeId
    )) {
      return 'Выбранный тип нагрузки больше не существует.'
    }

    if (!Number.isInteger(studyCourse) || studyCourse < 1) {
      return 'Курс должен быть положительным целым числом.'
    }

    if (![1, 2].includes(semester)) {
      return 'Семестр должен быть 1 или 2.'
    }

    if (!Number.isInteger(academicYear) || academicYear < 2000) {
      return 'Учебный год должен быть не меньше 2000.'
    }

    if (!Number.isFinite(hours) || hours < 0) {
      return 'Часы в неделю должны быть неотрицательным числом.'
    }

    if (hours > ADMIN_TEACHING_MAX_HOURS_PER_WEEK) {
      return `Часы в неделю не могут превышать ${ADMIN_TEACHING_MAX_HOURS_PER_WEEK}.`
    }

    if (!Object.hasOwn(STATUS_LABELS, status)) {
      return 'Выберите корректный статус назначения.'
    }

    if (notes.length > 1000) {
      return 'Примечание не может быть длиннее 1000 символов.'
    }

    return ''
  }

  function openCreateAssignment() {
    assignmentFormError.value = ''
    groupSearchQuery.value = ''
    courseVersionChoices.value = []

    openCreateAssignmentForm({
      semester: context.semester,
      studyCourse: context.studyCourse,
      academicYear: context.academicYear,
    })
  }

  async function openEditAssignment(
    assignment
  ) {
    assignmentFormError.value = ''
    groupSearchQuery.value = ''
    courseVersionChoices.value = []
    openEditAssignmentForm(assignment)
    await loadCourseVersionsForForm()
  }

  function onAssignmentSubjectChange() {
    const membership = membershipById(
      assignmentForm.subjectMembershipId
    )

    if (
      membership &&
      Number(membership.subjectId) !==
        Number(assignmentForm.subjectId)
    ) {
      assignmentForm.subjectMembershipId = ''
    }

    assignmentForm.courseVersionId = ''
    courseVersionChoices.value = []
    assignmentFormError.value = ''
  }

  async function onAssignmentTeacherChange() {
    assignmentForm.courseVersionId = ''
    assignmentFormError.value = ''
    await loadCourseVersionsForForm()
  }

  async function loadCourseVersionsForForm() {
    const requestId = courseVersionsRequest.begin()
    const membership = membershipById(
      assignmentForm.subjectMembershipId
    )
    const subjectId = Number(
      assignmentForm.subjectId
    )

    courseVersionChoices.value = []

    if (
      !membership ||
      !subjectId ||
      Number(membership.subjectId) !== subjectId
    ) {
      loadingCourseVersions.value = false
      return
    }

    loadingCourseVersions.value = true

    try {
      const templatesResponse =
        await coursesApi.getTemplates({
          subjectId,
          authorPersonId: Number(
            membership.personId
          ),
        })

      if (
        !courseVersionsRequest.isCurrent(
          requestId
        )
      ) {
        return
      }

      const templates = listFromResponse(
        templatesResponse
      )

      const versionResponses =
        await Promise.all(
          templates.map((template) =>
            coursesApi
              .getVersions(template.id)
              .then((response) => ({
                template,
                versions:
                  listFromResponse(response),
              }))
          )
        )

      if (
        !courseVersionsRequest.isCurrent(
          requestId
        )
      ) {
        return
      }

      courseVersionChoices.value =
        versionResponses
          .flatMap(({ template, versions }) =>
            versions.map((version) => ({
              ...version,
              label:
                `${template.name} · ` +
                `версия ${version.versionNumber}` +
                (version.title
                  ? ` · ${version.title}`
                  : '') +
                (version.published
                  ? ' · опубликована'
                  : ' · черновик'),
            }))
          )
          .sort(
            (left, right) =>
              left.label.localeCompare(
                right.label,
                'ru'
              )
          )
    } catch (error) {
      if (
        courseVersionsRequest.isCurrent(
          requestId
        )
      ) {
        assignmentFormError.value =
          getApiErrorMessage(
            error,
            'Не удалось загрузить версии курса для выбранного преподавателя.'
          )
      }
    } finally {
      if (
        courseVersionsRequest.isCurrent(
          requestId
        )
      ) {
        loadingCourseVersions.value = false
      }
    }
  }

  function assignmentPayload(groupId) {
    return {
      subjectMembershipId: Number(
        assignmentForm.subjectMembershipId
      ),
      groupId: Number(groupId),
      loadTypeId: Number(
        assignmentForm.loadTypeId
      ),
      courseVersionId:
        assignmentForm.courseVersionId
          ? Number(
              assignmentForm.courseVersionId
            )
          : null,
      semester: Number(
        assignmentForm.semester
      ),
      studyCourse: Number(
        assignmentForm.studyCourse
      ),
      academicYear: Number(
        assignmentForm.academicYear
      ),
      hoursPerWeek: Number(
        assignmentForm.hoursPerWeek
      ),
      status: Number(
        assignmentForm.status
      ),
      notes:
        String(
          assignmentForm.notes ?? ''
        ).trim() || null,
    }
  }

  return {
    assignmentForm,
    assignmentDrawerModel,
    assignmentIsCreate,
    assignmentSaving,
    assignmentCloseConfirmVisible,
    assignmentFormError,
    groupSearchQuery,
    courseVersionChoices,
    loadingCourseVersions,
    assignmentDrawerTitle,
    teacherOptionsForForm,
    filteredGroupsForCreate,
    courseVersionOptions,
    assignmentConflictForGroup,
    groupHasConflict,
    groupConflictDescription,
    assignmentValidationMessage,
    openCreateAssignment,
    openEditAssignment,
    onAssignmentSubjectChange,
    onAssignmentTeacherChange,
    loadCourseVersionsForForm,
    assignmentPayload,
    requestCloseAssignmentDrawer,
    discardAssignmentAndClose,
    continueAssignmentEditing,
    beginAssignmentSaving,
    finishAssignmentSaving,
    failAssignmentSaving,
  }
}
