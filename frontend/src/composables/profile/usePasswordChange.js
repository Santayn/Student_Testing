import {
  computed,
  reactive,
  ref,
  watch,
} from 'vue'

function validatePassword(value, label) {
  if (!value) {
    return `Введите поле «${label}»`
  }

  if (value.length < 6) {
    return `${label} должен содержать минимум 6 символов`
  }

  if (value.length > 200) {
    return `${label} не должен превышать 200 символов`
  }

  return ''
}

export function usePasswordChange({
  authStore,
  router,
  redirectPath = '/profile',
}) {
  const passwordDialogOpen = ref(false)
  const passwordSubmitted = ref(false)
  const passwordTouched = reactive({
    current: false,
    next: false,
    confirm: false,
  })
  const passwordForm = reactive({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })

  const currentPasswordIssue = computed(() => {
    return validatePassword(passwordForm.currentPassword, 'Текущий пароль')
  })

  const newPasswordIssue = computed(() => {
    const base = validatePassword(passwordForm.newPassword, 'Новый пароль')

    if (base) {
      return base
    }

    if (
      passwordForm.currentPassword &&
      passwordForm.newPassword === passwordForm.currentPassword
    ) {
      return 'Новый пароль должен отличаться от текущего'
    }

    return ''
  })

  const confirmPasswordIssue = computed(() => {
    if (!passwordForm.confirmPassword) {
      return 'Повторите новый пароль'
    }

    if (passwordForm.confirmPassword !== passwordForm.newPassword) {
      return 'Пароли не совпадают'
    }

    return ''
  })

  const currentPasswordError = computed(() => {
    return passwordSubmitted.value || passwordTouched.current
      ? currentPasswordIssue.value
      : ''
  })

  const newPasswordError = computed(() => {
    return passwordSubmitted.value || passwordTouched.next
      ? newPasswordIssue.value
      : ''
  })

  const confirmPasswordError = computed(() => {
    return passwordSubmitted.value || passwordTouched.confirm
      ? confirmPasswordIssue.value
      : ''
  })

  const canChangePassword = computed(() => {
    return Boolean(
      !currentPasswordIssue.value &&
      !newPasswordIssue.value &&
      !confirmPasswordIssue.value &&
      !authStore.changingPassword
    )
  })

  watch(
    () => [
      passwordForm.currentPassword,
      passwordForm.newPassword,
      passwordForm.confirmPassword,
    ],
    () => {
      if (authStore.passwordError) {
        authStore.clearError('password')
      }
    }
  )

  function resetPasswordForm() {
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordSubmitted.value = false
    passwordTouched.current = false
    passwordTouched.next = false
    passwordTouched.confirm = false
    authStore.clearError('password')
  }

  function openPasswordDialog() {
    resetPasswordForm()
    passwordDialogOpen.value = true
  }

  function setPasswordDialogVisible(visible) {
    if (!visible && authStore.changingPassword) {
      return
    }

    passwordDialogOpen.value = visible

    if (!visible) {
      resetPasswordForm()
    }
  }

  async function submitPasswordChange() {
    passwordSubmitted.value = true
    passwordTouched.current = true
    passwordTouched.next = true
    passwordTouched.confirm = true

    if (!canChangePassword.value) {
      return
    }

    try {
      const result = await authStore.changePassword(
        passwordForm.currentPassword,
        passwordForm.newPassword
      )

      if (result?.requiresReauthentication) {
        passwordDialogOpen.value = false
        resetPasswordForm()

        await router.replace({
          name: 'login',
          query: {
            passwordChanged: '1',
            redirect: redirectPath,
          },
        })
      }
    } catch {
      // Password-specific message is stored in authStore.passwordError.
    }
  }

  return {
    passwordDialogOpen,
    passwordTouched,
    passwordForm,
    currentPasswordError,
    newPasswordError,
    confirmPasswordError,
    canChangePassword,
    openPasswordDialog,
    setPasswordDialogVisible,
    submitPasswordChange,
  }
}
