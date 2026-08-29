<script setup>
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
</script>

<template>
  <main class="not-found-page">
    <section class="not-found-card">
      <div class="code">
        404
      </div>

      <h1>Страница не найдена</h1>

      <p>
        Такой страницы не существует или адрес был изменён.
      </p>

      <RouterLink
        v-if="authStore.isAuthenticated"
        :to="{ name: 'home' }"
      >
        На главную
      </RouterLink>

      <RouterLink
        v-else
        :to="{ name: 'login' }"
      >
        Войти
      </RouterLink>
    </section>
  </main>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.not-found-card {
  width: min(100%, 520px);
  display: grid;
  gap: 16px;
}

.code {
  font-size: 72px;
  font-weight: 700;
  line-height: 1;
}
</style>
