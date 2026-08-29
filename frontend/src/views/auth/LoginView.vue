<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

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
      typeof route.query.redirect === 'string'
        ? route.query.redirect
        : '/'

    await router.replace(redirect)
  } catch {
    // Сообщение уже находится в authStore.error
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
      <label class="auth-field">
        <span>Логин</span>

        <input
          v-model="login"
          type="text"
          autocomplete="username"
          placeholder="Введите логин"
          :disabled="authStore.loading"
        >
      </label>

      <label class="auth-field">
        <span>Пароль</span>

        <div class="password-field">
          <input
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="Введите пароль"
            :disabled="authStore.loading"
          >

          <button
            class="password-field__toggle"
            type="button"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? 'Скрыть' : 'Показать' }}
          </button>
        </div>
      </label>

      <div
        v-if="authStore.error"
        class="auth-alert"
        role="alert"
      >
        {{ authStore.error }}
      </div>

      <button
        class="auth-submit"
        type="submit"
        :disabled="!canSubmit"
      >
        {{ authStore.loading ? 'Вход...' : 'Войти' }}
      </button>
    </form>

    <p class="auth-switch">
      Нет аккаунта?

      <RouterLink :to="{ name: 'register' }">
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
  color:
    var(--text, #111827);

  font-size: 28px;
}

.auth-view__header p,
.auth-switch {
  color:
    var(--text-secondary, #6b7280);

  font-size: 14px;
}

.auth-form {
  display: grid;
  gap: 16px;
}

.auth-field {
  display: grid;
  gap: 7px;

  color:
    var(--text, #111827);

  font-size: 14px;
  font-weight: 600;
}

.auth-field input {
  width: 100%;
  min-height: 44px;

  padding: 9px 11px;

  color:
    var(--text, #111827);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #d1d5db);

  border-radius: 9px;

  font: inherit;
  font-weight: 400;
}

.auth-field input:focus {
  outline: 2px solid
    color-mix(in srgb, var(--brand, #2563eb) 28%, transparent);

  border-color:
    var(--brand, #2563eb);
}

.password-field {
  position: relative;
}

.password-field input {
  padding-right: 84px;
}

.password-field__toggle {
  position: absolute;
  top: 50%;
  right: 8px;

  transform: translateY(-50%);

  color:
    var(--brand, #2563eb);

  background: transparent;
  border: 0;

  font: inherit;
  font-size: 12px;
  font-weight: 600;

  cursor: pointer;
}

.auth-alert {
  padding: 10px 12px;

  color:
    var(--danger, #dc2626);

  background:
    var(--danger-soft, #fef2f2);

  border: 1px solid
    color-mix(in srgb, var(--danger, #dc2626) 28%, transparent);

  border-radius: 9px;

  font-size: 13px;
}

.auth-submit {
  min-height: 44px;

  padding: 9px 16px;

  color: #ffffff;

  background:
    var(--brand, #2563eb);

  border: 0;
  border-radius: 9px;

  font: inherit;
  font-weight: 700;

  cursor: pointer;
}

.auth-submit:disabled {
  opacity: 0.55;
  cursor: default;
}

.auth-switch {
  margin: 0;

  text-align: center;
}

.auth-switch a {
  color:
    var(--brand, #2563eb);

  font-weight: 600;
  text-decoration: none;
}
</style>
