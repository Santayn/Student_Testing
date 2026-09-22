<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import AdminNotice from '@/components/admin/AdminNotice.vue'
import AdminPageShell from '@/components/admin/AdminPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiDrawer,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  facultiesApi,
  getApiErrorMessage,
  groupsApi,
  membershipsApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const STUDENT_GROUP_ROLE = 1
const ACTIVE_MEMBERSHIP_STATUS = 1
const PAUSED_MEMBERSHIP_STATUS = 2
const REMOVED_MEMBERSHIP_STATUS = 3
const STUDENT_APP_ROLE = 'STUDENT'

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

const formError = ref('')

const deleteTarget = ref(null)
const deleteConfirmVisible = ref(false)
const deletingId = ref(null)
const deleteError = ref('')

const membersDrawerVisible = ref(false)
const membersGroup = ref(null)
const groupMemberships = ref([])
const people = ref([])
const students = ref([])
const membersLoading = ref(false)
const memberSearch = ref('')
const addingPersonId = ref(null)

const memberNotice = ref({
  type: 'info',
  message: '',
})

const memberRemoveTarget = ref(null)
const memberRemoveConfirmVisible = ref(false)
const removingMembershipId = ref(null)
const memberRemoveError = ref('')

let membersLoadSequence = 0

const sortOptions = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'code-asc', label: 'Код А–Я' },
  { value: 'faculty-asc', label: 'Факультет А–Я' },
]

const {
  form,
  model: groupDialogModel,
  isCreate,
  saving,
  confirmCloseVisible,
  openCreate,
  openEdit,
  requestClose,
  discardAndClose,
  continueEditing,
  beginSaving,
  finishSaving,
  failSaving,
} = useOverlayForm({
  createDefault: () => ({
    id: null,
    name: '',
    code: '',
    facultyId: '',
  }),
  mapEntity: (group) => ({
    id: group?.id ?? null,
    name: group?.name ?? '',
    code: group?.code ?? '',
    facultyId: group?.facultyId == null
      ? ''
      : String(group.facultyId),
  }),
})

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

const currentStudentMemberships = computed(() => {
  return groupMemberships.value
    .filter((membership) => (
      Number(membership.role) === STUDENT_GROUP_ROLE &&
      Number(membership.status) === ACTIVE_MEMBERSHIP_STATUS &&
      !membership.removedAtUtc
    ))
    .sort((left, right) =>
      personName(personById(left.personId)).localeCompare(
        personName(personById(right.personId)),
        'ru'
      )
    )
})

const currentStudentPersonIds = computed(() => {
  return new Set(
    currentStudentMemberships.value.map(
      (membership) => Number(membership.personId)
    )
  )
})

const availableStudents = computed(() => {
  return students.value
    .filter((person) =>
      !currentStudentPersonIds.value.has(Number(person.id))
    )
    .sort((left, right) =>
      personName(left).localeCompare(personName(right), 'ru')
    )
})

const normalizedMemberSearch = computed(() => {
  return memberSearch.value
    .trim()
    .toLocaleLowerCase('ru-RU')
})

const filteredCurrentStudentMemberships = computed(() => {
  const query = normalizedMemberSearch.value

  if (!query) {
    return currentStudentMemberships.value
  }

  return currentStudentMemberships.value.filter((membership) =>
    personMatchesSearch(personById(membership.personId), query)
  )
})

const filteredAvailableStudents = computed(() => {
  const query = normalizedMemberSearch.value

  if (!query) {
    return availableStudents.value
  }

  return availableStudents.value.filter((person) =>
    personMatchesSearch(person, query)
  )
})

const membersDrawerTitle = computed(() => {
  if (!membersGroup.value) {
    return 'Состав группы'
  }

  return `Состав группы ${membersGroup.value.code || membersGroup.value.name}`
})

const membersBusy = computed(() => (
  membersLoading.value ||
  addingPersonId.value !== null ||
  removingMembershipId.value !== null
))

const memberResultText = computed(() => {
  return (
    `В группе: ${filteredCurrentStudentMemberships.value.length} из ${currentStudentMemberships.value.length}; ` +
    `доступно: ${filteredAvailableStudents.value.length} из ${availableStudents.value.length}`
  )
})

const groupDialogTitle = computed(() => {
  return isCreate.value
    ? 'Новая группа'
    : 'Редактирование группы'
})

const canSubmit = computed(() => {
  return !groupFormValidationMessage() && !saving.value
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

function facultyName(facultyId) {
  const faculty = faculties.value.find(
    (item) => Number(item.id) === Number(facultyId)
  )

  return faculty?.name ?? 'Факультет не найден'
}

function personById(personId) {
  return people.value.find(
    (person) => Number(person.id) === Number(personId)
  ) ?? null
}

function personName(person) {
  if (!person) {
    return 'Профиль участника недоступен'
  }

  const name = [
    person.lastName,
    person.firstName,
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return name || person.email || 'Профиль участника недоступен'
}

function personContact(person) {
  if (!person) {
    return ''
  }

  return [person.email, person.phone]
    .filter(Boolean)
    .join(' · ')
}

function personMatchesSearch(person, query) {
  if (!query) {
    return true
  }

  const haystack = [
    personName(person),
    person?.email,
    person?.phone,
  ]
    .filter(Boolean)
    .join(' ')
    .toLocaleLowerCase('ru-RU')

  return haystack.includes(query)
}

function pausedStudentMembership(personId) {
  return groupMemberships.value.find((membership) => (
    Number(membership.personId) === Number(personId) &&
    Number(membership.role) === STUDENT_GROUP_ROLE &&
    Number(membership.status) === PAUSED_MEMBERSHIP_STATUS &&
    !membership.removedAtUtc
  )) ?? null
}

function availableStudentActionLabel(person) {
  return pausedStudentMembership(person?.id)
    ? 'Вернуть в группу'
    : 'Добавить'
}

function resetFilters() {
  searchQuery.value = ''
  facultyFilter.value = 'all'
  sortMode.value = 'name-asc'
}

function openCreateGroup() {
  formError.value = ''
  openCreate()
}

function openEditGroup(group) {
  formError.value = ''
  openEdit(group)
}

function requestDeleteGroup(group) {
  deleteTarget.value = group
  deleteError.value = ''
  deleteConfirmVisible.value = true
}

function closeDeleteDialog() {
  if (deletingId.value !== null) {
    return
  }

  deleteConfirmVisible.value = false
  deleteTarget.value = null
  deleteError.value = ''
}

function groupFormValidationMessage() {
  const name = String(form.name ?? '').trim()
  const code = String(form.code ?? '').trim()
  const facultyId = Number(form.facultyId)

  if (!name) {
    return 'Введите название группы.'
  }

  if (name.length > 200) {
    return 'Название группы не может быть длиннее 200 символов.'
  }

  if (!code) {
    return 'Введите код группы.'
  }

  if (code.length > 50) {
    return 'Код группы не может быть длиннее 50 символов.'
  }

  if (!Number.isFinite(facultyId) || facultyId <= 0) {
    return 'Выберите факультет.'
  }

  if (
    !faculties.value.some(
      (faculty) => Number(faculty.id) === facultyId
    )
  ) {
    return 'Выбранный факультет больше недоступен. Обновите страницу и выберите другой.'
  }

  const normalizedCode = code.toLocaleLowerCase('ru-RU')
  const duplicate = groups.value.find((group) => {
    return String(group.code ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU') === normalizedCode &&
      String(group.id) !== String(form.id ?? '')
  })

  if (duplicate) {
    return `Группа с кодом «${code}» уже существует.`
  }

  return ''
}

function clearMemberNotice() {
  memberNotice.value.message = ''
}

function showMemberNotice(type, message) {
  memberNotice.value = {
    type,
    message,
  }
}

function resetMemberSearch() {
  memberSearch.value = ''
}

async function loadGroupMembers(groupId, { quiet = false } = {}) {
  const normalizedGroupId = Number(groupId)

  if (!Number.isFinite(normalizedGroupId) || normalizedGroupId <= 0) {
    return
  }

  const requestId = ++membersLoadSequence

  if (!quiet) {
    membersLoading.value = true
    clearMemberNotice()
  }

  try {
    const [
      membershipsResponse,
      peopleResponse,
      studentsResponse,
    ] = await Promise.all([
      membershipsApi.getGroupMemberships({
        groupId: normalizedGroupId,
        activeOnly: false,
      }),
      usersApi.getPeople(),
      usersApi.getPeople({
        role: STUDENT_APP_ROLE,
      }),
    ])

    if (
      requestId !== membersLoadSequence ||
      Number(membersGroup.value?.id) !== normalizedGroupId
    ) {
      return
    }

    groupMemberships.value = listFromResponse(membershipsResponse)
    people.value = listFromResponse(peopleResponse)
    students.value = listFromResponse(studentsResponse)
  } catch (error) {
    if (requestId !== membersLoadSequence) {
      return
    }

    showMemberNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить состав группы'
      )
    )
  } finally {
    if (requestId === membersLoadSequence) {
      membersLoading.value = false
    }
  }
}

async function openGroupMembers(group) {
  membersGroup.value = group
  memberSearch.value = ''
  groupMemberships.value = []
  people.value = []
  students.value = []
  clearMemberNotice()
  membersDrawerVisible.value = true

  await loadGroupMembers(group.id)
}

function resetMembersDrawer() {
  if (membersBusy.value) {
    return
  }

  membersLoadSequence += 1
  membersGroup.value = null
  groupMemberships.value = []
  people.value = []
  students.value = []
  memberSearch.value = ''
  clearMemberNotice()
}

async function addStudentToGroup(person) {
  const groupId = Number(membersGroup.value?.id)
  const personId = Number(person?.id)

  if (
    !Number.isFinite(groupId) || groupId <= 0 ||
    !Number.isFinite(personId) || personId <= 0 ||
    addingPersonId.value !== null
  ) {
    return
  }

  addingPersonId.value = personId
  clearMemberNotice()

  try {
    const pausedMembership = pausedStudentMembership(personId)

    if (pausedMembership) {
      await membershipsApi.updateGroupMembershipStatus(
        pausedMembership.id,
        { status: ACTIVE_MEMBERSHIP_STATUS }
      )
    } else {
      await membershipsApi.addPersonToGroup(
        groupId,
        {
          personId,
          role: STUDENT_GROUP_ROLE,
          notes: null,
        }
      )
    }

    await loadGroupMembers(groupId, { quiet: true })

    showMemberNotice(
      'success',
      pausedMembership
        ? `${personName(person)} снова в составе группы.`
        : `${personName(person)} добавлен в группу.`
    )
  } catch (error) {
    showMemberNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось добавить студента в группу'
      )
    )
  } finally {
    addingPersonId.value = null
  }
}

function requestRemoveMember(membership) {
  memberRemoveTarget.value = {
    membership,
    person: personById(membership.personId),
  }
  memberRemoveError.value = ''
  memberRemoveConfirmVisible.value = true
}

function closeMemberRemoveDialog() {
  if (removingMembershipId.value !== null) {
    return
  }

  memberRemoveConfirmVisible.value = false
  memberRemoveTarget.value = null
  memberRemoveError.value = ''
}

async function removeStudentFromGroup() {
  const target = memberRemoveTarget.value
  const membership = target?.membership
  const groupId = Number(membersGroup.value?.id)

  if (
    !membership ||
    !Number.isFinite(groupId) || groupId <= 0 ||
    removingMembershipId.value !== null
  ) {
    return
  }

  removingMembershipId.value = membership.id
  memberRemoveError.value = ''

  try {
    await membershipsApi.updateGroupMembershipStatus(
      membership.id,
      { status: REMOVED_MEMBERSHIP_STATUS }
    )

    await loadGroupMembers(groupId, { quiet: true })

    memberRemoveConfirmVisible.value = false
    memberRemoveTarget.value = null

    showMemberNotice(
      'success',
      `${personName(target.person)} убран из группы.`
    )
  } catch (error) {
    memberRemoveError.value = getApiErrorMessage(
      error,
      'Не удалось убрать студента из группы'
    )
  } finally {
    removingMembershipId.value = null
  }
}

async function loadData() {
  loading.value = true

  try {
    const [
      facultiesResponse,
      groupsResponse,
    ] = await Promise.all([
      facultiesApi.getAll(),
      groupsApi.getAll(),
    ])

    faculties.value = listFromResponse(facultiesResponse)
    groups.value = listFromResponse(groupsResponse)
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить группы'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveGroup() {
  const validationMessage = groupFormValidationMessage()

  if (validationMessage) {
    formError.value = validationMessage
    return
  }

  beginSaving()
  formError.value = ''
  clearNotice()

  const payload = {
    name: String(form.name).trim(),
    code: String(form.code).trim(),
    facultyId: Number(form.facultyId),
  }

  try {
    const editingId = form.id

    if (editingId) {
      await groupsApi.update(
        editingId,
        payload
      )
    } else {
      await groupsApi.create(payload)
    }

    await loadData()
    finishSaving({ close: true })

    showNotice(
      'success',
      editingId
        ? 'Группа обновлена.'
        : 'Группа создана.'
    )
  } catch (error) {
    failSaving()
    formError.value = getApiErrorMessage(
      error,
      'Не удалось сохранить группу'
    )
  }
}

async function deleteGroup() {
  const group = deleteTarget.value

  if (!group || deletingId.value !== null) {
    return
  }

  deletingId.value = group.id
  deleteError.value = ''
  clearNotice()

  try {
    await groupsApi.remove(group.id)
    await loadData()

    deleteConfirmVisible.value = false
    deleteTarget.value = null

    showNotice(
      'success',
      'Группа удалена.'
    )
  } catch (error) {
    deleteError.value = getApiErrorMessage(
      error,
      'Не удалось удалить группу'
    )
  } finally {
    deletingId.value = null
  }
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Группы"
    description="Просматривайте учебные группы, находите их по названию, коду или факультету и редактируйте без ухода со страницы."
  >
    <template #actions>
      <UiButton
        size="sm"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        @click="loadData"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <UiCard
      title="Учебные группы"
      description="Каждая группа относится к факультету. Поиск работает по названию, коду группы и названию факультета."
    >
      <div class="admin-groups-workspace">
        <UiFilterBar
          v-model="searchQuery"
          search-placeholder="Название, код или факультет"
          :result-text="filterResultText"
          :reset-disabled="!hasActiveFilters"
          @reset="resetFilters"
        >
          <template #filters>
            <UiSelect
              v-model="facultyFilter"
              label="Факультет"
              :options="facultyFilterOptions"
              size="sm"
            />

            <UiSelect
              v-model="sortMode"
              label="Сортировка"
              :options="sortOptions"
              size="sm"
            />
          </template>

          <template #actions>
            <UiButton
              variant="primary"
              size="sm"
              icon="pi pi-plus"
              label="Добавить группу"
              :disabled="!faculties.length"
              @click="openCreateGroup"
            />
          </template>
        </UiFilterBar>

        <UiAlert
          v-if="!loading && !faculties.length"
          variant="warning"
          message="Сначала создайте хотя бы один факультет — без факультета учебную группу создать нельзя."
        />

        <UiEmptyState
          v-if="loading"
          description="Загрузка групп..."
          compact
        />

        <UiEmptyState
          v-else-if="!groups.length"
          description="Группы ещё не созданы. Добавьте первую группу кнопкой выше."
          compact
        />

        <UiEmptyState
          v-else-if="!filteredGroups.length"
          description="По текущему поиску и фильтрам группы не найдены."
          compact
        >
          <template #actions>
            <UiButton
              variant="secondary"
              size="sm"
              label="Сбросить фильтры"
              @click="resetFilters"
            />
          </template>
        </UiEmptyState>

        <div
          v-else
          class="admin-group-grid"
        >
          <article
            v-for="group in filteredGroups"
            :key="group.id"
            class="admin-group-card"
          >
            <div class="admin-group-card__heading">
              <span class="admin-group-card__code">
                {{ group.code }}
              </span>

              <h2 class="admin-group-card__title">
                {{ group.name }}
              </h2>
            </div>

            <div class="admin-group-card__faculty">
              <span>Факультет</span>
              <strong>{{ facultyName(group.facultyId) }}</strong>
            </div>

            <div class="admin-group-card__actions">
              <UiButton
                variant="secondary"
                size="sm"
                icon="pi pi-users"
                label="Состав группы"
                @click="openGroupMembers(group)"
              />

              <UiButton
                size="sm"
                icon="pi pi-pencil"
                label="Изменить"
                @click="openEditGroup(group)"
              />

              <UiButton
                variant="danger"
                size="sm"
                icon="pi pi-trash"
                label="Удалить"
                :loading="deletingId === group.id"
                loading-text="Удаление..."
                @click="requestDeleteGroup(group)"
              />
            </div>
          </article>
        </div>
      </div>
    </UiCard>

    <UiDrawer
      v-model="membersDrawerVisible"
      :title="membersDrawerTitle"
      width="48rem"
      :closable="!membersBusy"
      :close-on-escape="!membersBusy"
      :dismissable="false"
      @after-hide="resetMembersDrawer"
    >
      <div class="admin-group-members">
        <p class="admin-group-members__intro">
          Управляйте студентами этой группы. Добавление выполняется сразу, а удаление из состава требует подтверждения.
        </p>

        <UiAlert
          v-if="memberNotice.message"
          :variant="memberNotice.type"
          :message="memberNotice.message"
          closable
          @close="clearMemberNotice"
        />

        <UiFilterBar
          v-model="memberSearch"
          search-placeholder="ФИО, email или телефон"
          :result-text="memberResultText"
          :reset-disabled="!memberSearch.trim()"
          @reset="resetMemberSearch"
        />

        <UiEmptyState
          v-if="membersLoading"
          description="Загрузка состава группы..."
          compact
        />

        <template v-else>
          <section class="admin-group-members__section">
            <div class="admin-group-members__section-heading">
              <div>
                <h3>Текущие участники</h3>
                <p>Студенты, которые сейчас входят в группу.</p>
              </div>

              <span class="admin-group-members__count">
                {{ currentStudentMemberships.length }}
              </span>
            </div>

            <UiEmptyState
              v-if="!currentStudentMemberships.length"
              description="В группе пока нет студентов."
              compact
            />

            <UiEmptyState
              v-else-if="!filteredCurrentStudentMemberships.length"
              description="Среди участников ничего не найдено."
              compact
            />

            <div
              v-else
              class="admin-group-members__list"
            >
              <article
                v-for="membership in filteredCurrentStudentMemberships"
                :key="membership.id"
                class="admin-group-member-card"
              >
                <div class="admin-group-member-card__body">
                  <strong>
                    {{ personName(personById(membership.personId)) }}
                  </strong>

                  <span
                    v-if="personContact(personById(membership.personId))"
                    class="admin-group-member-card__meta"
                  >
                    {{ personContact(personById(membership.personId)) }}
                  </span>

                  <span
                    v-if="membership.notes"
                    class="admin-group-member-card__notes"
                  >
                    {{ membership.notes }}
                  </span>
                </div>

                <UiButton
                  variant="danger"
                  size="sm"
                  icon="pi pi-user-minus"
                  label="Убрать"
                  :disabled="addingPersonId !== null"
                  :loading="removingMembershipId === membership.id"
                  loading-text="Удаление..."
                  @click="requestRemoveMember(membership)"
                />
              </article>
            </div>
          </section>

          <section class="admin-group-members__section">
            <div class="admin-group-members__section-heading">
              <div>
                <h3>Доступные студенты</h3>
                <p>Активные пользователи с ролью «Студент», которых сейчас нет в этой группе.</p>
              </div>

              <span class="admin-group-members__count">
                {{ availableStudents.length }}
              </span>
            </div>

            <UiEmptyState
              v-if="!availableStudents.length"
              description="Нет доступных студентов для добавления."
              compact
            />

            <UiEmptyState
              v-else-if="!filteredAvailableStudents.length"
              description="Среди доступных студентов ничего не найдено."
              compact
            />

            <div
              v-else
              class="admin-group-members__list"
            >
              <article
                v-for="person in filteredAvailableStudents"
                :key="person.id"
                class="admin-group-member-card"
              >
                <div class="admin-group-member-card__body">
                  <strong>{{ personName(person) }}</strong>

                  <span
                    v-if="personContact(person)"
                    class="admin-group-member-card__meta"
                  >
                    {{ personContact(person) }}
                  </span>

                  <span
                    v-if="pausedStudentMembership(person.id)"
                    class="admin-group-member-card__hint"
                  >
                    Ранее состоял в группе — назначение будет восстановлено.
                  </span>
                </div>

                <UiButton
                  variant="primary"
                  size="sm"
                  icon="pi pi-user-plus"
                  :label="availableStudentActionLabel(person)"
                  :disabled="removingMembershipId !== null"
                  :loading="addingPersonId === person.id"
                  loading-text="Добавление..."
                  @click="addStudentToGroup(person)"
                />
              </article>
            </div>
          </section>
        </template>
      </div>
    </UiDrawer>

    <UiDialog
      v-model="memberRemoveConfirmVisible"
      title="Убрать студента из группы?"
      width="31rem"
      :closable="removingMembershipId === null"
      :close-on-escape="removingMembershipId === null"
      :dismissable-mask="false"
    >
      <div class="admin-group-delete">
        <UiAlert
          v-if="memberRemoveError"
          variant="danger"
          :message="memberRemoveError"
        />

        <p>
          <strong>{{ personName(memberRemoveTarget?.person) }}</strong>
          перестанет входить в группу
          <strong>«{{ membersGroup?.name }}»</strong>.
          Историческая запись назначения сохранится в системе.
        </p>
      </div>

      <template #footer>
        <div class="admin-group-delete__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="removingMembershipId !== null"
            @click="closeMemberRemoveDialog"
          />

          <UiButton
            variant="danger"
            label="Убрать из группы"
            :loading="removingMembershipId !== null"
            loading-text="Удаление..."
            @click="removeStudentFromGroup"
          />
        </div>
      </template>
    </UiDialog>

    <UiDialog
      v-model="groupDialogModel"
      :title="groupDialogTitle"
      width="38rem"
      :dismissable-mask="false"
    >
      <form
        class="admin-group-form"
        @submit.prevent="saveGroup"
      >
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
        />

        <UiInput
          v-model="form.name"
          label="Название группы"
          maxlength="200"
          :disabled="saving"
          required
        />

        <UiInput
          v-model="form.code"
          label="Код группы"
          maxlength="50"
          :disabled="saving"
          required
        />

        <UiSelect
          v-model="form.facultyId"
          label="Факультет"
          :options="facultyOptions"
          placeholder="Выберите факультет"
          :disabled="saving"
          required
        />

        <div class="admin-group-form__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="saving"
            @click="requestClose"
          />

          <UiButton
            type="submit"
            variant="primary"
            :label="isCreate ? 'Создать группу' : 'Сохранить изменения'"
            :loading="saving"
            loading-text="Сохранение..."
            :disabled="!canSubmit"
          />
        </div>
      </form>
    </UiDialog>

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="saving"
      @continue="continueEditing"
      @discard="discardAndClose"
    />

    <UiDialog
      v-model="deleteConfirmVisible"
      title="Удалить группу?"
      width="31rem"
      :closable="deletingId === null"
      :close-on-escape="deletingId === null"
      :dismissable-mask="false"
    >
      <div class="admin-group-delete">
        <UiAlert
          v-if="deleteError"
          variant="danger"
          :message="deleteError"
        />

        <p>
          Группа
          <strong>«{{ deleteTarget?.name }}»</strong>
          будет удалена. Если группа связана со студентами, учебной нагрузкой, тестами или другими данными и удалить её нельзя, причина останется в этом окне.
        </p>
      </div>

      <template #footer>
        <div class="admin-group-delete__actions">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="deletingId !== null"
            @click="closeDeleteDialog"
          />

          <UiButton
            variant="danger"
            label="Удалить группу"
            :loading="deletingId !== null"
            loading-text="Удаление..."
            @click="deleteGroup"
          />
        </div>
      </template>
    </UiDialog>
  </AdminPageShell>
</template>

<style scoped>
.admin-groups-workspace,
.admin-group-form,
.admin-group-delete {
  display: grid;
  gap: 14px;
}

.admin-group-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 300px), 1fr));
  gap: 12px;
}

.admin-group-card {
  min-width: 0;
  padding: 15px;

  display: grid;
  align-content: start;
  gap: 13px;

  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.admin-group-card__heading {
  min-width: 0;

  display: grid;
  gap: 7px;
}

.admin-group-card__code {
  width: fit-content;
  max-width: 100%;
  padding: 4px 8px;

  color: var(--st-primary);
  background: var(--st-primary-soft);
  border-radius: 999px;

  font-size: 11px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: 0.04em;
  overflow-wrap: anywhere;
}

.admin-group-card__title {
  margin: 0;

  font-size: 18px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.admin-group-card__faculty {
  min-width: 0;
  padding: 10px 11px;

  display: grid;
  gap: 4px;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.admin-group-card__faculty span {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.admin-group-card__faculty strong {
  overflow-wrap: anywhere;
}

.admin-group-card__actions,
.admin-group-form__actions,
.admin-group-delete__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-group-card__actions {
  margin-top: auto;
  padding-top: 2px;
}

.admin-group-delete p {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.65;
  overflow-wrap: anywhere;
}

.admin-group-delete strong {
  color: var(--st-text);
}

.admin-group-members {
  min-width: 0;

  display: grid;
  gap: 18px;
}

.admin-group-members__intro {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 14px;
  line-height: 1.6;
}

.admin-group-members__section {
  min-width: 0;
  padding-top: 2px;

  display: grid;
  gap: 10px;
}

.admin-group-members__section + .admin-group-members__section {
  padding-top: 18px;
  border-top: 1px solid var(--st-border);
}

.admin-group-members__section-heading {
  min-width: 0;

  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.admin-group-members__section-heading h3,
.admin-group-members__section-heading p {
  margin: 0;
}

.admin-group-members__section-heading h3 {
  color: var(--st-text);

  font-size: 16px;
  line-height: 1.35;
}

.admin-group-members__section-heading p {
  margin-top: 4px;

  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.5;
}

.admin-group-members__count {
  min-width: 32px;
  min-height: 32px;
  padding: 5px 9px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color: var(--st-primary);
  background: var(--st-primary-soft);
  border-radius: 999px;

  font-size: 12px;
  font-weight: 800;
}

.admin-group-members__list {
  min-width: 0;

  display: grid;
  gap: 8px;
}

.admin-group-member-card {
  min-width: 0;
  padding: 12px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.admin-group-member-card__body {
  min-width: 0;

  display: grid;
  gap: 4px;
}

.admin-group-member-card__body strong,
.admin-group-member-card__meta,
.admin-group-member-card__notes,
.admin-group-member-card__hint {
  overflow-wrap: anywhere;
}

.admin-group-member-card__meta,
.admin-group-member-card__notes,
.admin-group-member-card__hint {
  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.45;
}

.admin-group-member-card__hint {
  color: var(--st-primary);
}

@media (max-width: 640px) {
  .admin-group-card__actions,
  .admin-group-form__actions,
  .admin-group-delete__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-group-card__actions > *,
  .admin-group-form__actions > *,
  .admin-group-delete__actions > * {
    width: 100%;
  }

  .admin-group-member-card {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-group-member-card > :last-child {
    width: 100%;
  }
}
</style>
