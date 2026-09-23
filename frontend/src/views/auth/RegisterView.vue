<script setup>
import {
  computed,
  ref,
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
  hasWorkspaceAccess,
} from '@/utils/accountAccess'

import {
  useAuthStore,
} from '@/stores/auth'

const route = useRoute()
const router = useRouter()

const authStore =
  useAuthStore()

authStore.clearError('register')

const form = ref({
  login: '',
  password: '',
  confirmPassword: '',
})

const localError = ref('')

const canSubmit = computed(() => {
  return (
    form.value.login.trim() &&
    form.value.password &&
    form.value.confirmPassword &&
    !authStore.registering
  )
})

async function submit() {
  localError.value = ''

  if (!canSubmit.value) {
    localError.value =
      'Заполните обязательные поля'

    return
  }

  if (
    form.value.password.length < 6
  ) {
    localError.value =
      'Пароль должен содержать минимум 6 символов'

    return
  }

  if (
    form.value.password !==
    form.value.confirmPassword
  ) {
    localError.value =
      'Пароли не совпадают'

    return
  }

  try {
    await authStore.register({
      login:
        form.value.login.trim(),

      password:
        form.value.password,
    })

    if (
      !hasWorkspaceAccess(authStore)
    ) {
      await router.replace({
        name: 'account-pending',
      })

      return
    }

    const redirect =
      typeof route.query
        .redirect === 'string'
        ? route.query.redirect
        : '/'

    await router.replace(
      redirect
    )
  } catch {
    // Ошибка относится только к registration-flow и находится в authStore.registerError.
  }
}
</script>

<template>
  <section class="auth-page" aria-labelledby="register-title">
    <div class="auth-page__brand" aria-hidden="true">
      Student Testing
    </div>

    <UiCard class="auth-card">
      <header class="auth-card__header">
        <p class="auth-card__eyebrow">Учётная запись</p>

        <h1 id="register-title">Регистрация</h1>

        <p>
          Создайте учётную запись. Привязку профиля выполняет администратор.
        </p>
      </header>

      <form class="auth-form" @submit.prevent="submit">
        <UiInput
          v-model="form.login"
          label="Логин"
          autocomplete="username"
          maxlength="100"
          :disabled="authStore.registering"
          required
        />

        <UiInput
          v-model="form.password"
          label="Пароль"
          type="password"
          minlength="6"
          maxlength="200"
          autocomplete="new-password"
          :disabled="authStore.registering"
          required
        />

        <UiInput
          v-model="form.confirmPassword"
          label="Повторите пароль"
          type="password"
          minlength="6"
          maxlength="200"
          autocomplete="new-password"
          :disabled="authStore.registering"
          required
        />

        <UiAlert
          v-if="localError || authStore.registerError"
          variant="danger"
          :message="localError || authStore.registerError"
        />

        <UiButton
          variant="primary"
          size="lg"
          type="submit"
          block
          :disabled="!canSubmit"
          :loading="authStore.registering"
          loading-text="Регистрация..."
        >
          Зарегистрироваться
        </UiButton>
      </form>

      <p class="auth-switch">
        Уже есть аккаунт?

        <RouterLink :to="{ name: 'login' }">
          Войти
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

.auth-form {
  display: grid;
  gap: 16px;
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
