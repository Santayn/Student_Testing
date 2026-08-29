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
    var(--text);

  font-size: 28px;
}

.auth-view__header p,
.auth-switch {
  color:
    var(--text-secondary);

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
    var(--text);

  font-size: 14px;
  font-weight: 600;
}

.auth-field input {
  width: 100%;
  min-height: 44px;

  padding: 9px 11px;

  color:
    var(--text);

  background:
    var(--surface);

  border: 1px solid
    var(--border);

  border-radius: 9px;

  font: inherit;
  font-weight: 400;
}

.auth-field input:focus {
  outline: 2px solid
    var(--focus-ring);

  border-color:
    var(--brand);
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
    var(--brand);

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
    var(--danger);

  background:
    var(--danger-soft);

  border: 1px solid
    var(--danger-border);

  border-radius: 9px;

  font-size: 13px;
}

.auth-submit {
  min-height: 44px;

  padding: 9px 16px;

  color: var(--text-on-brand);

  background:
    var(--brand);

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
    var(--brand);

  font-weight: 600;
  text-decoration: none;
}
</style>
