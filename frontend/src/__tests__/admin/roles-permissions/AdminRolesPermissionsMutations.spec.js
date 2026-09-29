import {
  ref,
} from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

const mocks = vi.hoisted(() => ({
  createRole: vi.fn(),
  createPermission: vi.fn(),
  setPermissions: vi.fn(),
  getApiErrorMessage: vi.fn((error, fallback) =>
    error?.message || fallback
  ),
}))

vi.mock('@/api', () => ({
  getApiErrorMessage: mocks.getApiErrorMessage,
  rolesApi: {
    createRole: mocks.createRole,
    createPermission: mocks.createPermission,
    setPermissions: mocks.setPermissions,
  },
}))

import {
  useAdminRolesPermissionsMutations,
} from '@/composables/admin/roles-permissions/useAdminRolesPermissionsMutations'

function overlay(form) {
  return {
    form,
    saving: ref(false),
    openCreate: vi.fn(),
    beginSaving: vi.fn(function beginSaving() {
      this.saving.value = true
    }),
    finishSaving: vi.fn(function finishSaving() {
      this.saving.value = false
    }),
    failSaving: vi.fn(function failSaving() {
      this.saving.value = false
    }),
  }
}

function setup() {
  const roles = ref([
    {
      id: 1,
      name: 'ADMIN',
      permissions: [],
    },
  ])
  const permissions = ref([
    {
      id: 5,
      name: 'READ_USERS',
    },
  ])

  const roleCreateOverlay = overlay({
    name: '',
    description: '',
  })
  const permissionCreateOverlay = overlay({
    name: '',
    description: '',
  })
  const rolePermissionsOverlay = overlay({
    permissionIds: [],
  })
  const selectedRoleId = ref(null)
  const rolePermissionsError = ref('')
  const showNotice = vi.fn()
  const loadData = vi.fn()

  const state = useAdminRolesPermissionsMutations({
    roles,
    permissions,
    normalize: (value) => String(value ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU'),
    showNotice,
    loadData,
    roleCreateOverlay,
    permissionCreateOverlay,
    rolePermissionsOverlay,
    selectedRoleId,
    rolePermissionIds: (role) =>
      Array.isArray(role?.permissions)
        ? role.permissions.map((item) => Number(item.id))
        : [],
    rolePermissionsError,
  })

  return {
    state,
    roles,
    permissions,
    roleCreateOverlay,
    permissionCreateOverlay,
    rolePermissionsOverlay,
    selectedRoleId,
    rolePermissionsError,
    showNotice,
    loadData,
  }
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('admin roles and permissions mutations', () => {
  it('creates a trimmed role and appends the backend entity', async () => {
    const context = setup()
    context.roleCreateOverlay.form.name = '  CURATOR  '
    context.roleCreateOverlay.form.description = '  Curator role  '

    mocks.createRole.mockResolvedValue({
      data: {
        id: 2,
        name: 'CURATOR',
        description: 'Curator role',
        permissions: [],
      },
    })

    await expect(context.state.createRole()).resolves.toBe(true)

    expect(mocks.createRole).toHaveBeenCalledWith({
      name: 'CURATOR',
      description: 'Curator role',
    })
    expect(context.roles.value.map((role) => role.id)).toEqual([1, 2])
    expect(context.roleCreateOverlay.finishSaving).toHaveBeenCalledTimes(1)
    expect(context.showNotice).toHaveBeenCalledWith(
      'success',
      'Роль «CURATOR» создана.'
    )
  })

  it('blocks duplicate role names before calling the backend', async () => {
    const context = setup()
    context.roleCreateOverlay.form.name = ' admin '

    await expect(context.state.createRole()).resolves.toBe(false)

    expect(context.state.roleFormError.value).toBe(
      'Роль с таким названием уже существует.'
    )
    expect(mocks.createRole).not.toHaveBeenCalled()
    expect(context.roleCreateOverlay.beginSaving).not.toHaveBeenCalled()
  })

  it('creates a permission and keeps backend-supported fields only', async () => {
    const context = setup()
    context.permissionCreateOverlay.form.name = ' WRITE_USERS '
    context.permissionCreateOverlay.form.description = ' '

    mocks.createPermission.mockResolvedValue({
      data: {
        id: 6,
        name: 'WRITE_USERS',
        description: null,
      },
    })

    await expect(context.state.createPermission()).resolves.toBe(true)

    expect(mocks.createPermission).toHaveBeenCalledWith({
      name: 'WRITE_USERS',
      description: null,
    })
    expect(context.permissions.value.map((item) => item.id)).toEqual([5, 6])
  })

  it('deduplicates permission ids and replaces the refreshed role', async () => {
    const context = setup()
    context.selectedRoleId.value = 1
    context.rolePermissionsOverlay.form.permissionIds = [5, '5', 8, 'bad']

    const updatedRole = {
      id: 1,
      name: 'ADMIN',
      permissions: [
        { id: 5, name: 'READ_USERS' },
        { id: 8, name: 'WRITE_USERS' },
      ],
    }

    mocks.setPermissions.mockResolvedValue({ data: updatedRole })

    await expect(context.state.saveRolePermissions()).resolves.toBe(true)

    expect(mocks.setPermissions).toHaveBeenCalledWith(1, [5, 8])
    expect(context.roles.value[0]).toEqual(updatedRole)
    expect(context.rolePermissionsOverlay.finishSaving).toHaveBeenCalledWith({
      values: {
        permissionIds: [5, 8],
      },
    })
    expect(context.selectedRoleId.value).toBeNull()
    expect(context.showNotice).toHaveBeenCalledWith(
      'success',
      'Права роли «ADMIN» сохранены.'
    )
  })
})
