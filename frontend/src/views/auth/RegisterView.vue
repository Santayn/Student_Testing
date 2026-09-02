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
  UiInput,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

const route = useRoute()
const router = useRouter()

const authStore =
  useAuthStore()

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
    !authStore.loading
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

    const redirect =
      typeof route.query
        .redirect === 'string'
        ? route.query.redirect
        : '/'

    await router.replace(
      redirect
    )
  } catch {
    // Ошибка уже находится в authStore.error.
  }
}
</script>

<template>
  <div class="auth-view">
    <header class="auth-view__header">
      <h1>Регистрация</h1>

      <p>
        Создайте учётную запись.
        Привязку профиля выполняет администратор.
      </p>
    </header>

    <form
      class="auth-form"
      @submit.prevent="submit"
    >
      <UiInput
        v-model="form.login"
        label="Логин"
        autocomplete="username"
        maxlength="100"
        :disabled="authStore.loading"
        required
        size="lg"
      />

      <UiInput
        v-model="form.password"
        label="Пароль"
        type="password"
        minlength="6"
        maxlength="200"
        autocomplete="new-password"
        :disabled="authStore.loading"
        required
        size="lg"
      />

      <UiInput
        v-model="form.confirmPassword"
        label="Повторите пароль"
        type="password"
        minlength="6"
        maxlength="200"
        autocomplete="new-password"
        :disabled="authStore.loading"
        required
        size="lg"
      />

      <UiAlert
        v-if="
          localError ||
          authStore.error
        "
        variant="danger"
        :message="
          localError ||
          authStore.error
        "
      />

      <UiButton
        variant="primary"
        size="lg"
        type="submit"
        block
        :disabled="!canSubmit"
        :loading="authStore.loading"
        loading-text="Регистрация..."
      >
        Зарегистрироваться
      </UiButton>
    </form>

    <p class="auth-switch">
      Уже есть аккаунт?

      <RouterLink
        :to="{
          name: 'login',
        }"
      >
        Войти
      </RouterLink>
    </p>
  </div>
</template>

<style scoped>
.auth-view {
  display: grid;
  gap: 22px;
}

.auth-view__header {
  display: grid;
  gap: 7px;
}

.auth-view__header h1,
.auth-view__header p {
  margin: 0;
}

.auth-view__header h1 {
  color: var(--text);

  font-size: 28px;
}

.auth-view__header p,
.auth-switch {
  color: var(--text-secondary);

  font-size: 14px;
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
  color: var(--brand);

  font-weight: 600;
  text-decoration: none;
}
</style>
