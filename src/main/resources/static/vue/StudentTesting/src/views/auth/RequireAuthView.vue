<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const loginRoute = computed(() => ({
  name: 'login',
  query: {
    redirect:
      typeof route.query.redirect === 'string'
        ? route.query.redirect
        : '/',
  },
}))
</script>

<template>
  <main class="auth-required-page">
    <section class="auth-required-card">
      <h1>Требуется авторизация</h1>

      <p>
        Для просмотра этой страницы необходимо войти в систему.
      </p>

      <RouterLink :to="loginRoute">
        Войти
      </RouterLink>

      <RouterLink :to="{ name: 'register' }">
        Зарегистрироваться
      </RouterLink>
    </section>
  </main>
</template>

<style scoped>
.auth-required-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-required-card {
  width: min(100%, 520px);
  display: grid;
  gap: 16px;
}
</style>
