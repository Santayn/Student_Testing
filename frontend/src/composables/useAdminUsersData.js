import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  rolesApi,
  usersApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

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

/**
 * Owns the read-only admin users workspace state: users, roles, people,
 * presentation helpers, filters and stale-request protection.
 * User/Person mutations deliberately stay in UsersView so their partial-save
 * recovery and separate persistence flows remain explicit.
 */
export function useAdminUsersData() {
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

  function resetFilters() {
    searchQuery.value = ''
    roleFilter.value = null
    activeFilter.value = 'all'
    profileFilter.value = 'all'
    sortMode.value = 'login-asc'
  }

  async function loadData({ clearMessage = true } = {}) {
    const requestId = dataRequest.begin()
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

      const nextRoles = listFromResponse(
        rolesResponse
      ).sort((left, right) =>
        String(left.name ?? '').localeCompare(
          String(right.name ?? ''),
          'ru'
        )
      )

      const nextUsers = listFromResponse(
        usersResponse
      )

      const nextPeople = listFromResponse(
        peopleResponse
      ).sort((left, right) =>
        fullName(left).localeCompare(
          fullName(right),
          'ru'
        )
      )

      if (!dataRequest.isCurrent(requestId)) {
        return false
      }

      roles.value = nextRoles
      users.value = nextUsers
      people.value = nextPeople
      return true
    } catch (error) {
      if (!dataRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'error',
        getApiErrorMessage(
          error,
          'Не удалось загрузить пользователей и роли.'
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
    ACTIVE_OPTIONS,
    PROFILE_OPTIONS,
    SORT_OPTIONS,
    roles,
    users,
    people,
    loading,
    notice,
    searchQuery,
    roleFilter,
    activeFilter,
    profileFilter,
    sortMode,
    roleOptions,
    filteredUsers,
    hasActiveFilters,
    showNotice,
    clearNotice,
    personById,
    fullName,
    userFullName,
    userEmail,
    userPhone,
    roleIdsForUser,
    roleNamesForUser,
    permissionNames,
    availablePersonOptionsFor,
    resetFilters,
    loadData,
  }
}
