<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = ref({
  login: '',
  email: '',
  firstName: '',
  lastName: '',
  middleName: '',
  password: '',
  confirmPassword: '',
})

const localError = ref('')

const canSubmit = computed(() => {
  return (
    form.value.login.trim() &&
    form.value.email.trim() &&
    form.value.firstName.trim() &&
    form.value.lastName.trim() &&
    form.value.password &&
    form.value.confirmPassword &&
    !authStore.loading
  )
})

async function submit() {
  localError.value = ''

  if (!canSubmit.value) {
    localError.value = 'Заполните обязательные поля'
    return
  }

  if (form.value.password !== form.value.confirmPassword) {
    localError.value = 'Пароли не совпадают'
    return
  }

  try {
    await authStore.register({
      login: form.value.login.trim(),
      email: form.value.email.trim(),
      firstName: form.value.firstName.trim(),
      lastName: form.value.lastName.trim(),
      middleName: form.value.middleName.trim() || null,
      password: form.value.password,
    })

    await router.replace({
      name: 'login',
      query: {
        registered: '1',
      },
    })
  } catch {
    // backend error уже доступна через authStore.error
  }
}
</script>

<template>
  <div class="auth-view">
    <header class="auth-view__header">
      <h1>Регистрация</h1>

      <p>
        Создайте новую учётную запись.
      </p>
    </header>

    <form
      class="auth-form"
      @submit.prevent="submit"
    >
      <label class="auth-field">
        <span>Логин *</span>

        <input
          v-model="form.login"
          type="text"
          autocomplete="username"
        >
      </label>

      <label class="auth-field">
        <span>Email *</span>

        <input
          v-model="form.email"
          type="email"
          autocomplete="email"
        >
      </label>

      <div class="auth-form__grid">
        <label class="auth-field">
          <span>Фамилия *</span>

          <input
            v-model="form.lastName"
            type="text"
            autocomplete="family-name"
          >
        </label>

        <label class="auth-field">
          <span>Имя *</span>

          <input
            v-model="form.firstName"
            type="text"
            autocomplete="given-name"
          >
        </label>
      </div>

      <label class="auth-field">
        <span>Отчество</span>

        <input
          v-model="form.middleName"
          type="text"
        >
      </label>

      <label class="auth-field">
        <span>Пароль *</span>

        <input
          v-model="form.password"
          type="password"
          autocomplete="new-password"
        >
      </label>

      <label class="auth-field">
        <span>Повторите пароль *</span>

        <input
          v-model="form.confirmPassword"
          type="password"
          autocomplete="new-password"
        >
      </label>

      <div
        v-if="localError || authStore.error"
        class="auth-alert"
        role="alert"
      >
        {{ localError || authStore.error }}
      </div>

      <button
        class="auth-submit"
        type="submit"
        :disabled="!canSubmit"
      >
        {{
          authStore.loading
            ? 'Регистрация...'
            : 'Зарегистрироваться'
        }}
      </button>
    </form>

    <p class="auth-switch">
      Уже есть аккаунт?

      <RouterLink :to="{ name: 'login' }">
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

.auth-form__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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

@media (max-width: 520px) {
  .auth-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
