<script setup>
import {
  computed,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDialog,
  UiDrawer,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTag,
  UiTextarea,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  coursesApi,
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
  membershipsApi,
  teachingApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
  uniqueNumbers,
} from '@/utils/apiData'

import {
  isAssignableTeacherMembership,
  revalidateAssignableTeacherMembershipIds,
  TeacherMembershipEligibilityError,
} from '@/utils/teacherMembershipEligibility'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const TEACHER_ROLE = 1
const MAX_HOURS_PER_WEEK = 9999.99

const STATUS_LABELS = {
  1: 'Активно',
  2: 'Черновик',
  3: 'Закрыто',
  4: 'В паузе',
}

const STATUS_OPTIONS = Object.entries(
  STATUS_LABELS
).map(([value, label]) => ({
  value: String(value),
  label,
}))

const COURSE_OPTIONS = Array.from(
  { length: 6 },
  (_, index) => ({
    value: String(index + 1),
    label: String(index + 1),
  })
)

const SEMESTER_OPTIONS = [
  { value: '1', label: '1' },
  { value: '2', label: '2' },
]

const SORT_OPTIONS = [
  { value: 'subject-asc', label: 'Предмет А–Я' },
  { value: 'teacher-asc', label: 'Преподаватель А–Я' },
  { value: 'group-asc', label: 'Группа А–Я' },
  { value: 'hours-desc', label: 'Часы: больше → меньше' },
]

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

const contextRequest = createLatestRequestGuard()
const assignmentsRequest = createLatestRequestGuard()
const courseVersionsRequest = createLatestRequestGuard()

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

const assignmentFormError = ref('')
const groupSearchQuery = ref('')
const courseVersionChoices = ref([])
const loadingCourseVersions = ref(false)

const loadTypeFormError = ref('')
const loadTypeSearchQuery = ref('')

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

const {
  form: loadTypeForm,
  model: loadTypeDialogModel,
  isCreate: loadTypeIsCreate,
  dirty: loadTypeDirty,
  saving: loadTypeSaving,
  confirmCloseVisible: loadTypeCloseConfirmVisible,
  openCreate: openCreateLoadTypeForm,
  openEdit: openEditLoadTypeForm,
  requestClose: requestCloseLoadTypeDialog,
  discardAndClose: discardLoadTypeAndClose,
  continueEditing: continueLoadTypeEditing,
  beginSaving: beginLoadTypeSaving,
  finishSaving: finishLoadTypeSaving,
  failSaving: failLoadTypeSaving,
  resetToBaseline: resetLoadTypeToBaseline,
} = useOverlayForm({
  createDefault: () => ({
    id: null,
    name: '',
    description: '',
  }),
  mapEntity: (loadType) => ({
    id: loadType?.id ?? null,
    name: loadType?.name ?? '',
    description: loadType?.description ?? '',
  }),
})

const loading = computed(() => (
  loadingBase.value ||
  loadingContext.value ||
  loadingAssignments.value
))

const selectedFaculty = computed(() => {
  return faculties.value.find(
    (item) =>
      Number(item.id) ===
      Number(context.facultyId)
  ) ?? null
})

const facultyOptions = computed(() => {
  return faculties.value.map((faculty) => ({
    value: String(faculty.id),
    label: faculty.name,
  }))
})

const subjectOptions = computed(() => {
  return facultySubjects.value.map(
    (subject) => ({
      value: String(subject.id),
      label: subject.name,
    })
  )
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
  ...STATUS_OPTIONS,
]

const assignmentTeacherFilterOptions = computed(() => {
  const seenPeople = new Set()
  const result = []

  assignments.value.forEach((assignment) => {
    const membership = membershipById(
      assignment.subjectMembershipId
    )
    const personId = Number(
      membership?.personId
    )

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
    left.label.localeCompare(
      right.label,
      'ru'
    )
  )

  return [
    { value: 'all', label: 'Все преподаватели' },
    ...result,
  ]
})

const assignmentDrawerTitle = computed(() => {
  return assignmentIsCreate.value
    ? 'Новое назначение нагрузки'
    : 'Изменение учебной нагрузки'
})

const loadTypeDialogTitle = computed(() => {
  return 'Типы нагрузки'
})

const loadTypeEditorTitle = computed(() => {
  return loadTypeIsCreate.value
    ? 'Новый тип нагрузки'
    : 'Редактирование типа'
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
    (sum, item) =>
      sum + Number(item.hoursPerWeek ?? 0),
    0
  ),
  teachers: new Set(
    activeAssignments.value
      .map((item) =>
        membershipById(
          item.subjectMembershipId
        )?.personId
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

  const result = assignments.value.filter(
    (assignment) => {
      const membership = membershipById(
        assignment.subjectMembershipId
      )
      const subjectId = Number(
        membership?.subjectId
      )
      const personId = Number(
        membership?.personId
      )

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
        STATUS_LABELS[Number(assignment.status)],
        assignment.notes,
        assignment.hoursPerWeek,
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
  )

  return [...result].sort(
    (left, right) => {
      if (sortMode.value === 'hours-desc') {
        return (
          Number(right.hoursPerWeek ?? 0) -
          Number(left.hoursPerWeek ?? 0)
        )
      }

      if (sortMode.value === 'teacher-asc') {
        return teacherNameForAssignment(
          left
        ).localeCompare(
          teacherNameForAssignment(right),
          'ru'
        )
      }

      if (sortMode.value === 'group-asc') {
        return groupName(
          left.groupId
        ).localeCompare(
          groupName(right.groupId),
          'ru'
        )
      }

      return assignmentSubjectName(
        left
      ).localeCompare(
        assignmentSubjectName(right),
        'ru'
      )
    }
  )
})

const filterResultText = computed(() => {
  return `Показано: ${filteredAssignments.value.length} из ${assignments.value.length}`
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

const filteredLoadTypes = computed(() => {
  const query = loadTypeSearchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')

  return loadTypes.value
    .filter((item) => {
      if (!query) {
        return true
      }

      return [
        item.name,
        item.description,
      ]
        .filter(Boolean)
        .join(' ')
        .toLocaleLowerCase('ru-RU')
        .includes(query)
    })
    .sort((left, right) =>
      String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
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

function resetFilters() {
  searchQuery.value = ''
  subjectFilter.value = 'all'
  teacherFilter.value = 'all'
  groupFilter.value = 'all'
  loadTypeFilter.value = 'all'
  statusFilter.value = 'all'
  sortMode.value = 'subject-asc'
}

function personById(personId) {
  return people.value.find(
    (item) =>
      Number(item.id) ===
      Number(personId)
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
  const person = personById(
    membership?.personId
  )
  const name = personLabel(
    membership?.personId
  )
  const email = String(
    person?.email ?? ''
  ).trim()

  return email
    ? `${name} · ${email}`
    : name
}

function subjectName(subjectId) {
  return (
    facultySubjects.value.find(
      (item) =>
        Number(item.id) ===
        Number(subjectId)
    )?.name ??
    'Предмет'
  )
}

function groupName(groupId) {
  const group = groups.value.find(
    (item) =>
      Number(item.id) ===
      Number(groupId)
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
    (item) =>
      Number(item.id) === Number(id)
  ) ?? null
}

function loadTypeName(loadTypeId) {
  return (
    loadTypes.value.find(
      (item) =>
        Number(item.id) ===
        Number(loadTypeId)
    )?.name ??
    'Тип нагрузки'
  )
}

function assignmentSubjectName(assignment) {
  const membership = membershipById(
    assignment?.subjectMembershipId
  )

  return subjectName(
    membership?.subjectId
  )
}

function teacherNameForAssignment(assignment) {
  const membership = membershipById(
    assignment?.subjectMembershipId
  )

  return personLabel(
    membership?.personId
  )
}

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

  return number.toLocaleString(
    'ru-RU',
    {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2,
    }
  )
}

function assignmentPeriodLabel(assignment) {
  return (
    `${assignment.academicYear}, ` +
    `${assignment.studyCourse} курс, ` +
    `${assignment.semester} семестр`
  )
}

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

async function currentAssignableMembershipIds(
  membershipIds
) {
  return revalidateAssignableTeacherMembershipIds({
    api: membershipsApi,
    membershipIds,
  })
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

  if (hours > MAX_HOURS_PER_WEEK) {
    return `Часы в неделю не могут превышать ${MAX_HOURS_PER_WEEK}.`
  }

  if (!Object.hasOwn(STATUS_LABELS, status)) {
    return 'Выберите корректный статус назначения.'
  }

  if (notes.length > 1000) {
    return 'Примечание не может быть длиннее 1000 символов.'
  }

  return ''
}

function loadTypeValidationMessage() {
  const name = String(
    loadTypeForm.name ?? ''
  ).trim()
  const description = String(
    loadTypeForm.description ?? ''
  ).trim()

  if (!name) {
    return 'Введите название типа нагрузки.'
  }

  if (name.length > 100) {
    return 'Название типа нагрузки не может быть длиннее 100 символов.'
  }

  if (description.length > 1000) {
    return 'Описание типа нагрузки не может быть длиннее 1000 символов.'
  }

  const normalizedName = name.toLocaleLowerCase(
    'ru-RU'
  )
  const duplicate = loadTypes.value.find(
    (item) =>
      String(item.name ?? '')
        .trim()
        .toLocaleLowerCase('ru-RU') ===
        normalizedName &&
      String(item.id) !==
        String(loadTypeForm.id ?? '')
  )

  if (duplicate) {
    return `Тип нагрузки «${name}» уже существует.`
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

async function ensureSelectedSubjectLoadType() {
  await teachingApi
    .addLoadTypeToSubjectMembership(
      Number(
        assignmentForm.subjectMembershipId
      ),
      {
        teachingLoadTypeId: Number(
          assignmentForm.loadTypeId
        ),
        notes:
          'Добавлено администратором при настройке учебной нагрузки',
      }
    )
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

async function validateSelectedTeacherMembership() {
  const selectedId = Number(
    assignmentForm.subjectMembershipId
  )

  const editingAssignment =
    assignments.value.find(
      (item) =>
        Number(item.id) ===
        Number(assignmentForm.id)
    ) ?? null

  const teacherChanged =
    !editingAssignment ||
    Number(
      editingAssignment.subjectMembershipId
    ) !== selectedId

  const statusRequiresActiveTeacher =
    [1, 2].includes(
      Number(assignmentForm.status)
    )

  if (
    assignmentIsCreate.value ||
    teacherChanged ||
    statusRequiresActiveTeacher
  ) {
    await currentAssignableMembershipIds([
      selectedId,
    ])
  }
}

async function saveAssignment() {
  if (assignmentSaving.value) {
    return
  }

  assignmentFormError.value = ''

  const validation =
    assignmentValidationMessage()

  if (validation) {
    assignmentFormError.value = validation
    return
  }

  beginAssignmentSaving()

  try {
    await validateSelectedTeacherMembership()
    await ensureSelectedSubjectLoadType()

    if (assignmentIsCreate.value) {
      const groupIds = uniqueNumbers(
        assignmentForm.groupIds
      )

      const results =
        await Promise.allSettled(
          groupIds.map((groupId) =>
            teachingApi.createAssignment(
              assignmentPayload(groupId)
            )
          )
        )

      const failedGroupIds = []
      let successCount = 0
      let firstError = null

      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          successCount += 1
          return
        }

        failedGroupIds.push(
          groupIds[index]
        )
        firstError ??= result.reason
      })

      await refreshAssignments()

      if (failedGroupIds.length) {
        const failedNames = failedGroupIds
          .map(groupName)
          .join(', ')

        finishAssignmentSaving({
          close: false,
          values: {
            ...assignmentForm,
            groupIds: failedGroupIds,
          },
        })

        assignmentFormError.value =
          successCount
            ? `Создано назначений: ${successCount}. Не удалось создать для групп: ${failedNames}. ${getApiErrorMessage(firstError, '')}`.trim()
            : getApiErrorMessage(
                firstError,
                'Не удалось создать назначения.'
              )
        return
      }

      showNotice(
        'success',
        groupIds.length === 1
          ? 'Учебная нагрузка создана.'
          : `Создано назначений: ${groupIds.length}.`
      )

      finishAssignmentSaving({ close: true })
      return
    }

    const previous = assignments.value.find(
      (item) =>
        Number(item.id) ===
        Number(assignmentForm.id)
    )

    await teachingApi.updateAssignment(
      assignmentForm.id,
      assignmentPayload(
        assignmentForm.groupId
      )
    )

    const movedOutOfContext =
      Number(assignmentForm.studyCourse) !==
        Number(context.studyCourse) ||
      Number(assignmentForm.semester) !==
        Number(context.semester) ||
      Number(assignmentForm.academicYear) !==
        Number(context.academicYear) ||
      (
        previous &&
        !groups.value.some(
          (group) =>
            Number(group.id) ===
            Number(assignmentForm.groupId)
        )
      )

    await refreshAssignments()

    showNotice(
      'success',
      movedOutOfContext
        ? 'Назначение обновлено и перенесено в другой учебный период.'
        : 'Учебная нагрузка обновлена.'
    )

    finishAssignmentSaving({ close: true })
  } catch (error) {
    if (
      error instanceof
      TeacherMembershipEligibilityError
    ) {
      assignmentFormError.value =
        'Выбранное назначение преподавателя больше не активно. Выберите преподавателя заново.'
    } else {
      assignmentFormError.value =
        getApiErrorMessage(
          error,
          assignmentIsCreate.value
            ? 'Не удалось создать учебную нагрузку.'
            : 'Не удалось обновить учебную нагрузку.'
        )
    }

    failAssignmentSaving()
  }
}

function openLoadTypeManager() {
  loadTypeFormError.value = ''
  loadTypeSearchQuery.value = ''
  openCreateLoadTypeForm()
}

function startNewLoadType() {
  if (loadTypeDirty.value) {
    loadTypeFormError.value =
      'Сначала сохраните или отмените изменения текущего типа нагрузки.'
    return
  }

  loadTypeFormError.value = ''
  openCreateLoadTypeForm()
}

function editLoadType(loadType) {
  if (loadTypeDirty.value) {
    loadTypeFormError.value =
      'Сначала сохраните или отмените изменения текущего типа нагрузки.'
    return
  }

  loadTypeFormError.value = ''
  openEditLoadTypeForm(loadType)
}

function cancelLoadTypeChanges() {
  resetLoadTypeToBaseline()
  loadTypeFormError.value = ''
}

async function saveLoadType() {
  if (loadTypeSaving.value) {
    return
  }

  loadTypeFormError.value = ''
  const validation =
    loadTypeValidationMessage()

  if (validation) {
    loadTypeFormError.value = validation
    return
  }

  const creating = loadTypeIsCreate.value

  beginLoadTypeSaving()

  const payload = {
    name: String(
      loadTypeForm.name
    ).trim(),
    description:
      String(
        loadTypeForm.description ?? ''
      ).trim() || null,
  }

  try {
    const response = creating
      ? await teachingApi.createLoadType(
          payload
        )
      : await teachingApi.updateLoadType(
          loadTypeForm.id,
          payload
        )

    const saved = {
      id: response.data?.id ??
        loadTypeForm.id,
      name: response.data?.name ??
        payload.name,
      description:
        response.data?.description ??
        payload.description ?? '',
    }

    await reloadLoadTypes()

    if (creating) {
      openEditLoadTypeForm(saved)
    } else {
      finishLoadTypeSaving({
        close: false,
        values: saved,
      })
    }

    showNotice(
      'success',
      creating
        ? 'Тип нагрузки создан.'
        : 'Тип нагрузки обновлён.'
    )
  } catch (error) {
    loadTypeFormError.value =
      getApiErrorMessage(
        error,
        creating
          ? 'Не удалось создать тип нагрузки.'
          : 'Не удалось обновить тип нагрузки.'
      )
    failLoadTypeSaving()
  }
}

async function reloadLoadTypes() {
  const response =
    await teachingApi.getLoadTypes()

  loadTypes.value = listFromResponse(
    response
  ).sort((left, right) =>
    String(left.name ?? '').localeCompare(
      String(right.name ?? ''),
      'ru'
    )
  )
}

async function loadBaseData() {
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
      membershipsApi
        .getSubjectMemberships({
          activeOnly: false,
        }),
      teachingApi.getLoadTypes(),
    ])

    faculties.value = listFromResponse(
      facultiesResponse
    ).sort((left, right) =>
      String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    )

    people.value = listFromResponse(
      peopleResponse
    )

    teacherMemberships.value =
      listFromResponse(
        membershipsResponse
      ).filter(
        (item) =>
          Number(item.role) ===
          TEACHER_ROLE
      )

    loadTypes.value = listFromResponse(
      loadTypesResponse
    ).sort((left, right) =>
      String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    )

    if (
      !faculties.value.some(
        (item) =>
          String(item.id) ===
          String(context.facultyId)
      )
    ) {
      context.facultyId =
        faculties.value[0]?.id
          ? String(
              faculties.value[0].id
            )
          : ''
    }
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить справочники учебной нагрузки.'
      )
    )
  } finally {
    loadingBase.value = false
  }
}

async function loadFacultyContext() {
  const requestId = contextRequest.begin()
  assignmentsRequest.invalidate()

  const facultyId = Number(
    context.facultyId
  )

  facultySubjects.value = []
  groups.value = []
  assignments.value = []

  if (!facultyId) {
    loadingContext.value = false
    return
  }

  loadingContext.value = true

  try {
    const [
      subjectsResponse,
      groupsResponse,
    ] = await Promise.all([
      facultiesApi.getSubjects(
        facultyId
      ),
      groupsApi.getAll({
        facultyId,
      }),
    ])

    if (
      !contextRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    facultySubjects.value =
      listFromResponse(
        subjectsResponse
      ).sort((left, right) =>
        String(left.name ?? '').localeCompare(
          String(right.name ?? ''),
          'ru'
        )
      )

    groups.value = listFromResponse(
      groupsResponse
    ).sort((left, right) =>
      groupSortLabel(left).localeCompare(
        groupSortLabel(right),
        'ru'
      )
    )

    resetFilters()
    await refreshAssignments()
  } catch (error) {
    if (
      !contextRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить данные выбранного факультета.'
      )
    )
  } finally {
    if (
      contextRequest.isCurrent(
        requestId
      )
    ) {
      loadingContext.value = false
    }
  }
}

function groupSortLabel(group) {
  return `${group?.name ?? ''} ${group?.code ?? ''}`
}

async function refreshAssignments() {
  const requestId = assignmentsRequest.begin()
  const facultyId = Number(
    context.facultyId
  )
  const academicYear = Number(
    context.academicYear
  )

  if (
    !facultyId ||
    !academicYear ||
    academicYear < 2000
  ) {
    assignments.value = []
    loadingAssignments.value = false
    return
  }

  loadingAssignments.value = true

  try {
    const response = await teachingApi
      .getAssignments({
        facultyId,
        studyCourse: Number(
          context.studyCourse
        ),
        semester: Number(
          context.semester
        ),
        academicYear,
      })

    if (
      !assignmentsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    assignments.value = listFromResponse(
      response
    )
  } catch (error) {
    if (
      !assignmentsRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить учебную нагрузку.'
      )
    )
  } finally {
    if (
      assignmentsRequest.isCurrent(
        requestId
      )
    ) {
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

watch(
  () => context.facultyId,
  () => {
    if (!initialized.value) {
      return
    }

    loadFacultyContext()
  }
)

watch(
  [
    () => context.studyCourse,
    () => context.semester,
    () => context.academicYear,
  ],
  () => {
    if (
      !initialized.value ||
      !context.facultyId
    ) {
      return
    }

    refreshAssignments()
  }
)

onMounted(async () => {
  await loadBaseData()
  initialized.value = true

  if (context.facultyId) {
    await loadFacultyContext()
  }
})
</script>

<template>
  <AdminPageShell
    title="Учебная нагрузка"
    description="Настраивайте назначения преподавателей на группы: предмет, тип нагрузки, часы, учебный период и статус."
  >
    <template #actions>
      <UiButton
        variant="secondary"
        icon="pi pi-tags"
        label="Типы нагрузки"
        :disabled="loading || assignmentDrawerModel"
        @click="openLoadTypeManager"
      />

      <UiButton
        variant="secondary"
        icon="pi pi-refresh"
        label="Обновить"
        :disabled="loading || assignmentDrawerModel"
        @click="reloadAll"
      />

      <UiButton
        variant="primary"
        icon="pi pi-plus"
        label="Добавить нагрузку"
        :disabled="
          loading ||
          !selectedFaculty ||
          !facultySubjects.length ||
          !groups.length
        "
        @click="openCreateAssignment"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Учебный период"
      description="Контекст ограничивает список нагрузки. Период конкретного назначения можно изменить в боковой панели."
    >
      <div class="admin-form-grid admin-form-grid--4">
        <UiSelect
          v-model="context.facultyId"
          label="Факультет"
          :options="facultyOptions"
          placeholder="Выберите факультет"
          :filter="true"
          filter-placeholder="Поиск факультета"
          :disabled="loadingBase || assignmentDrawerModel"
        />

        <UiSelect
          v-model="context.studyCourse"
          label="Курс"
          :options="COURSE_OPTIONS"
          :disabled="assignmentDrawerModel"
        />

        <UiSelect
          v-model="context.semester"
          label="Семестр"
          :options="SEMESTER_OPTIONS"
          :disabled="assignmentDrawerModel"
        />

        <UiInput
          v-model="context.academicYear"
          label="Учебный год"
          type="number"
          min="2000"
          step="1"
          placeholder="2026"
          :disabled="assignmentDrawerModel"
        />
      </div>
    </UiCard>

    <section class="admin-summary workload-summary">
      <div class="admin-stat">
        <span class="admin-stat__label">
          Назначений
        </span>
        <strong class="admin-stat__value">
          {{ summary.assignments }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Активных
        </span>
        <strong class="admin-stat__value">
          {{ summary.active }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Активных часов / нед.
        </span>
        <strong class="admin-stat__value">
          {{ formatHours(summary.hours) }}
        </strong>
      </div>

      <div class="admin-stat">
        <span class="admin-stat__label">
          Преподавателей
        </span>
        <strong class="admin-stat__value">
          {{ summary.teachers }}
        </strong>
      </div>
    </section>

    <UiCard
      title="Назначения"
      :description="selectedFaculty ? `Факультет: ${selectedFaculty.name}.` : 'Выберите факультет.'"
    >
      <div class="workload-stack">
        <UiFilterBar
          v-model="searchQuery"
          aria-label="Фильтры учебной нагрузки"
          search-placeholder="Предмет, преподаватель, группа, тип или примечание"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="subjectFilter"
              :options="assignmentSubjectFilterOptions"
              aria-label="Фильтр по предмету"
              size="sm"
            />

            <UiSelect
              v-model="teacherFilter"
              :options="assignmentTeacherFilterOptions"
              aria-label="Фильтр по преподавателю"
              :filter="true"
              filter-placeholder="Поиск преподавателя"
              size="sm"
            />

            <UiSelect
              v-model="groupFilter"
              :options="assignmentGroupFilterOptions"
              aria-label="Фильтр по группе"
              :filter="true"
              filter-placeholder="Поиск группы"
              size="sm"
            />

            <UiSelect
              v-model="loadTypeFilter"
              :options="assignmentLoadTypeFilterOptions"
              aria-label="Фильтр по типу нагрузки"
              size="sm"
            />

            <UiSelect
              v-model="statusFilter"
              :options="assignmentStatusFilterOptions"
              aria-label="Фильтр по статусу"
              size="sm"
            />

            <UiSelect
              v-model="sortMode"
              :options="SORT_OPTIONS"
              aria-label="Сортировка нагрузки"
              size="sm"
            />
          </template>
        </UiFilterBar>

        <UiEmptyState
          v-if="loadingAssignments || loadingContext"
          description="Загрузка учебной нагрузки..."
          compact
        />

        <UiEmptyState
          v-else-if="!selectedFaculty"
          description="Выберите факультет, чтобы увидеть учебную нагрузку."
          compact
        />

        <UiEmptyState
          v-else-if="!assignments.length"
          description="Для выбранного факультета и периода нагрузка ещё не назначена."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredAssignments.length"
          description="По текущим фильтрам назначения не найдены."
          compact
        />

        <div
          v-else
          class="workload-grid"
        >
          <UiCard
            v-for="assignment in filteredAssignments"
            :key="assignment.id"
            compact
          >
            <article class="workload-assignment">
              <div class="workload-assignment__header">
                <div class="workload-assignment__heading">
                  <h3>
                    {{ assignmentSubjectName(assignment) }}
                  </h3>

                  <p>
                    {{ teacherNameForAssignment(assignment) }}
                  </p>
                </div>

                <div class="workload-assignment__tags">
                  <UiTag
                    :variant="statusVariant(assignment.status)"
                    :value="STATUS_LABELS[Number(assignment.status)] ?? 'Статус'"
                  />

                  <UiTag
                    variant="info"
                    :value="loadTypeName(assignment.loadTypeId)"
                  />
                </div>
              </div>

              <dl class="workload-assignment__meta">
                <div>
                  <dt>Группа</dt>
                  <dd>{{ groupName(assignment.groupId) }}</dd>
                </div>

                <div>
                  <dt>Часы в неделю</dt>
                  <dd>{{ formatHours(assignment.hoursPerWeek) }}</dd>
                </div>

                <div>
                  <dt>Период</dt>
                  <dd>{{ assignmentPeriodLabel(assignment) }}</dd>
                </div>

                <div>
                  <dt>Версия курса</dt>
                  <dd>
                    {{ assignment.courseVersionId ? 'Привязана' : 'Не выбрана' }}
                  </dd>
                </div>
              </dl>

              <p
                v-if="assignment.notes"
                class="workload-assignment__notes"
              >
                {{ assignment.notes }}
              </p>

              <div class="workload-assignment__actions">
                <UiButton
                  variant="secondary"
                  size="sm"
                  icon="pi pi-pencil"
                  label="Изменить"
                  @click="openEditAssignment(assignment)"
                />
              </div>
            </article>
          </UiCard>
        </div>
      </div>
    </UiCard>

    <UiDrawer
      v-model="assignmentDrawerModel"
      :title="assignmentDrawerTitle"
      width="48rem"
    >
      <div class="workload-drawer">
        <UiAlert
          v-if="assignmentFormError"
          variant="danger"
          :message="assignmentFormError"
        />

        <UiAlert
          v-if="!loadTypes.length"
          variant="warning"
          message="Сначала создайте хотя бы один тип нагрузки через действие «Типы нагрузки»."
        />

        <UiCard
          title="Назначение"
          description="Преподаватель выбирается среди активных назначений на выбранный предмет."
          compact
        >
          <div class="admin-form-grid">
            <UiSelect
              v-model="assignmentForm.subjectId"
              label="Предмет"
              :options="subjectOptions"
              placeholder="Выберите предмет"
              :filter="true"
              filter-placeholder="Поиск предмета"
              required
              :disabled="assignmentSaving"
              @change="onAssignmentSubjectChange"
            />

            <UiSelect
              v-model="assignmentForm.subjectMembershipId"
              label="Преподаватель"
              :options="teacherOptionsForForm"
              placeholder="Выберите преподавателя"
              :filter="true"
              filter-placeholder="Поиск по ФИО или email"
              required
              :disabled="assignmentSaving || !assignmentForm.subjectId"
              @change="onAssignmentTeacherChange"
            />

            <UiSelect
              v-model="assignmentForm.loadTypeId"
              label="Тип нагрузки"
              :options="loadTypeOptions"
              placeholder="Выберите тип нагрузки"
              :filter="true"
              filter-placeholder="Поиск типа"
              required
              :disabled="assignmentSaving || !loadTypes.length"
            />

            <UiInput
              v-model="assignmentForm.hoursPerWeek"
              label="Часы в неделю"
              type="number"
              min="0"
              :max="MAX_HOURS_PER_WEEK"
              step="0.25"
              placeholder="2.00"
              required
              :disabled="assignmentSaving"
            />

            <UiSelect
              v-model="assignmentForm.courseVersionId"
              label="Версия курса"
              :options="courseVersionOptions"
              placeholder="Без версии курса"
              :filter="true"
              filter-placeholder="Поиск версии"
              :disabled="assignmentSaving || loadingCourseVersions || !assignmentForm.subjectMembershipId"
              hint="Необязательно. Показываются версии курса выбранного преподавателя по этому предмету."
            />

            <UiSelect
              v-model="assignmentForm.status"
              label="Статус"
              :options="STATUS_OPTIONS"
              required
              :disabled="assignmentSaving"
            />
          </div>
        </UiCard>

        <UiCard
          title="Учебный период"
          description="Изменение периода у существующего назначения может переместить его из текущего списка."
          compact
        >
          <div class="admin-form-grid admin-form-grid--3">
            <UiSelect
              v-model="assignmentForm.studyCourse"
              label="Курс"
              :options="COURSE_OPTIONS"
              required
              :disabled="assignmentSaving"
            />

            <UiSelect
              v-model="assignmentForm.semester"
              label="Семестр"
              :options="SEMESTER_OPTIONS"
              required
              :disabled="assignmentSaving"
            />

            <UiInput
              v-model="assignmentForm.academicYear"
              label="Учебный год"
              type="number"
              min="2000"
              step="1"
              required
              :disabled="assignmentSaving"
            />
          </div>
        </UiCard>

        <UiCard
          v-if="assignmentIsCreate"
          title="Группы"
          description="Можно создать одинаковую нагрузку сразу для нескольких групп факультета."
          compact
        >
          <UiInput
            v-model="groupSearchQuery"
            label="Поиск группы"
            placeholder="Название или код группы"
            :disabled="assignmentSaving"
          />

          <div
            v-if="filteredGroupsForCreate.length"
            class="workload-group-picker"
          >
            <UiCheckbox
              v-for="group in filteredGroupsForCreate"
              :key="group.id"
              v-model="assignmentForm.groupIds"
              mode="multiple"
              :value="group.id"
              :label="group.name"
              :description="
                groupHasConflict(group.id)
                  ? groupConflictDescription(group.id)
                  : group.code || 'Доступна для назначения'
              "
              :disabled="
                assignmentSaving ||
                (
                  groupHasConflict(group.id) &&
                  !assignmentForm.groupIds.includes(group.id)
                )
              "
            />
          </div>

          <UiEmptyState
            v-else
            description="Группы по поиску не найдены."
            compact
          />
        </UiCard>

        <UiCard
          v-else
          title="Группа"
          description="Для одного существующего назначения выбирается одна группа."
          compact
        >
          <UiSelect
            v-model="assignmentForm.groupId"
            label="Группа"
            :options="groupOptions"
            placeholder="Выберите группу"
            :filter="true"
            filter-placeholder="Поиск группы"
            required
            :disabled="assignmentSaving"
          />
        </UiCard>

        <UiTextarea
          v-model="assignmentForm.notes"
          label="Примечание"
          maxlength="1000"
          placeholder="Необязательное примечание к нагрузке"
          :disabled="assignmentSaving"
        />
      </div>

      <template #footer>
        <div class="workload-drawer__footer">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="assignmentSaving"
            @click="requestCloseAssignmentDrawer"
          />

          <UiButton
            variant="primary"
            :label="assignmentIsCreate ? 'Создать нагрузку' : 'Сохранить изменения'"
            :loading="assignmentSaving"
            loading-text="Сохранение..."
            :disabled="assignmentSaving || !loadTypes.length"
            @click="saveAssignment"
          />
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="assignmentCloseConfirmVisible"
      :busy="assignmentSaving"
      @continue="continueAssignmentEditing"
      @discard="discardAssignmentAndClose"
    />

    <UiDialog
      v-model="loadTypeDialogModel"
      :title="loadTypeDialogTitle"
      width="52rem"
    >
      <div class="load-type-manager">
        <UiAlert
          v-if="loadTypeFormError"
          variant="danger"
          :message="loadTypeFormError"
        />

        <div class="load-type-manager__toolbar">
          <UiInput
            v-model="loadTypeSearchQuery"
            label="Поиск"
            placeholder="Название или описание"
          />

          <UiButton
            variant="secondary"
            icon="pi pi-plus"
            label="Новый тип"
            :disabled="loadTypeSaving"
            @click="startNewLoadType"
          />
        </div>

        <div class="load-type-manager__layout">
          <section class="load-type-manager__list">
            <UiEmptyState
              v-if="!filteredLoadTypes.length"
              description="Типы нагрузки не найдены."
              compact
            />

            <template v-else>
              <button
                v-for="loadType in filteredLoadTypes"
                :key="loadType.id"
                type="button"
                class="load-type-item"
                :class="{
                  'load-type-item--active':
                    Number(loadTypeForm.id) === Number(loadType.id),
                }"
                :disabled="loadTypeSaving"
                @click="editLoadType(loadType)"
              >
                <strong>{{ loadType.name }}</strong>
                <span>{{ loadType.description || 'Без описания' }}</span>
              </button>
            </template>
          </section>

          <section class="load-type-manager__editor">
            <h3>{{ loadTypeEditorTitle }}</h3>

            <UiInput
              v-model="loadTypeForm.name"
              label="Название"
              maxlength="100"
              required
              :disabled="loadTypeSaving"
            />

            <UiTextarea
              v-model="loadTypeForm.description"
              label="Описание"
              maxlength="1000"
              placeholder="Необязательное описание"
              :disabled="loadTypeSaving"
            />

            <p class="load-type-manager__hint">
              Тип нагрузки нельзя удалить через текущий backend API, но его название и описание можно изменить.
            </p>

            <div class="admin-actions admin-actions--end admin-actions--mobile-stack">
              <UiButton
                v-if="loadTypeDirty"
                variant="secondary"
                label="Отменить изменения"
                :disabled="loadTypeSaving"
                @click="cancelLoadTypeChanges"
              />

              <UiButton
                variant="primary"
                :label="loadTypeIsCreate ? 'Создать тип' : 'Сохранить тип'"
                :loading="loadTypeSaving"
                loading-text="Сохранение..."
                :disabled="loadTypeSaving"
                @click="saveLoadType"
              />
            </div>
          </section>
        </div>
      </div>
    </UiDialog>

    <UiUnsavedChangesConfirm
      v-model="loadTypeCloseConfirmVisible"
      :busy="loadTypeSaving"
      @continue="continueLoadTypeEditing"
      @discard="discardLoadTypeAndClose"
    />
  </AdminPageShell>
</template>

<style scoped>
.workload-stack,
.workload-drawer,
.load-type-manager {
  display: grid;
  gap: 16px;
}

.workload-grid {
  display: grid;
  grid-template-columns: repeat(
    auto-fill,
    minmax(330px, 1fr)
  );
  gap: 12px;
}

.workload-assignment {
  min-width: 0;
  height: 100%;

  display: flex;
  flex-direction: column;
  gap: 14px;
}

.workload-assignment__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.workload-assignment__heading {
  min-width: 0;
}

.workload-assignment__heading h3,
.workload-assignment__heading p {
  margin: 0;
}

.workload-assignment__heading h3 {
  color: var(--st-text);

  font-size: 17px;
  line-height: 1.35;
}

.workload-assignment__heading p {
  margin-top: 4px;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.45;
}

.workload-assignment__tags {
  max-width: 50%;

  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 6px;
}

.workload-assignment__meta {
  margin: 0;

  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.workload-assignment__meta > div {
  min-width: 0;
  padding: 10px;

  display: grid;
  gap: 3px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.workload-assignment__meta dt {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.workload-assignment__meta dd {
  margin: 0;

  color: var(--st-text);

  overflow-wrap: anywhere;

  font-size: 13px;
  font-weight: 700;
  line-height: 1.4;
}

.workload-assignment__notes {
  margin: 0;
  padding: 10px 12px;

  color: var(--st-text-secondary);
  background: var(--st-surface-muted);
  border-radius: 9px;

  overflow-wrap: anywhere;

  font-size: 13px;
  line-height: 1.5;
}

.workload-assignment__actions {
  margin-top: auto;

  display: flex;
  justify-content: flex-end;
}

.workload-group-picker {
  max-height: 360px;
  overflow-y: auto;

  margin-top: 12px;

  display: grid;
  gap: 7px;
}

.workload-drawer__footer {
  width: 100%;

  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.load-type-manager__toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 10px;
}

.load-type-manager__layout {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(0, 1.2fr);
  gap: 16px;
}

.load-type-manager__list,
.load-type-manager__editor {
  min-width: 0;

  display: grid;
  align-content: start;
  gap: 9px;
}

.load-type-manager__list {
  max-height: 430px;
  overflow-y: auto;
}

.load-type-manager__editor {
  padding: 14px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.load-type-manager__editor h3 {
  margin: 0 0 2px;

  color: var(--st-text);

  font-size: 16px;
}

.load-type-item {
  width: 100%;
  min-height: 56px;
  padding: 10px 12px;

  color: var(--st-text);
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 9px;

  display: grid;
  gap: 4px;

  text-align: left;
  cursor: pointer;
}

.load-type-item:hover,
.load-type-item--active {
  border-color: var(--st-primary);
}

.load-type-item strong {
  font-size: 13px;
}

.load-type-item span,
.load-type-manager__hint {
  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.45;
}

.load-type-manager__hint {
  margin: 0;
}

@media (max-width: 900px) {
  .load-type-manager__layout {
    grid-template-columns: 1fr;
  }

  .load-type-manager__list {
    max-height: 240px;
  }
}

@media (max-width: 640px) {
  .workload-grid,
  .workload-assignment__meta,
  .load-type-manager__toolbar {
    grid-template-columns: 1fr;
  }

  .workload-assignment__header {
    flex-direction: column;
  }

  .workload-assignment__tags {
    max-width: none;
    justify-content: flex-start;
  }

  .workload-assignment__actions,
  .workload-assignment__actions > *,
  .workload-drawer__footer,
  .workload-drawer__footer > * {
    width: 100%;
  }

  .workload-drawer__footer {
    flex-direction: column-reverse;
  }
}
</style>
