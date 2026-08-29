<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const attemptedPath = computed(() => {
  return typeof route.query.from === 'string'
    ? route.query.from
    : ''
})

const roleLabels = {
  ADMIN: 'Администратор',
  TEACHER: 'Преподаватель',
  STUDENT: 'Студент',
}

const currentRoles = computed(() => {
  return authStore.roles
    .map((role) => {
      if (typeof role === 'string') {
        return roleLabels[role] ?? role
      }

      const value =
        role?.name ??
        role?.code ??
        role?.authority ??
        ''

      return roleLabels[value] ?? value
    })
    .filter(Boolean)
})
</script>

<template>
  <section class="error-page">
    <div class="error-card">
      <div class="error-code error-code--forbidden">
        403
      </div>

      <h1 class="error-title">
        Доступ запрещён
      </h1>

      <p class="error-description">
        У вашей учётной записи недостаточно прав
        для просмотра этой страницы.
      </p>

      <div
        v-if="currentRoles.length"
        class="error-meta"
      >
        <span class="error-meta__label">
          Ваши роли:
        </span>

        <span class="error-meta__value">
          {{ currentRoles.join(', ') }}
        </span>
      </div>

      <div
        v-if="attemptedPath"
        class="error-meta"
      >
        <span class="error-meta__label">
          Запрошенный адрес:
        </span>

        <code class="error-meta__value">
          {{ attemptedPath }}
        </code>
      </div>

      <div class="error-actions">
        <RouterLink
          class="error-button error-button--primary"
          :to="{ name: 'home' }"
        >
          На главную
        </RouterLink>

        <RouterLink
          class="error-button"
          :to="{ name: 'profile' }"
        >
          Профиль
        </RouterLink>

        <button
          class="error-button"
          type="button"
          @click="$router.back()"
        >
          Назад
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.error-page {
  min-height: calc(100vh - 160px);

  display: grid;
  place-items: center;

  padding: 32px 20px;
}

.error-card {
  width: min(100%, 600px);

  padding: 36px;

  display: grid;
  justify-items: center;
  gap: 18px;

  text-align: center;

  color:
    var(--text, #111827);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 16px;

  box-shadow:
    var(--shadow, 0 12px 30px rgb(0 0 0 / 8%));
}

.error-code {
  font-size: clamp(64px, 14vw, 110px);
  font-weight: 800;
  line-height: 0.9;

  letter-spacing: -0.05em;
}

.error-code--forbidden {
  color:
    var(--danger, #dc2626);
}

.error-title {
  margin: 0;

  font-size: clamp(24px, 5vw, 34px);
  line-height: 1.15;
}

.error-description {
  max-width: 450px;

  margin: 0;

  color:
    var(--text-secondary, #6b7280);

  font-size: 15px;
  line-height: 1.6;
}

.error-meta {
  width: 100%;

  padding: 10px 12px;

  display: grid;
  gap: 4px;

  text-align: left;

  background:
    var(--surface-secondary, #f3f4f6);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 9px;
}

.error-meta__label {
  color:
    var(--text-secondary, #6b7280);

  font-size: 12px;
}

.error-meta__value {
  color:
    var(--text, #111827);

  font-size: 13px;
  font-weight: 600;

  overflow-wrap: anywhere;
}

.error-actions {
  margin-top: 4px;

  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.error-button {
  min-height: 42px;

  padding: 9px 16px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color:
    var(--text, #111827);

  background:
    var(--surface-secondary, #f3f4f6);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 9px;

  font: inherit;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;

  cursor: pointer;
}

.error-button:hover {
  filter: brightness(0.98);
}

.error-button--primary {
  color: #ffffff;

  background:
    var(--brand, #2563eb);

  border-color:
    var(--brand, #2563eb);
}

@media (max-width: 520px) {
  .error-page {
    min-height: calc(100vh - 140px);
    padding: 20px 14px;
  }

  .error-card {
    padding: 28px 20px;
  }

  .error-actions {
    width: 100%;
  }

  .error-button {
    width: 100%;
  }
}
</style>
