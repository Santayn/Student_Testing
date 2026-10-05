import {
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  rolesApi,
} from '@/api'

/**
 * Owns all write-side operations for the roles/permissions workspace while
 * keeping read-side state and drawer lifecycle in their dedicated composables.
 */
export function useAdminRolesPermissionsMutations({
  roles,
  permissions,
  normalize,
  showNotice,
  loadData,
  roleCreateOverlay,
  permissionCreateOverlay,
  rolePermissionsOverlay,
  selectedRoleId,
  rolePermissionIds,
  rolePermissionsError,
}) {
  const roleFormError = ref('')
  const permissionFormError = ref('')

  function openCreateRole() {
    roleFormError.value = ''
    roleCreateOverlay.openCreate()
  }

  function openCreatePermission() {
    permissionFormError.value = ''
    permissionCreateOverlay.openCreate()
  }

  function validateRoleForm() {
    const name = String(
      roleCreateOverlay.form.name ?? ''
    ).trim()
    const description = String(
      roleCreateOverlay.form.description ?? ''
    ).trim()

    if (!name) {
      return 'Введите название роли.'
    }

    if (name.length > 100) {
      return 'Название роли не должно превышать 100 символов.'
    }

    if (description.length > 500) {
      return 'Описание роли не должно превышать 500 символов.'
    }

    const duplicate = roles.value.some(
      (role) => normalize(role.name) === normalize(name)
    )

    if (duplicate) {
      return 'Роль с таким названием уже существует.'
    }

    return ''
  }

  function validatePermissionForm() {
    const name = String(
      permissionCreateOverlay.form.name ?? ''
    ).trim()
    const description = String(
      permissionCreateOverlay.form.description ?? ''
    ).trim()

    if (!name) {
      return 'Введите название permission.'
    }

    if (name.length > 100) {
      return 'Название permission не должно превышать 100 символов.'
    }

    if (description.length > 500) {
      return 'Описание permission не должно превышать 500 символов.'
    }

    const duplicate = permissions.value.some(
      (permission) =>
        normalize(permission.name) === normalize(name)
    )

    if (duplicate) {
      return 'Permission с таким названием уже существует.'
    }

    return ''
  }

  async function createRole() {
    if (roleCreateOverlay.saving.value) {
      return false
    }

    const validationError = validateRoleForm()

    if (validationError) {
      roleFormError.value = validationError
      return false
    }

    roleFormError.value = ''
    roleCreateOverlay.beginSaving()

    try {
      const response = await rolesApi.createRole({
        name: String(
          roleCreateOverlay.form.name
        ).trim(),
        description:
          String(
            roleCreateOverlay.form.description ?? ''
          ).trim() || null,
      })

      const createdRole = response?.data

      if (createdRole?.id != null) {
        roles.value = [
          ...roles.value,
          createdRole,
        ]
      } else {
        await loadData({ preserveDrawer: false })
      }

      roleCreateOverlay.finishSaving()

      showNotice(
        'success',
        createdRole?.name
          ? `Роль «${createdRole.name}» создана.`
          : 'Роль создана.'
      )

      return true
    } catch (error) {
      roleCreateOverlay.failSaving()
      roleFormError.value = getApiErrorMessage(
        error,
        'Не удалось создать роль.'
      )
      return false
    }
  }

  async function createPermission() {
    if (permissionCreateOverlay.saving.value) {
      return false
    }

    const validationError = validatePermissionForm()

    if (validationError) {
      permissionFormError.value = validationError
      return false
    }

    permissionFormError.value = ''
    permissionCreateOverlay.beginSaving()

    try {
      const response = await rolesApi.createPermission({
        name: String(
          permissionCreateOverlay.form.name
        ).trim(),
        description:
          String(
            permissionCreateOverlay.form.description ?? ''
          ).trim() || null,
      })

      const createdPermission = response?.data

      if (createdPermission?.id != null) {
        permissions.value = [
          ...permissions.value,
          createdPermission,
        ]
      } else {
        await loadData()
      }

      permissionCreateOverlay.finishSaving()

      showNotice(
        'success',
        createdPermission?.name
          ? `Permission «${createdPermission.name}» создан.`
          : 'Permission создан.'
      )

      return true
    } catch (error) {
      permissionCreateOverlay.failSaving()
      permissionFormError.value = getApiErrorMessage(
        error,
        'Не удалось создать permission.'
      )
      return false
    }
  }

  async function saveRolePermissions() {
    if (
      rolePermissionsOverlay.saving.value ||
      selectedRoleId.value == null
    ) {
      return false
    }

    rolePermissionsError.value = ''
    rolePermissionsOverlay.beginSaving()

    const permissionIds = [
      ...new Set(
        (rolePermissionsOverlay.form.permissionIds ?? [])
          .map(Number)
          .filter(Number.isFinite)
      ),
    ]

    try {
      const response = await rolesApi.setPermissions(
        selectedRoleId.value,
        permissionIds
      )

      const updatedRole = response?.data

      roles.value = roles.value.map((role) =>
        Number(role.id) === Number(selectedRoleId.value)
          ? updatedRole ?? role
          : role
      )

      rolePermissionsOverlay.finishSaving({
        values: {
          permissionIds:
            rolePermissionIds(updatedRole),
        },
      })

      selectedRoleId.value = null

      showNotice(
        'success',
        updatedRole?.name
          ? `Права роли «${updatedRole.name}» сохранены.`
          : 'Права роли сохранены.'
      )

      return true
    } catch (error) {
      rolePermissionsOverlay.failSaving()
      rolePermissionsError.value = getApiErrorMessage(
        error,
        'Не удалось сохранить права роли.'
      )
      return false
    }
  }

  return {
    roleFormError,
    permissionFormError,
    openCreateRole,
    openCreatePermission,
    validateRoleForm,
    validatePermissionForm,
    createRole,
    createPermission,
    saveRolePermissions,
  }
}
