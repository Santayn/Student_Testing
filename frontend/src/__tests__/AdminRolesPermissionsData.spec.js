import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const apiMocks = vi.hoisted(() => ({
  getAll: vi.fn(),
  getPermissions: vi.fn(),
  getApiErrorMessage: vi.fn(
    (error, fallback) => error?.message || fallback
  ),
}))

vi.mock('@/api', () => ({
  rolesApi: {
    getAll: apiMocks.getAll,
    getPermissions: apiMocks.getPermissions,
  },
  getApiErrorMessage: apiMocks.getApiErrorMessage,
}))

import {
  useAdminRolesPermissionsData,
} from '@/composables/useAdminRolesPermissionsData'

function deferred() {
  let resolve
  let reject

  const promise = new Promise((res, rej) => {
    resolve = res
    reject = rej
  })

  return {
    promise,
    resolve,
    reject,
  }
}

const ROLE_ADMIN = {
  id: 1,
  name: 'Администратор',
  description: 'Полный доступ',
  permissions: [
    { id: 10, name: 'users.read' },
    { id: 20, name: 'users.write' },
  ],
}

const ROLE_TEACHER = {
  id: 2,
  name: 'Преподаватель',
  description: 'Работа с курсами',
  permissions: [
    { id: 10, name: 'users.read' },
  ],
}

const ROLE_EMPTY = {
  id: 3,
  name: 'Новая роль',
  description: '',
  permissions: [],
}

const PERMISSIONS = [
  { id: 10, name: 'users.read', description: 'Чтение пользователей' },
  { id: 20, name: 'users.write', description: 'Изменение пользователей' },
  { id: 30, name: 'audit.read', description: 'Чтение аудита' },
]

describe('admin roles and permissions data state', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    apiMocks.getAll.mockResolvedValue({ data: [] })
    apiMocks.getPermissions.mockResolvedValue({ data: [] })
  })

  it('loads roles and permissions and derives usage counts', async () => {
    apiMocks.getAll.mockResolvedValue({
      data: [ROLE_ADMIN, ROLE_TEACHER, ROLE_EMPTY],
    })
    apiMocks.getPermissions.mockResolvedValue({
      data: PERMISSIONS,
    })

    const state = useAdminRolesPermissionsData()
    await state.loadData()

    expect(state.roles.value).toHaveLength(3)
    expect(state.permissions.value).toHaveLength(3)
    expect(state.permissionUsageCount(10)).toBe(2)
    expect(state.permissionUsageCount(20)).toBe(1)
    expect(state.permissionUsageCount(30)).toBe(0)
    expect(state.loading.value).toBe(false)
  })

  it('filters and sorts roles locally without issuing new API reads', async () => {
    apiMocks.getAll.mockResolvedValue({
      data: [ROLE_ADMIN, ROLE_TEACHER, ROLE_EMPTY],
    })
    apiMocks.getPermissions.mockResolvedValue({
      data: PERMISSIONS,
    })

    const state = useAdminRolesPermissionsData()
    await state.loadData()

    state.rolePermissionFilter.value = 'with'
    state.roleSearch.value = 'users.read'
    state.roleSortMode.value = 'permissions-desc'

    expect(state.filteredRoles.value.map((role) => role.id)).toEqual([1, 2])
    expect(state.roleFiltersActive.value).toBe(true)
    expect(apiMocks.getAll).toHaveBeenCalledTimes(1)
    expect(apiMocks.getPermissions).toHaveBeenCalledTimes(1)

    state.resetRoleFilters()

    expect(state.roleFiltersActive.value).toBe(false)
    expect(state.filteredRoles.value.map((role) => role.id)).toEqual([1, 3, 2])
  })

  it('filters permissions by usage and search and sorts by usage count', async () => {
    apiMocks.getAll.mockResolvedValue({
      data: [ROLE_ADMIN, ROLE_TEACHER, ROLE_EMPTY],
    })
    apiMocks.getPermissions.mockResolvedValue({
      data: PERMISSIONS,
    })

    const state = useAdminRolesPermissionsData()
    await state.loadData()

    state.permissionUsageFilter.value = 'used'
    state.permissionSearch.value = 'users'
    state.permissionSortMode.value = 'usage-desc'

    expect(
      state.filteredPermissions.value.map((permission) => permission.id)
    ).toEqual([10, 20])

    state.resetPermissionFilters()

    expect(state.permissionFiltersActive.value).toBe(false)
    expect(
      state.filteredPermissions.value.map((permission) => permission.id)
    ).toEqual([30, 10, 20])
  })

  it('refreshes an externally owned selected role through callbacks', async () => {
    const onSelectedRoleRefreshed = vi.fn()
    const onSelectedRoleMissing = vi.fn()

    apiMocks.getAll.mockResolvedValue({
      data: [ROLE_ADMIN],
    })
    apiMocks.getPermissions.mockResolvedValue({
      data: PERMISSIONS,
    })

    const state = useAdminRolesPermissionsData()

    await state.loadData({
      selectedRoleId: 1,
      onSelectedRoleRefreshed,
      onSelectedRoleMissing,
    })

    expect(onSelectedRoleRefreshed).toHaveBeenCalledWith(ROLE_ADMIN)
    expect(onSelectedRoleMissing).not.toHaveBeenCalled()

    apiMocks.getAll.mockResolvedValue({ data: [] })
    await state.loadData({
      selectedRoleId: 1,
      onSelectedRoleRefreshed,
      onSelectedRoleMissing,
    })

    expect(onSelectedRoleMissing).toHaveBeenCalledTimes(1)
  })

  it('ignores stale refresh results when a newer request finishes first', async () => {
    const rolesOld = deferred()
    const permissionsOld = deferred()
    const rolesNew = deferred()
    const permissionsNew = deferred()

    apiMocks.getAll
      .mockReturnValueOnce(rolesOld.promise)
      .mockReturnValueOnce(rolesNew.promise)
    apiMocks.getPermissions
      .mockReturnValueOnce(permissionsOld.promise)
      .mockReturnValueOnce(permissionsNew.promise)

    const state = useAdminRolesPermissionsData()
    const oldLoad = state.loadData()
    const newLoad = state.loadData()

    rolesNew.resolve({ data: [ROLE_TEACHER] })
    permissionsNew.resolve({ data: [PERMISSIONS[2]] })
    await newLoad

    rolesOld.resolve({ data: [ROLE_ADMIN] })
    permissionsOld.resolve({ data: [PERMISSIONS[0]] })
    await oldLoad

    expect(state.roles.value.map((role) => role.id)).toEqual([2])
    expect(state.permissions.value.map((permission) => permission.id)).toEqual([30])
    expect(state.loading.value).toBe(false)
  })

  it('reports only the current load failure through the existing notice state', async () => {
    apiMocks.getAll.mockRejectedValue(new Error('Сеть недоступна'))

    const state = useAdminRolesPermissionsData()
    await state.loadData()

    expect(state.notice.value).toEqual({
      type: 'danger',
      message: 'Сеть недоступна',
    })
    expect(apiMocks.getApiErrorMessage).toHaveBeenCalled()
    expect(state.loading.value).toBe(false)
  })
})
