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
  UiCheckbox,
  UiDrawer,
  UiEmptyState,
  UiFilterBar,
  UiInput,
  UiSelect,
  UiTag,
  UiUnsavedChangesConfirm,
  useOverlayForm,
} from '@/components/ui'

import {
  getApiErrorMessage,
  rolesApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

const roles = ref([])
const users = ref([])
const people = ref([])
const loading = ref(false)

const notice = ref({
  type: 'info',
  message: '',
})

const searchQuery = ref('')
const roleFilter = ref(null)
const activeFilter = ref('all')
const profileFilter = ref('all')
const sortMode = ref('login-asc')

const formError = ref('')
const personCreatorVisible = ref(false)
const personCreateError = ref('')
const personCreateMessage = ref('')
const creatingPerson = ref(false)

const personEditorVisible = ref(false)
const personEditError = ref('')
const personEditMessage = ref('')
const updatingPerson = ref(false)
const personEditDraft = ref({
  firstName: '',
  lastName: '',
  dateOfBirth: '',
  email: '',
  phone: '',
})
const personEditBaseline = ref('')
const personEditGuardVisible = ref(false)
let pendingPersonAction = null

const ACTIVE_OPTIONS = [
  { value: 'all', label: 'Все статусы' },
  { value: 'active', label: 'Активные' },
  { value: 'inactive', label: 'Отключённые' },
]

const PROFILE_OPTIONS = [
  { value: 'all', label: 'Любая привязка' },
  { value: 'bound', label: 'С профилем' },
  { value: 'unbound', label: 'Без профиля' },
]

const SORT_OPTIONS = [
  { value: 'login-asc', label: 'Логин А–Я' },
  { value: 'name-asc', label: 'ФИО А–Я' },
  { value: 'email-asc', label: 'Email А–Я' },
]

function showNotice(type, message) {
  notice.value = {
    type,
    message,
  }
}

function clearNotice() {
  notice.value.message = ''
}

function normalizeRoleName(value) {
  return String(value ?? '')
    .trim()
    .toLocaleLowerCase('ru-RU')
}

function personById(personId) {
  const id = Number(personId)

  if (!Number.isFinite(id)) {
    return null
  }

  return people.value.find(
    (person) => Number(person.id) === id
  ) ?? null
}

function fullName(person) {
  if (!person) {
    return '—'
  }

  const value = [
    person.lastName,
    person.firstName,
    person.middleName,
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return value || '—'
}

function userPerson(user) {
  return personById(user?.personId)
}

function userFullName(user) {
  return fullName(userPerson(user))
}

function userEmail(user) {
  return userPerson(user)?.email || '—'
}

function userPhone(user) {
  return userPerson(user)?.phone || '—'
}

function roleIdsForUser(user) {
  const values = Array.isArray(user?.roleIds)
    ? user.roleIds
    : Array.isArray(user?.roles)
      ? user.roles
      : []

  return values
    .map((value) => {
      if (typeof value === 'number') {
        return value
      }

      if (value && typeof value === 'object') {
        return Number(value.id)
      }

      const normalized = normalizeRoleName(value)
      const role = roles.value.find(
        (item) =>
          normalizeRoleName(item.name) === normalized
      )

      return Number(role?.id)
    })
    .filter(Number.isFinite)
}

function roleNamesForUser(user) {
  const directNames = Array.isArray(user?.roles)
    ? user.roles
        .map((value) =>
          typeof value === 'string'
            ? value
            : value?.name
        )
        .filter(Boolean)
    : []

  if (directNames.length) {
    return [...new Set(directNames)]
      .sort((left, right) =>
        String(left).localeCompare(
          String(right),
          'ru'
        )
      )
  }

  return roleIdsForUser(user)
    .map(
      (roleId) =>
        roles.value.find(
          (role) =>
            Number(role.id) === Number(roleId)
        )?.name
    )
    .filter(Boolean)
}

function roleLabel(role) {
  return String(role?.name ?? 'Роль')
}

function permissionNames(user) {
  const values = Array.isArray(user?.permissions)
    ? user.permissions
    : []

  return [...new Set(
    values
      .map((value) =>
        typeof value === 'string'
          ? value
          : value?.name
      )
      .filter(Boolean)
  )].sort((left, right) =>
    String(left).localeCompare(
      String(right),
      'ru'
    )
  )
}

const roleOptions = computed(() => [
  { value: null, label: 'Все роли' },
  ...roles.value.map((role) => ({
    value: Number(role.id),
    label: roleLabel(role),
  })),
])

const boundPersonIds = computed(() => {
  return new Set(
    users.value
      .filter((user) => user.personId != null)
      .map((user) => Number(user.personId))
      .filter(Number.isFinite)
  )
})

function availablePersonOptionsFor(userId) {
  const currentUser = users.value.find(
    (user) => Number(user.id) === Number(userId)
  )
  const currentPersonId = Number(
    currentUser?.personId
  )

  return people.value
    .filter((person) => {
      const personId = Number(person.id)

      return (
        personId === currentPersonId ||
        !boundPersonIds.value.has(personId)
      )
    })
    .map((person) => ({
      value: Number(person.id),
      label: [
        fullName(person),
        person.email,
      ]
        .filter(Boolean)
        .join(' · '),
    }))
}

const filteredUsers = computed(() => {
  const query = searchQuery.value
    .trim()
    .toLocaleLowerCase('ru-RU')
  const hasRoleFilter = roleFilter.value !== null && roleFilter.value !== ''
  const selectedRoleId = hasRoleFilter
    ? Number(roleFilter.value)
    : null

  const result = users.value.filter((user) => {
    if (
      hasRoleFilter &&
      Number.isFinite(selectedRoleId) &&
      !roleIdsForUser(user).includes(selectedRoleId)
    ) {
      return false
    }

    if (
      activeFilter.value === 'active' &&
      !user.active
    ) {
      return false
    }

    if (
      activeFilter.value === 'inactive' &&
      user.active
    ) {
      return false
    }

    const hasProfile = user.personId != null

    if (
      profileFilter.value === 'bound' &&
      !hasProfile
    ) {
      return false
    }

    if (
      profileFilter.value === 'unbound' &&
      hasProfile
    ) {
      return false
    }

    if (!query) {
      return true
    }

    const haystack = [
      user.login,
      userFullName(user),
      userEmail(user),
      userPhone(user),
      ...roleNamesForUser(user),
    ]
      .join(' ')
      .toLocaleLowerCase('ru-RU')

    return haystack.includes(query)
  })

  return [...result].sort((left, right) => {
    if (sortMode.value === 'name-asc') {
      return userFullName(left).localeCompare(
        userFullName(right),
        'ru'
      )
    }

    if (sortMode.value === 'email-asc') {
      return userEmail(left).localeCompare(
        userEmail(right),
        'ru'
      )
    }

    return String(left.login ?? '').localeCompare(
      String(right.login ?? ''),
      'ru'
    )
  })
})

const hasActiveFilters = computed(() => {
  return Boolean(searchQuery.value.trim()) ||
    roleFilter.value !== null ||
    activeFilter.value !== 'all' ||
    profileFilter.value !== 'all' ||
    sortMode.value !== 'login-asc'
})

function emptyPersonDraft() {
  return {
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    email: '',
    phone: '',
  }
}

function mapPersonToDraft(person) {
  return {
    firstName: String(person?.firstName ?? ''),
    lastName: String(person?.lastName ?? ''),
    dateOfBirth: String(person?.dateOfBirth ?? ''),
    email: String(person?.email ?? ''),
    phone: String(person?.phone ?? ''),
  }
}

function normalizedPersonDraft(draft) {
  return {
    firstName: String(draft?.firstName ?? '').trim(),
    lastName: String(draft?.lastName ?? '').trim(),
    dateOfBirth: String(draft?.dateOfBirth ?? '').trim(),
    email: normalizePersonEmail(draft?.email),
    phone: String(draft?.phone ?? '').trim(),
  }
}

function personDraftKey(draft) {
  return JSON.stringify(normalizedPersonDraft(draft))
}

function mapUserToForm(user) {
  return {
    id: Number(user?.id) || null,
    login: user?.login ?? '',
    active: user?.active !== false,
    personId:
      user?.personId == null
        ? null
        : Number(user.personId),
    roleIds: roleIdsForUser(user),
    newPerson: emptyPersonDraft(),
  }
}

const {
  form: userForm,
  model: userDrawerModel,
  saving: userSaving,
  confirmCloseVisible,
  openEdit: openUserForm,
  requestClose: requestCloseUserDrawer,
  discardAndClose,
  continueEditing,
  beginSaving,
  finishSaving,
  failSaving,
  markClean,
} = useOverlayForm({
  mapEntity: mapUserToForm,
})

const selectedUser = computed(() => {
  return users.value.find(
    (user) =>
      Number(user.id) === Number(userForm.id)
  ) ?? null
})

const selectedPerson = computed(() => {
  return personById(userForm.personId)
})

const personEditorDirty = computed(() => {
  if (!personEditorVisible.value) {
    return false
  }

  return personDraftKey(personEditDraft.value) !==
    personEditBaseline.value
})

const safeUserDrawerModel = computed({
  get: () => userDrawerModel.value,
  set: (nextValue) => {
    if (nextValue) {
      userDrawerModel.value = true
      return
    }

    requestCloseUserDrawerSafely()
  },
})

const selectedRoleItems = computed(() => {
  const selectedIds = new Set(
    (userForm.roleIds ?? []).map(Number)
  )

  return roles.value.filter(
    (role) => selectedIds.has(Number(role.id))
  )
})

const selectedRolePermissionNames = computed(() => {
  return [...new Set(
    selectedRoleItems.value.flatMap(
      (role) =>
        Array.isArray(role.permissions)
          ? role.permissions
              .map((permission) =>
                permission?.name
              )
              .filter(Boolean)
          : []
    )
  )].sort((left, right) =>
    String(left).localeCompare(
      String(right),
      'ru'
    )
  )
})

function roleIdsEqual(left, right) {
  const normalize = (values) =>
    [...new Set(
      (values ?? [])
        .map(Number)
        .filter(Number.isFinite)
    )].sort((a, b) => a - b)

  return JSON.stringify(normalize(left)) ===
    JSON.stringify(normalize(right))
}

function resetPersonCreator() {
  personCreatorVisible.value = false
  personCreateError.value = ''
  personCreateMessage.value = ''

  if (userForm.newPerson) {
    Object.assign(
      userForm.newPerson,
      emptyPersonDraft()
    )
  }
}

function openPersonCreator() {
  personCreateError.value = ''
  personCreateMessage.value = ''
  personEditMessage.value = ''
  personCreatorVisible.value = true
}

function cancelPersonCreator() {
  personCreateError.value = ''

  if (userForm.newPerson) {
    Object.assign(
      userForm.newPerson,
      emptyPersonDraft()
    )
  }

  personCreatorVisible.value = false
}

function normalizePersonEmail(value) {
  return String(value ?? '')
    .trim()
    .toLocaleLowerCase('ru-RU')
}

function resetPersonEditor({ keepMessage = false } = {}) {
  personEditorVisible.value = false
  personEditError.value = ''

  if (!keepMessage) {
    personEditMessage.value = ''
  }

  personEditDraft.value = emptyPersonDraft()
  personEditBaseline.value = ''
}

function openPersonEditor() {
  if (!selectedPerson.value) {
    return
  }

  cancelPersonCreator()
  personCreateMessage.value = ''
  personEditError.value = ''
  personEditMessage.value = ''
  personEditDraft.value = mapPersonToDraft(
    selectedPerson.value
  )
  personEditBaseline.value = personDraftKey(
    personEditDraft.value
  )
  personEditorVisible.value = true
}

function continuePersonEditing() {
  pendingPersonAction = null
  personEditGuardVisible.value = false
}

function discardPersonEditAndContinue() {
  const action = pendingPersonAction
  pendingPersonAction = null
  personEditGuardVisible.value = false
  resetPersonEditor()

  if (typeof action === 'function') {
    action()
  }
}

function runAfterPersonEditorGuard(action) {
  if (updatingPerson.value) {
    return false
  }

  if (personEditorDirty.value) {
    pendingPersonAction = action
    personEditGuardVisible.value = true
    return false
  }

  resetPersonEditor()

  if (typeof action === 'function') {
    action()
  }

  return true
}

function requestOpenPersonCreator() {
  runAfterPersonEditorGuard(() => {
    openPersonCreator()
  })
}

function requestCancelPersonEditor() {
  runAfterPersonEditorGuard(() => {})
}

function normalizeSelectedPersonId(value) {
  if (value === null || value === '') {
    return null
  }

  const id = Number(value)
  return Number.isFinite(id) ? id : null
}

function requestPersonSelectionChange(nextValue) {
  const nextPersonId =
    normalizeSelectedPersonId(nextValue)

  if (nextPersonId === userForm.personId) {
    return
  }

  runAfterPersonEditorGuard(() => {
    personCreateMessage.value = ''
    userForm.personId = nextPersonId
  })
}

function personDraftValidationMessage(
  draft = userForm.newPerson ?? emptyPersonDraft(),
  { excludePersonId = null } = {}
) {
  const firstName = String(draft.firstName ?? '').trim()
  const lastName = String(draft.lastName ?? '').trim()
  const email = normalizePersonEmail(draft.email)
  const phone = String(draft.phone ?? '').trim()
  const dateOfBirth = String(draft.dateOfBirth ?? '').trim()

  if (!firstName) {
    return 'Укажите имя.'
  }

  if (firstName.length > 100) {
    return 'Имя не должно превышать 100 символов.'
  }

  if (!lastName) {
    return 'Укажите фамилию.'
  }

  if (lastName.length > 100) {
    return 'Фамилия не должна превышать 100 символов.'
  }

  if (!email) {
    return 'Укажите email.'
  }

  if (email.length > 255) {
    return 'Email не должен превышать 255 символов.'
  }

  if (!/^[^\s@]+@[^\s@]+$/.test(email)) {
    return 'Укажите корректный email.'
  }

  if (phone.length > 50) {
    return 'Телефон не должен превышать 50 символов.'
  }

  if (
    dateOfBirth &&
    dateOfBirth < '1900-01-01'
  ) {
    return 'Дата рождения не может быть раньше 01.01.1900.'
  }

  const duplicateEmail = people.value.some(
    (person) =>
      Number(person.id) !== Number(excludePersonId) &&
      normalizePersonEmail(person.email) === email
  )

  if (duplicateEmail) {
    return 'Профиль с таким email уже существует.'
  }

  return ''
}

async function createPersonFromDrawer() {
  if (creatingPerson.value) {
    return
  }

  personCreateError.value = ''
  personCreateMessage.value = ''

  const validationMessage =
    personDraftValidationMessage(
      userForm.newPerson
    )

  if (validationMessage) {
    personCreateError.value = validationMessage
    return
  }

  const draft = userForm.newPerson
  const payload = {
    firstName: String(draft.firstName).trim(),
    lastName: String(draft.lastName).trim(),
    dateOfBirth:
      String(draft.dateOfBirth ?? '').trim() || null,
    email: normalizePersonEmail(draft.email),
    phone: String(draft.phone ?? '').trim(),
  }

  creatingPerson.value = true

  try {
    const response = await usersApi.createPerson(
      payload
    )
    const createdPerson = response?.data
    const createdId = Number(createdPerson?.id)

    if (!Number.isFinite(createdId)) {
      throw new Error(
        'Backend не вернул идентификатор созданного профиля.'
      )
    }

    const existingIndex = people.value.findIndex(
      (person) => Number(person.id) === createdId
    )

    if (existingIndex >= 0) {
      people.value.splice(
        existingIndex,
        1,
        createdPerson
      )
    } else {
      people.value.push(createdPerson)
    }

    people.value.sort((left, right) =>
      fullName(left).localeCompare(
        fullName(right),
        'ru'
      )
    )

    userForm.personId = createdId
    Object.assign(
      userForm.newPerson,
      emptyPersonDraft()
    )
    personCreatorVisible.value = false
    personCreateMessage.value =
      'Профиль создан и выбран. Чтобы привязать его к учётной записи, сохраните изменения пользователя.'
  } catch (error) {
    personCreateError.value = getApiErrorMessage(
      error,
      'Не удалось создать профиль.'
    )
  } finally {
    creatingPerson.value = false
  }
}

async function updatePersonFromDrawer() {
  if (
    updatingPerson.value ||
    !selectedPerson.value
  ) {
    return
  }

  personEditError.value = ''
  personEditMessage.value = ''

  const personId = Number(selectedPerson.value.id)
  const validationMessage =
    personDraftValidationMessage(
      personEditDraft.value,
      { excludePersonId: personId }
    )

  if (validationMessage) {
    personEditError.value = validationMessage
    return
  }

  const payload = normalizedPersonDraft(
    personEditDraft.value
  )

  updatingPerson.value = true

  try {
    const response = await usersApi.updatePerson(
      personId,
      {
        ...payload,
        dateOfBirth: payload.dateOfBirth || null,
      }
    )

    const updatedPerson = response?.data
    const updatedId = Number(updatedPerson?.id)

    if (!Number.isFinite(updatedId)) {
      throw new Error(
        'Backend не вернул идентификатор обновлённого профиля.'
      )
    }

    const existingIndex = people.value.findIndex(
      (person) => Number(person.id) === updatedId
    )

    if (existingIndex >= 0) {
      people.value.splice(
        existingIndex,
        1,
        updatedPerson
      )
    } else {
      people.value.push(updatedPerson)
    }

    people.value.sort((left, right) =>
      fullName(left).localeCompare(
        fullName(right),
        'ru'
      )
    )

    personEditMessage.value =
      'Профиль обновлён. Изменения уже отображаются во всех разделах, где используется этот Person.'
    resetPersonEditor({ keepMessage: true })
  } catch (error) {
    personEditError.value = getApiErrorMessage(
      error,
      'Не удалось обновить профиль.'
    )
  } finally {
    updatingPerson.value = false
  }
}

function requestCloseUserDrawerSafely() {
  return runAfterPersonEditorGuard(() => {
    requestCloseUserDrawer()
  })
}

function discardUserChangesAndClose() {
  resetPersonEditor()
  resetPersonCreator()
  discardAndClose()
}

function openUserDrawer(user) {
  formError.value = ''
  resetPersonCreator()
  resetPersonEditor()
  openUserForm(user)
}

function resetFilters() {
  searchQuery.value = ''
  roleFilter.value = null
  activeFilter.value = 'all'
  profileFilter.value = 'all'
  sortMode.value = 'login-asc'
}

async function loadData({ clearMessage = true } = {}) {
  loading.value = true

  if (clearMessage) {
    clearNotice()
  }

  try {
    const [
      rolesResponse,
      usersResponse,
      peopleResponse,
    ] = await Promise.all([
      rolesApi.getAll(),
      usersApi.getAll(),
      usersApi.getPeople(),
    ])

    roles.value = listFromResponse(
      rolesResponse
    ).sort((left, right) =>
      String(left.name ?? '').localeCompare(
        String(right.name ?? ''),
        'ru'
      )
    )

    users.value = listFromResponse(
      usersResponse
    )

    people.value = listFromResponse(
      peopleResponse
    ).sort((left, right) =>
      fullName(left).localeCompare(
        fullName(right),
        'ru'
      )
    )
  } catch (error) {
    showNotice(
      'error',
      getApiErrorMessage(
        error,
        'Не удалось загрузить пользователей и роли.'
      )
    )
  } finally {
    loading.value = false
  }
}

async function saveUser() {
  if (
    userSaving.value ||
    updatingPerson.value ||
    creatingPerson.value ||
    !userForm.id
  ) {
    return
  }

  formError.value = ''

  if (personEditorDirty.value) {
    formError.value =
      'Сначала сохраните или отмените изменения профиля Person.'
    return
  }

  const normalizedRoleIds = [
    ...new Set(
      (userForm.roleIds ?? [])
        .map(Number)
        .filter(Number.isFinite)
    ),
  ]

  if (!normalizedRoleIds.length) {
    formError.value =
      'У пользователя должна остаться хотя бы одна роль.'
    return
  }

  const original = users.value.find(
    (user) =>
      Number(user.id) === Number(userForm.id)
  )

  if (!original) {
    formError.value =
      'Пользователь больше не найден. Обновите список.'
    return
  }

  const nextPersonId =
    userForm.personId == null ||
    userForm.personId === ''
      ? null
      : Number(userForm.personId)

  const currentPersonId =
    original.personId == null
      ? null
      : Number(original.personId)

  const personChanged =
    nextPersonId !== currentPersonId
  const rolesChanged = !roleIdsEqual(
    normalizedRoleIds,
    roleIdsForUser(original)
  )
  const activeChanged =
    Boolean(userForm.active) !==
    Boolean(original.active)

  if (
    !personChanged &&
    !rolesChanged &&
    !activeChanged
  ) {
    finishSaving({ close: true })
    return
  }

  beginSaving()
  let completedSteps = 0

  try {
    if (personChanged) {
      await usersApi.updatePersonBinding(
        userForm.id,
        nextPersonId
      )
      completedSteps += 1
    }

    if (rolesChanged) {
      await usersApi.updateRoles(
        userForm.id,
        {
          roleIds: normalizedRoleIds,
        }
      )
      completedSteps += 1
    }

    if (activeChanged) {
      await usersApi.setActive(
        userForm.id,
        Boolean(userForm.active)
      )
      completedSteps += 1
    }

    await loadData({ clearMessage: false })

    finishSaving({ close: true })

    showNotice(
      'success',
      `Пользователь ${original.login} обновлён.`
    )
  } catch (error) {
    failSaving()

    await loadData({ clearMessage: false })

    const refreshed = users.value.find(
      (user) =>
        Number(user.id) === Number(userForm.id)
    )

    if (refreshed) {
      markClean(mapUserToForm(refreshed))
    }

    const message = getApiErrorMessage(
      error,
      'Не удалось сохранить пользователя.'
    )

    formError.value = completedSteps
      ? `${message} Часть предыдущих изменений уже была применена; форма синхронизирована с сервером.`
      : message
  }
}

onMounted(loadData)
</script>

<template>
  <AdminPageShell
    title="Пользователи"
    description="Поиск пользователей, управление активностью, привязкой профиля и назначенными ролями через явное сохранение изменений."
  >
    <template #actions>
      <UiButton
        type="button"
        variant="secondary"
        icon="pi pi-refresh"
        label="Обновить"
        :loading="loading"
        loading-text="Обновление..."
        :disabled="loading"
        @click="loadData()"
      />
    </template>

    <AdminNotice
      :type="notice.type"
      :message="notice.message"
      @close="clearNotice"
    />

    <div class="admin-users-workspace">
      <UiFilterBar
        v-model="searchQuery"
        search-placeholder="Логин, ФИО, email, телефон или роль"
        :result-count="filteredUsers.length"
        :reset-disabled="!hasActiveFilters"
        @reset="resetFilters"
      >
        <template #filters>
          <UiSelect
            v-model="roleFilter"
            :options="roleOptions"
            placeholder="Все роли"
            aria-label="Фильтр по роли"
          />

          <UiSelect
            v-model="activeFilter"
            :options="ACTIVE_OPTIONS"
            aria-label="Фильтр по активности"
          />

          <UiSelect
            v-model="profileFilter"
            :options="PROFILE_OPTIONS"
            aria-label="Фильтр по профилю"
          />

          <UiSelect
            v-model="sortMode"
            :options="SORT_OPTIONS"
            aria-label="Сортировка пользователей"
          />
        </template>
      </UiFilterBar>

      <UiEmptyState
        v-if="loading"
        description="Загрузка пользователей..."
      />

      <UiEmptyState
        v-else-if="!filteredUsers.length"
        :description="
          hasActiveFilters
            ? 'Пользователи по выбранным фильтрам не найдены.'
            : 'Пользователи пока не найдены.'
        "
      />

      <div
        v-else
        class="admin-user-grid"
      >
        <article
          v-for="user in filteredUsers"
          :key="user.id"
          class="admin-user-card"
        >
          <header class="admin-user-card__header">
            <div class="admin-user-card__identity">
              <div class="admin-user-card__status-row">
                <UiTag
                  :variant="user.active ? 'success' : 'secondary'"
                  :value="user.active ? 'Активен' : 'Отключён'"
                />

                <UiTag
                  :variant="user.personId ? 'info' : 'warning'"
                  :value="user.personId ? 'Профиль привязан' : 'Без профиля'"
                />
              </div>

              <h2 class="admin-user-card__login">
                {{ user.login }}
              </h2>

              <p class="admin-user-card__name">
                {{ userFullName(user) }}
              </p>
            </div>
          </header>

          <dl class="admin-user-card__details">
            <div>
              <dt>Email</dt>
              <dd>{{ userEmail(user) }}</dd>
            </div>

            <div>
              <dt>Телефон</dt>
              <dd>{{ userPhone(user) }}</dd>
            </div>
          </dl>

          <div class="admin-user-card__roles">
            <span class="admin-user-card__section-label">
              Роли
            </span>

            <div
              v-if="roleNamesForUser(user).length"
              class="admin-user-card__tags"
            >
              <UiTag
                v-for="roleName in roleNamesForUser(user)"
                :key="roleName"
                :value="roleName"
              />
            </div>

            <span
              v-else
              class="admin-user-card__empty-copy"
            >
              Роли не назначены
            </span>
          </div>

          <div class="admin-user-card__actions">
            <UiButton
              variant="secondary"
              icon="pi pi-pencil"
              label="Настроить"
              @click="openUserDrawer(user)"
            />
          </div>
        </article>
      </div>
    </div>

    <UiDrawer
      v-model="safeUserDrawerModel"
      :title="`Пользователь ${userForm.login || ''}`"
      width="46rem"
    >
      <div class="admin-user-drawer">
        <UiAlert
          v-if="formError"
          variant="danger"
          :message="formError"
        />

        <UiCard
          title="Учётная запись"
          description="Логин изменяется только через отдельный серверный сценарий и здесь доступен только для просмотра."
          compact
        >
          <dl class="admin-user-drawer__summary">
            <div>
              <dt>Логин</dt>
              <dd>{{ userForm.login || '—' }}</dd>
            </div>

            <div>
              <dt>Текущий профиль</dt>
              <dd>{{ userFullName(selectedUser) }}</dd>
            </div>
          </dl>

          <UiCheckbox
            v-model="userForm.active"
            label="Учётная запись активна"
            description="Отключённый пользователь не должен получать обычный доступ к системе."
            :disabled="userSaving"
          />
        </UiCard>

        <UiCard
          title="Профиль пользователя"
          description="Один профиль Person может быть привязан только к одной учётной записи."
          compact
        >
          <UiSelect
            :model-value="userForm.personId"
            label="Профиль"
            :options="availablePersonOptionsFor(userForm.id)"
            placeholder="Без профиля"
            :filter="true"
            filter-placeholder="Поиск по ФИО или email"
            :clearable="true"
            :disabled="userSaving || creatingPerson || updatingPerson"
            @update:model-value="requestPersonSelectionChange"
          />

          <div class="admin-user-person-actions">
            <UiButton
              v-if="!personCreatorVisible"
              type="button"
              variant="secondary"
              icon="pi pi-user-plus"
              label="Создать новый профиль"
              :disabled="userSaving || creatingPerson || updatingPerson"
              @click="requestOpenPersonCreator"
            />

            <UiButton
              v-if="
                selectedPerson &&
                !personCreatorVisible &&
                !personEditorVisible
              "
              type="button"
              variant="secondary"
              icon="pi pi-user-edit"
              label="Изменить профиль"
              :disabled="userSaving || creatingPerson || updatingPerson"
              @click="openPersonEditor"
            />
          </div>

          <UiAlert
            v-if="personCreateMessage"
            variant="success"
            :message="personCreateMessage"
          />

          <UiAlert
            v-if="personEditMessage"
            variant="success"
            :message="personEditMessage"
          />

          <div
            v-if="personCreatorVisible"
            class="admin-user-person-creator"
          >
            <div>
              <h3 class="admin-user-person-creator__title">
                Новый профиль
              </h3>

              <p class="admin-user-person-creator__description">
                Профиль создаётся сразу на сервере. Его привязка к текущей учётной записи произойдёт только после сохранения пользователя.
              </p>
            </div>

            <UiAlert
              v-if="personCreateError"
              variant="danger"
              :message="personCreateError"
            />

            <div class="admin-user-person-creator__grid">
              <UiInput
                v-model="userForm.newPerson.lastName"
                label="Фамилия"
                maxlength="100"
                required
                :disabled="creatingPerson || userSaving"
              />

              <UiInput
                v-model="userForm.newPerson.firstName"
                label="Имя"
                maxlength="100"
                required
                :disabled="creatingPerson || userSaving"
              />

              <UiInput
                v-model="userForm.newPerson.dateOfBirth"
                type="date"
                label="Дата рождения"
                min="1900-01-01"
                hint="Если дата неизвестна, будет сохранено 01.01.1900."
                :disabled="creatingPerson || userSaving"
              />

              <UiInput
                v-model="userForm.newPerson.email"
                type="email"
                label="Email"
                maxlength="255"
                required
                :disabled="creatingPerson || userSaving"
              />

              <UiInput
                v-model="userForm.newPerson.phone"
                type="tel"
                label="Телефон"
                maxlength="50"
                :disabled="creatingPerson || userSaving"
              />
            </div>

            <div class="admin-user-person-creator__actions">
              <UiButton
                type="button"
                variant="secondary"
                label="Отмена создания"
                :disabled="creatingPerson || userSaving"
                @click="cancelPersonCreator"
              />

              <UiButton
                type="button"
                variant="primary"
                icon="pi pi-user-plus"
                label="Создать профиль"
                :loading="creatingPerson"
                loading-text="Создание..."
                :disabled="creatingPerson || userSaving"
                @click="createPersonFromDrawer"
              />
            </div>
          </div>

          <div
            v-if="personEditorVisible && selectedPerson"
            class="admin-user-person-creator admin-user-person-editor"
          >
            <div>
              <h3 class="admin-user-person-creator__title">
                Редактирование профиля
              </h3>

              <p class="admin-user-person-creator__description">
                Изменения Person сохраняются отдельно и сразу становятся видны во всех разделах системы, где используется этот человек.
              </p>
            </div>

            <UiAlert
              v-if="personEditError"
              variant="danger"
              :message="personEditError"
            />

            <div class="admin-user-person-creator__grid">
              <UiInput
                v-model="personEditDraft.lastName"
                label="Фамилия"
                maxlength="100"
                required
                :disabled="updatingPerson || userSaving"
              />

              <UiInput
                v-model="personEditDraft.firstName"
                label="Имя"
                maxlength="100"
                required
                :disabled="updatingPerson || userSaving"
              />

              <UiInput
                v-model="personEditDraft.dateOfBirth"
                type="date"
                label="Дата рождения"
                min="1900-01-01"
                hint="Если очистить поле, backend оставит текущую дату без изменений."
                :disabled="updatingPerson || userSaving"
              />

              <UiInput
                v-model="personEditDraft.email"
                type="email"
                label="Email"
                maxlength="255"
                required
                :disabled="updatingPerson || userSaving"
              />

              <UiInput
                v-model="personEditDraft.phone"
                type="tel"
                label="Телефон"
                maxlength="50"
                :disabled="updatingPerson || userSaving"
              />
            </div>

            <div class="admin-user-person-creator__actions">
              <UiButton
                type="button"
                variant="secondary"
                label="Отмена редактирования"
                :disabled="updatingPerson || userSaving"
                @click="requestCancelPersonEditor"
              />

              <UiButton
                type="button"
                variant="primary"
                icon="pi pi-save"
                label="Сохранить профиль"
                :loading="updatingPerson"
                loading-text="Сохранение..."
                :disabled="
                  updatingPerson ||
                  userSaving ||
                  !personEditorDirty
                "
                @click="updatePersonFromDrawer"
              />
            </div>
          </div>

          <dl
            v-if="selectedPerson && !personEditorVisible"
            class="admin-user-drawer__summary"
          >
            <div>
              <dt>ФИО</dt>
              <dd>{{ fullName(selectedPerson) }}</dd>
            </div>

            <div>
              <dt>Email</dt>
              <dd>{{ selectedPerson.email || '—' }}</dd>
            </div>

            <div>
              <dt>Телефон</dt>
              <dd>{{ selectedPerson.phone || '—' }}</dd>
            </div>
          </dl>
        </UiCard>

        <UiCard
          title="Роли"
          description="Выберите одну или несколько ролей. Изменения отправятся только после нажатия «Сохранить»."
          compact
        >
          <div class="admin-user-role-picker">
            <UiCheckbox
              v-for="role in roles"
              :key="role.id"
              v-model="userForm.roleIds"
              mode="multiple"
              :value="Number(role.id)"
              :label="role.name"
              :description="role.description || 'Без описания'"
              :disabled="userSaving"
            />
          </div>

          <UiAlert
            v-if="!roles.length"
            variant="warning"
            message="В системе пока нет доступных ролей."
          />
        </UiCard>

        <UiCard
          title="Права выбранных ролей"
          description="Справочно. Здесь показаны права, которые дают выбранные роли; индивидуальные права пользователя этим экраном не изменяются."
          compact
        >
          <div
            v-if="selectedRolePermissionNames.length"
            class="admin-user-permissions"
          >
            <UiTag
              v-for="permission in selectedRolePermissionNames"
              :key="permission"
              :value="permission"
              variant="info"
            />
          </div>

          <UiEmptyState
            v-else
            description="Выбранные роли не содержат перечисленных прав."
            compact
          />

          <p
            v-if="permissionNames(selectedUser).length"
            class="admin-user-drawer__hint"
          >
            Текущие эффективные права пользователя могут дополнительно включать индивидуальные назначения, которые backend возвращает вместе с правами ролей.
          </p>
        </UiCard>
      </div>

      <template #footer>
        <div class="admin-user-drawer__footer">
          <UiButton
            variant="secondary"
            label="Отмена"
            :disabled="userSaving || updatingPerson || creatingPerson"
            @click="requestCloseUserDrawerSafely"
          />

          <UiButton
            variant="primary"
            label="Сохранить изменения"
            :loading="userSaving"
            loading-text="Сохранение..."
            :disabled="
              userSaving ||
              updatingPerson ||
              creatingPerson ||
              personEditorDirty ||
              !roles.length
            "
            @click="saveUser"
          />
        </div>
      </template>
    </UiDrawer>

    <UiUnsavedChangesConfirm
      v-model="confirmCloseVisible"
      :busy="userSaving"
      @continue="continueEditing"
      @discard="discardUserChangesAndClose"
    />

    <UiUnsavedChangesConfirm
      v-model="personEditGuardVisible"
      title="Есть несохранённые изменения профиля"
      message="Изменения Person ещё не сохранены. Их можно продолжить редактировать или отбросить."
      continue-label="Продолжить редактирование"
      discard-label="Отбросить изменения"
      :busy="updatingPerson"
      @continue="continuePersonEditing"
      @discard="discardPersonEditAndContinue"
    />
  </AdminPageShell>
</template>

<style scoped>
.admin-users-workspace,
.admin-user-drawer {
  display: grid;
  gap: 14px;
}

.admin-user-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 320px), 1fr));
  gap: 12px;
}

.admin-user-card {
  min-width: 0;
  padding: 15px;

  display: grid;
  align-content: start;
  gap: 14px;

  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: 12px;
}

.admin-user-card__header,
.admin-user-card__identity,
.admin-user-card__roles {
  min-width: 0;
  display: grid;
  gap: 7px;
}

.admin-user-card__status-row,
.admin-user-card__tags,
.admin-user-permissions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}

.admin-user-card__login {
  margin: 0;

  font-size: 18px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.admin-user-card__name {
  margin: 0;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.admin-user-card__details,
.admin-user-drawer__summary {
  margin: 0;

  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 180px), 1fr));
  gap: 10px;
}

.admin-user-card__details > div,
.admin-user-drawer__summary > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.admin-user-card__details dt,
.admin-user-drawer__summary dt,
.admin-user-card__section-label {
  color: var(--st-text-secondary);

  font-size: 11px;
  font-weight: 700;
  line-height: 1.35;
}

.admin-user-card__details dd,
.admin-user-drawer__summary dd {
  margin: 0;

  color: var(--st-text);

  font-size: 13px;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.admin-user-card__empty-copy,
.admin-user-drawer__hint {
  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.55;
}

.admin-user-drawer__hint {
  margin: 10px 0 0;
}

.admin-user-card__actions,
.admin-user-drawer__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-user-card__actions {
  margin-top: auto;
  padding-top: 2px;
}

.admin-user-role-picker {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(min(100%, 220px), 1fr));
  gap: 8px;
}

.admin-user-person-actions,
.admin-user-person-creator__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-user-person-creator {
  padding: 13px;

  display: grid;
  gap: 12px;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: 10px;
}

.admin-user-person-creator__title {
  margin: 0;

  color: var(--st-text);

  font-size: 15px;
  line-height: 1.4;
}

.admin-user-person-creator__description {
  margin: 4px 0 0;

  color: var(--st-text-secondary);

  font-size: 12px;
  line-height: 1.55;
}

.admin-user-person-creator__grid {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 640px) {
  .admin-user-card__actions,
  .admin-user-drawer__footer {
    align-items: stretch;
    flex-direction: column;
  }

  .admin-user-card__actions > *,
  .admin-user-drawer__footer > *,
  .admin-user-person-actions > *,
  .admin-user-person-creator__actions > * {
    width: 100%;
  }

  .admin-user-person-creator__grid {
    grid-template-columns: 1fr;
  }
}
</style>
