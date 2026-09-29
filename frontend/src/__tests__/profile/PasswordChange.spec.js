import {
  nextTick,
} from 'vue'

import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest'

import {
  usePasswordChange,
} from '@/composables/profile/usePasswordChange'

function createState() {
  const authStore = {
    changingPassword: false,
    passwordError: '',
    clearError: vi.fn(),
    changePassword: vi.fn(),
  }

  const router = {
    replace: vi.fn(),
  }

  return {
    authStore,
    router,
    state: usePasswordChange({
      authStore,
      router,
    }),
  }
}

describe('password change flow', () => {
  let context

  beforeEach(() => {
    context = createState()
  })

  it('validates password fields before calling the auth store', async () => {
    const { state, authStore } = context

    state.openPasswordDialog()
    state.passwordForm.currentPassword = 'secret1'
    state.passwordForm.newPassword = 'secret1'
    state.passwordForm.confirmPassword = 'secret1'

    await state.submitPasswordChange()

    expect(state.newPasswordError.value)
      .toBe('Новый пароль должен отличаться от текущего')
    expect(authStore.changePassword).not.toHaveBeenCalled()
  })

  it('changes password and redirects when reauthentication is required', async () => {
    const { state, authStore, router } = context

    authStore.changePassword.mockResolvedValue({
      requiresReauthentication: true,
    })

    state.openPasswordDialog()
    state.passwordForm.currentPassword = 'secret1'
    state.passwordForm.newPassword = 'secret2'
    state.passwordForm.confirmPassword = 'secret2'

    await state.submitPasswordChange()

    expect(authStore.changePassword)
      .toHaveBeenCalledWith('secret1', 'secret2')
    expect(state.passwordDialogOpen.value).toBe(false)
    expect(router.replace).toHaveBeenCalledWith({
      name: 'login',
      query: {
        passwordChanged: '1',
        redirect: '/profile',
      },
    })
  })

  it('keeps the dialog open while a password change is in progress', () => {
    const { state, authStore } = context

    state.openPasswordDialog()
    authStore.changingPassword = true

    state.setPasswordDialogVisible(false)

    expect(state.passwordDialogOpen.value).toBe(true)
  })

  it('clears password-specific errors after the user edits the form', async () => {
    const { state, authStore } = context

    authStore.passwordError = 'Неверный текущий пароль'
    state.passwordForm.currentPassword = 'secret1'
    await nextTick()

    expect(authStore.clearError).toHaveBeenCalledWith('password')
  })
})
