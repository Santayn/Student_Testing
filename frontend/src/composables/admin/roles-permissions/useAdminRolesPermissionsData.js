import {
  computed,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  rolesApi,
} from '@/api'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const ROLE_PERMISSION_FILTER_OPTIONS = [
  { value: 'all', label: 'Все роли' },
  { value: 'with', label: 'С правами' },
  { value: 'without', label: 'Без прав' },
]

const ROLE_SORT_OPTIONS = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'permissions-desc', label: 'Сначала больше прав' },
]

const PERMISSION_USAGE_OPTIONS = [
  { value: 'all', label: 'Все permissions' },
  { value: 'used', label: 'Назначены ролям' },
  { value: 'unused', label: 'Не используются' },
]

const PERMISSION_SORT_OPTIONS = [
  { value: 'name-asc', label: 'Название А–Я' },
  { value: 'name-desc', label: 'Название Я–А' },
  { value: 'usage-desc', label: 'Сначала чаще используемые' },
]

/**
 * Owns the read-side state of the roles and permissions workspace: roles,
 * permissions, filters, presentation helpers and stale refresh protection.
 * Role/permission mutations and editor state deliberately stay outside this
 * read-side composable.
 */
export function useAdminRolesPermissionsData() {
  const roles = ref([])
  const permissions = ref([])
  const loading = ref(false)

  const notice = ref({
    type: 'info',
    message: '',
  })

  const roleSearch = ref('')
  const rolePermissionFilter = ref('all')
  const roleSortMode = ref('name-asc')

  const permissionSearch = ref('')
  const permissionUsageFilter = ref('all')
  const permissionSortMode = ref('name-asc')

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

  function normalize(value) {
    return String(value ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU')
  }

  function rolePermissions(role) {
    return Array.isArray(role?.permissions)
      ? role.permissions
      : []
  }

  function rolePermissionIds(role) {
    return rolePermissions(role)
      .map((permission) => Number(permission?.id))
      .filter(Number.isFinite)
  }


  function permissionUsageCount(permissionId) {
    const id = Number(permissionId)

    return roles.value.filter((role) =>
      rolePermissionIds(role).includes(id)
    ).length
  }

  const filteredRoles = computed(() => {
    const query = normalize(roleSearch.value)

    const result = roles.value.filter((role) => {
      const permissionCount = rolePermissionIds(role).length

      if (
        rolePermissionFilter.value === 'with' &&
        permissionCount === 0
      ) {
        return false
      }

      if (
        rolePermissionFilter.value === 'without' &&
        permissionCount > 0
      ) {
        return false
      }

      if (!query) {
        return true
      }

      const haystack = [
        role.name,
        role.description,
        ...rolePermissions(role).map(
          (permission) => permission?.name
        ),
      ]
        .filter(Boolean)
        .join(' ')
        .toLocaleLowerCase('ru-RU')

      return haystack.includes(query)
    })

    return [...result].sort((left, right) => {
      if (roleSortMode.value === 'permissions-desc') {
        const difference =
          rolePermissionIds(right).length -
          rolePermissionIds(left).length

        if (difference !== 0) {
          return difference
        }
      }

      const direction =
        roleSortMode.value === 'name-desc'
          ? -1
          : 1

      return direction * String(left.name ?? '')
        .localeCompare(
          String(right.name ?? ''),
          'ru'
        )
    })
  })

  const filteredPermissions = computed(() => {
    const query = normalize(permissionSearch.value)

    const result = permissions.value.filter((permission) => {
      const usageCount = permissionUsageCount(permission.id)

      if (
        permissionUsageFilter.value === 'used' &&
        usageCount === 0
      ) {
        return false
      }

      if (
        permissionUsageFilter.value === 'unused' &&
        usageCount > 0
      ) {
        return false
      }

      if (!query) {
        return true
      }

      return [
        permission.name,
        permission.description,
      ]
        .filter(Boolean)
        .join(' ')
        .toLocaleLowerCase('ru-RU')
        .includes(query)
    })

    return [...result].sort((left, right) => {
      if (permissionSortMode.value === 'usage-desc') {
        const difference =
          permissionUsageCount(right.id) -
          permissionUsageCount(left.id)

        if (difference !== 0) {
          return difference
        }
      }

      const direction =
        permissionSortMode.value === 'name-desc'
          ? -1
          : 1

      return direction * String(left.name ?? '')
        .localeCompare(
          String(right.name ?? ''),
          'ru'
        )
    })
  })

  const roleFiltersActive = computed(() => {
    return Boolean(roleSearch.value.trim()) ||
      rolePermissionFilter.value !== 'all' ||
      roleSortMode.value !== 'name-asc'
  })

  const permissionFiltersActive = computed(() => {
    return Boolean(permissionSearch.value.trim()) ||
      permissionUsageFilter.value !== 'all' ||
      permissionSortMode.value !== 'name-asc'
  })


  function resetRoleFilters() {
    roleSearch.value = ''
    rolePermissionFilter.value = 'all'
    roleSortMode.value = 'name-asc'
  }

  function resetPermissionFilters() {
    permissionSearch.value = ''
    permissionUsageFilter.value = 'all'
    permissionSortMode.value = 'name-asc'
  }

  async function loadData({
    preserveDrawer = true,
    selectedRoleId = null,
    onSelectedRoleRefreshed = () => {},
    onSelectedRoleMissing = () => {},
  } = {}) {
    const requestId = dataRequest.begin()
    loading.value = true
    clearNotice()

    const drawerRoleId = preserveDrawer
      ? selectedRoleId
      : null

    try {
      const [
        rolesResponse,
        permissionsResponse,
      ] = await Promise.all([
        rolesApi.getAll(),
        rolesApi.getPermissions(),
      ])

      const nextRoles = listFromResponse(rolesResponse)
      const nextPermissions = listFromResponse(
        permissionsResponse
      )

      if (!dataRequest.isCurrent(requestId)) {
        return false
      }

      roles.value = nextRoles
      permissions.value = nextPermissions

      if (drawerRoleId != null) {
        const refreshedRole = roles.value.find(
          (role) =>
            Number(role.id) === Number(drawerRoleId)
        )

        if (refreshedRole) {
          onSelectedRoleRefreshed(refreshedRole)
        } else {
          onSelectedRoleMissing()
        }
      }

      return true
    } catch (error) {
      if (!dataRequest.isCurrent(requestId)) {
        return false
      }

      showNotice(
        'danger',
        getApiErrorMessage(
          error,
          'Не удалось загрузить роли и права.'
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
    ROLE_PERMISSION_FILTER_OPTIONS,
    ROLE_SORT_OPTIONS,
    PERMISSION_USAGE_OPTIONS,
    PERMISSION_SORT_OPTIONS,
    roles,
    permissions,
    loading,
    notice,
    roleSearch,
    rolePermissionFilter,
    roleSortMode,
    permissionSearch,
    permissionUsageFilter,
    permissionSortMode,
    filteredRoles,
    filteredPermissions,
    roleFiltersActive,
    permissionFiltersActive,
    showNotice,
    clearNotice,
    normalize,
    rolePermissions,
    rolePermissionIds,
    permissionUsageCount,
    resetRoleFilters,
    resetPermissionFilters,
    loadData,
  }
}
