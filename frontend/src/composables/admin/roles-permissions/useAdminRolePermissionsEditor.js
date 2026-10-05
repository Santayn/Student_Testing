import {
  computed,
  ref,
} from 'vue'

import {
  useOverlayForm,
} from '@/components/ui'

/**
 * Owns the role-permissions editor lifecycle while leaving persistence to the
 * parent view. The selected role, draft permission ids, dirty state, search and
 * close-confirmation flow live here.
 */
export function useAdminRolePermissionsEditor({
  roles,
  permissions,
  rolePermissionIds,
}) {
  const rolePermissionsError = ref('')
  const permissionDrawerSearch = ref('')
  const selectedRoleId = ref(null)

  const rolePermissionsOverlay = useOverlayForm({
    createDefault: () => ({
      permissionIds: [],
    }),
    mapEntity: (role) => ({
      permissionIds: rolePermissionIds(role),
    }),
  })

  const drawerRole = computed(() => {
    return roles.value.find(
      (role) =>
        Number(role.id) === Number(selectedRoleId.value)
    ) ?? null
  })

  const drawerPermissions = computed(() => {
    const query = String(permissionDrawerSearch.value ?? '')
      .trim()
      .toLocaleLowerCase('ru-RU')

    return [...permissions.value]
      .filter((permission) => {
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
      .sort((left, right) =>
        String(left.name ?? '').localeCompare(
          String(right.name ?? ''),
          'ru'
        )
      )
  })

  function openRolePermissions(role) {
    rolePermissionsError.value = ''
    permissionDrawerSearch.value = ''
    selectedRoleId.value = Number(role.id)
    rolePermissionsOverlay.openEdit(role)
  }

  function onSelectedRoleRefreshed(role) {
    rolePermissionsOverlay.markClean({
      permissionIds: rolePermissionIds(role),
    })
  }

  function onSelectedRoleMissing() {
    selectedRoleId.value = null
    rolePermissionsError.value = ''
    permissionDrawerSearch.value = ''
    rolePermissionsOverlay.closeImmediately()
  }

  function requestRolePermissionsClose() {
    const closed = rolePermissionsOverlay.requestClose()

    if (closed) {
      selectedRoleId.value = null
      rolePermissionsError.value = ''
      permissionDrawerSearch.value = ''
    }

    return closed
  }

  function discardRolePermissionsAndClose() {
    rolePermissionsOverlay.discardAndClose()
    selectedRoleId.value = null
    rolePermissionsError.value = ''
    permissionDrawerSearch.value = ''
  }

  return {
    rolePermissionsError,
    permissionDrawerSearch,
    selectedRoleId,
    rolePermissionsOverlay,
    drawerRole,
    drawerPermissions,
    openRolePermissions,
    onSelectedRoleRefreshed,
    onSelectedRoleMissing,
    requestRolePermissionsClose,
    discardRolePermissionsAndClose,
  }
}
