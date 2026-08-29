<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const attemptedPath = computed(() => {
  return typeof route.query.from === 'string'
    ? route.query.from
    : null
})
</script>

<template>
  <main class="forbidden-page">
    <section class="forbidden-card">
      <div class="code">
        403
      </div>

      <h1>Доступ запрещён</h1>

      <p>
        У вашей учётной записи недостаточно прав
        для просмотра этой страницы.
      </p>

      <p v-if="attemptedPath">
        Запрошенный адрес:
        <code>{{ attemptedPath }}</code>
      </p>

      <div class="actions">
        <RouterLink :to="{ name: 'home' }">
          На главную
        </RouterLink>

        <RouterLink :to="{ name: 'profile' }">
          Профиль
        </RouterLink>
      </div>

      <p v-if="authStore.roles.length">
        Ваши роли:
        {{ authStore.roles
          .map(role =>
            typeof role === 'string'
              ? role
              : role?.name ?? role?.code ?? role?.authority
          )
          .filter(Boolean)
          .join(', ')
        }}
      </p>
    </section>
  </main>
</template>

<style scoped>
.forbidden-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.forbidden-card {
  width: min(100%, 560px);
  display: grid;
  gap: 16px;
}

.code {
  font-size: 72px;
  font-weight: 700;
  line-height: 1;
}

.actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

code {
  word-break: break-all;
}
</style>
