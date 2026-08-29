<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const login = ref('')
const password = ref('')

async function submit() {
  try {
    await authStore.login(
      login.value,
      password.value
    )

    const redirect =
      typeof route.query.redirect === 'string'
        ? route.query.redirect
        : '/'

    await router.replace(redirect)
  } catch {
    // Сообщение уже доступно через authStore.error
  }
}
</script>

<template>
  <main>
    <h1>Вход</h1>

    <form @submit.prevent="submit">
      <input
        v-model="login"
        type="text"
        autocomplete="username"
        placeholder="Логин"
      >

      <input
        v-model="password"
        type="password"
        autocomplete="current-password"
        placeholder="Пароль"
      >

      <button
        type="submit"
        :disabled="authStore.loading"
      >
        Войти
      </button>

      <p v-if="authStore.error">
        {{ authStore.error }}
      </p>
    </form>
  </main>
</template>
