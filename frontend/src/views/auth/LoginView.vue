<script setup>
import {
  computed,
  ref,
  watch,
} from 'vue'

import {
  useRoute,
  useRouter,
} from 'vue-router'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiInput,
} from '@/components/ui'

import {
  publicRegistrationEnabled,
} from '@/config/features'

import {
  hasWorkspaceAccess,
} from '@/utils/accountAccess'

import {
  useAuthStore,
} from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

authStore.clearError('login')

const login = ref('')
const password = ref('')
const showPassword = ref(false)
const submitted = ref(false)
const loginTouched = ref(false)
const passwordTouched = ref(false)

const registrationDisabledNotice = computed(() => {
  return route.query.registration === 'disabled'
})

const passwordChangedNotice = computed(() => {
  return route.query.passwordChanged === '1'
})

const loginValidationError = computed(() => {
  if (!submitted.value && !loginTouched.value) {
    return ''
  }

  const value = login.value.trim()

  if (!value) {
    return 'Введите логин'
  }

  if (value.length > 100) {
    return 'Логин не должен превышать 100 символов'
  }

  return ''
})

const passwordValidationError = computed(() => {
  if (!submitted.value && !passwordTouched.value) {
    return ''
  }

  if (!password.value) {
    return 'Введите пароль'
  }

  if (password.value.length < 6) {
    return 'Пароль должен содержать минимум 6 символов'
  }

  if (password.value.length > 200) {
    return 'Пароль не должен превышать 200 символов'
  }

  return ''
})

const formValid = computed(() => {
  const loginLength = login.value.trim().length
  const passwordLength = password.value.length

  return (
    loginLength > 0 &&
    loginLength <= 100 &&
    passwordLength >= 6 &&
    passwordLength <= 200
  )
})

const canSubmit = computed(() => {
  return formValid.value && !authStore.loggingIn
})

watch(
  [login, password],
  () => {
    if (authStore.loginError) {
      authStore.clearError('login')
    }
  }
)

async function submit() {
  submitted.value = true
  loginTouched.value = true
  passwordTouched.value = true

  if (!formValid.value || authStore.loggingIn) {
    return
  }

  try {
    await authStore.login(
      login.value.trim(),
      password.value
    )

    if (!hasWorkspaceAccess(authStore)) {
      await router.replace({
        name: 'account-pending',
      })

      return
    }

    const redirect =
      typeof route.query.redirect === 'string'
        ? route.query.redirect
        : '/'

    await router.replace(redirect)
  } catch {
    // Login-specific message is stored in authStore.loginError.
  }
}
</script>

<template>
  <section class="auth-page" aria-labelledby="login-title">
    <div class="auth-page__brand" aria-hidden="true">
      Student Testing
    </div>

    <UiCard class="auth-card">
      <header class="auth-card__header">
        <p class="auth-card__eyebrow">
          Учётная запись
        </p>

        <h1 id="login-title">
          Вход
        </h1>

        <p>
          Войдите, чтобы продолжить работу в системе.
        </p>
      </header>

      <div class="auth-notices">
        <UiAlert
          v-if="passwordChangedNotice"
          variant="success"
          message="Пароль изменён. Войдите снова с новым паролем."
        />

        <UiAlert
          v-if="registrationDisabledNotice"
          variant="info"
          message="Самостоятельная регистрация отключена. Для создания учётной записи обратитесь к администратору."
        />
      </div>

      <form
        class="auth-form"
        @submit.prevent="submit"
      >
        <UiInput
          v-model="login"
          label="Логин"
          autocomplete="username"
          maxlength="100"
          placeholder="Введите логин"
          :error="loginValidationError"
          :disabled="authStore.loggingIn"
          required
          @blur="loginTouched = true"
        />

        <div class="auth-password-field">
          <UiInput
            v-model="password"
            label="Пароль"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            minlength="6"
            maxlength="200"
            placeholder="Введите пароль"
            :error="passwordValidationError"
            :disabled="authStore.loggingIn"
            required
            @blur="passwordTouched = true"
          />

          <button
            class="auth-password-toggle"
            type="button"
            :aria-label="showPassword ? 'Скрыть пароль' : 'Показать пароль'"
            :aria-pressed="showPassword"
            :disabled="authStore.loggingIn"
            @click="showPassword = !showPassword"
          >
            <i
              :class="showPassword ? 'pi pi-eye-slash' : 'pi pi-eye'"
              aria-hidden="true"
            />
          </button>
        </div>

        <UiAlert
          v-if="authStore.loginError"
          variant="danger"
          :message="authStore.loginError"
        />

        <UiButton
          variant="primary"
          size="lg"
          type="submit"
          block
          :disabled="!canSubmit"
          :loading="authStore.loggingIn"
          loading-text="Вход..."
        >
          Войти
        </UiButton>
      </form>

      <p
        v-if="publicRegistrationEnabled"
        class="auth-switch"
      >
        Нет аккаунта?

        <RouterLink :to="{ name: 'register' }">
          Зарегистрироваться
        </RouterLink>
      </p>
    </UiCard>
  </section>
</template>

<style scoped>
.auth-page {
  width: min(100%, 480px);
  margin: clamp(28px, 7vh, 72px) auto;
  display: grid;
  gap: 14px;
}

.auth-page__brand {
  color: var(--st-text-secondary);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-align: center;
  text-transform: uppercase;
}

.auth-card :deep(.p-card-body) {
  padding: clamp(20px, 4vw, 30px);
}

.auth-card :deep(.p-card-content) {
  display: grid;
  gap: 22px;
}

.auth-card__header {
  display: grid;
  gap: 7px;
}

.auth-card__header h1,
.auth-card__header p {
  margin: 0;
}

.auth-card__eyebrow {
  color: var(--st-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-card__header h1 {
  color: var(--st-text);
  font-size: clamp(28px, 6vw, 34px);
  line-height: 1.15;
}

.auth-card__header p:not(.auth-card__eyebrow),
.auth-switch {
  color: var(--st-text-secondary);
  font-size: 14px;
  line-height: 1.55;
}

.auth-notices,
.auth-form {
  display: grid;
  gap: 16px;
}

.auth-notices:empty {
  display: none;
}

.auth-password-field {
  position: relative;
}

.auth-password-field :deep(.p-inputtext) {
  padding-right: 52px;
}

.auth-password-toggle {
  position: absolute;
  right: 4px;
  top: 25px;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  color: var(--st-text-secondary);
  background: transparent;
  border: 0;
  border-radius: var(--st-radius-control);
  cursor: pointer;
}

.auth-password-toggle:hover:not(:disabled) {
  color: var(--st-text);
  background: var(--st-surface-muted);
}

.auth-password-toggle:focus-visible {
  outline: none;
  box-shadow: var(--st-focus-shadow);
}

.auth-password-toggle:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.auth-switch {
  margin: 0;
  text-align: center;
}

.auth-switch a {
  color: var(--st-primary);
  font-weight: 700;
  text-decoration: none;
}

.auth-switch a:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .auth-page {
    width: 100%;
    margin: 12px auto 24px;
  }

  .auth-card :deep(.p-card-body) {
    padding: 20px 18px;
  }
}
</style>
