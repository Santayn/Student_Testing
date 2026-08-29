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

const login = ref('')
const password = ref('')
const showPassword = ref(false)

const canSubmit = computed(() => {
  return (
    login.value.trim().length > 0 &&
    password.value.length > 0 &&
    !authStore.loading
  )
})

async function submit() {
  if (!canSubmit.value) {
    return
  }

  try {
    await authStore.login(
      login.value.trim(),
      password.value
    )

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
      <h1>Вход</h1>

      <p>
        Войдите в свою учётную запись.
      </p>
    </header>

    <form
      class="auth-form"
      @submit.prevent="submit"
    >
      <UiInput
        v-model="login"
        label="Логин"
        autocomplete="username"
        placeholder="Введите логин"
        :disabled="authStore.loading"
        required
        size="lg"
      />

      <div class="auth-password">
        <UiInput
          v-model="password"
          label="Пароль"
          :type="
            showPassword
              ? 'text'
              : 'password'
          "
          autocomplete="current-password"
          placeholder="Введите пароль"
          :disabled="authStore.loading"
          required
          size="lg"
        />

        <UiButton
          class="auth-password__toggle"
          variant="ghost"
          size="sm"
          type="button"
          @click="
            showPassword =
              !showPassword
          "
        >
          {{
            showPassword
              ? 'Скрыть пароль'
              : 'Показать пароль'
          }}
        </UiButton>
      </div>

      <UiAlert
        v-if="authStore.error"
        variant="danger"
        :message="authStore.error"
      />

      <UiButton
        variant="primary"
        size="lg"
        type="submit"
        block
        :disabled="!canSubmit"
        :loading="authStore.loading"
        loading-text="Вход..."
      >
        Войти
      </UiButton>
    </form>

    <p class="auth-switch">
      Нет аккаунта?

      <RouterLink
        :to="{
          name: 'register',
        }"
      >
        Зарегистрироваться
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

.auth-password {
  display: grid;
  gap: 5px;
}

.auth-password__toggle {
  justify-self: end;
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
