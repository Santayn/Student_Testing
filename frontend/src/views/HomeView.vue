<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { UiButton, UiTag } from '@/components/ui'
import { useAuthStore } from '@/stores/auth'
import { WORKSPACE_ROLE_LABELS } from '@/utils/workspaceRole'

const authStore = useAuthStore()
const router = useRouter()

const displayName = computed(() => {
  if (authStore.fullName) {
    return authStore.fullName
  }

  const person = authStore.user?.person

  if (person) {
    const nestedName = [
      person.firstName,
      person.middleName,
      person.lastName,
    ]
      .filter(Boolean)
      .join(' ')
      .trim()

    if (nestedName) {
      return nestedName
    }
  }

  return (
    authStore.user?.fullName ||
    authStore.loginName ||
    authStore.email ||
    'Пользователь'
  )
})

const roleLabels = {
  ADMIN: 'Администратор',
  TEACHER: 'Преподаватель',
  STUDENT: 'Студент',
}

const roleNames = computed(() => {
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

const roleText = computed(() => {
  return roleNames.value.length
    ? roleNames.value.join(', ')
    : 'Роль не указана'
})

const workspaceRoleText = computed(() => {
  return (
    WORKSPACE_ROLE_LABELS[
      authStore.workspaceRole
    ] ?? 'Не выбран'
  )
})

const workspaceSummary = computed(() => {
  switch (authStore.workspaceRole) {
    case 'ADMIN':
      return {
        title: 'Рабочее пространство администратора',
        description:
          'Управление академической структурой, назначениями, доступом пользователей и учебным контентом.',
        areas: [
          'Академическая структура',
          'Назначения',
          'Доступ пользователей',
          'Учебный контент',
        ],
      }

    case 'TEACHER':
      return {
        title: 'Рабочее пространство преподавателя',
        description:
          'Работа с предметами, лекциями, вопросами и тестами, а также просмотр назначенной учебной нагрузки.',
        areas: [
          'Мои предметы',
          'Учебный контент',
          'Моя нагрузка',
          'Результаты',
        ],
      }

    case 'STUDENT':
      return {
        title: 'Рабочее пространство студента',
        description:
          'Доступ к назначенным предметам, учебным материалам, тестированию и собственным результатам.',
        areas: [
          'Предметы',
          'Лекции',
          'Тестирование',
          'Результаты',
        ],
      }

    default:
      return {
        title: 'Рабочее пространство',
        description:
          'Выберите доступный рабочий режим в верхней панели.',
        areas: [],
      }
  }
})

async function refreshUser() {
  try {
    await authStore.refreshIdentity()
  } catch {
    if (!authStore.isAuthenticated) {
      await router.replace({
        name: 'login',
        query: {
          redirect: '/',
        },
      })
    }
  }
}
</script>

<template>
  <div class="home-view">
    <section class="welcome-panel">
      <div class="welcome-panel__content">
        <p class="welcome-panel__eyebrow">
          Student Testing
        </p>

        <h1 class="welcome-panel__title">
          Добро пожаловать, {{ displayName }}
        </h1>

        <p class="welcome-panel__description">
          Это обзор текущего рабочего пространства.
          Основная навигация теперь находится в боковом меню,
          поэтому главная страница не дублирует его пункты.
        </p>
      </div>

      <div class="workspace-badge" aria-label="Текущий рабочий режим">
        <span class="workspace-badge__label">
          Текущий режим
        </span>

        <strong class="workspace-badge__value">
          {{ workspaceRoleText }}
        </strong>
      </div>
    </section>

    <section class="home-grid">
      <article class="home-panel home-panel--workspace">
        <div class="home-panel__header">
          <div>
            <p class="home-panel__eyebrow">
              Рабочая область
            </p>

            <h2>{{ workspaceSummary.title }}</h2>
          </div>
        </div>

        <p class="home-panel__description">
          {{ workspaceSummary.description }}
        </p>

        <div
          v-if="workspaceSummary.areas.length"
          class="workspace-areas"
          aria-label="Доступные направления работы"
        >
          <UiTag
            v-for="area in workspaceSummary.areas"
            :key="area"
            variant="secondary"
          >
            {{ area }}
          </UiTag>
        </div>

        <p class="home-panel__hint">
          Для перехода между разделами используйте Sidebar слева.
          На узких экранах он открывается отдельной кнопкой меню.
        </p>
      </article>

      <article class="home-panel">
        <div class="home-panel__header">
          <div>
            <p class="home-panel__eyebrow">
              Учётная запись
            </p>

            <h2>Текущая сессия</h2>
          </div>
        </div>

        <dl class="account-summary">
          <div class="account-summary__item">
            <dt>Пользователь</dt>
            <dd>{{ displayName }}</dd>
          </div>

          <div class="account-summary__item">
            <dt>Роли</dt>
            <dd>{{ roleText }}</dd>
          </div>

          <div class="account-summary__item">
            <dt>Рабочий режим</dt>
            <dd>{{ workspaceRoleText }}</dd>
          </div>
        </dl>

        <div class="home-panel__actions">
          <UiButton
            type="button"
            :loading="authStore.syncingIdentity"
            loading-text="Обновление..."
            @click="refreshUser"
          >
            Обновить данные
          </UiButton>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  display: grid;
  gap: var(--st-space-section);
}

.welcome-panel {
  padding: clamp(22px, 4vw, 34px);

  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 24px;

  color: var(--st-text);
  background: var(--st-surface);

  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.welcome-panel__eyebrow,
.home-panel__eyebrow {
  margin: 0 0 8px;

  color: var(--st-primary);

  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.welcome-panel__title {
  max-width: 760px;
  margin: 0;

  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.08;
  letter-spacing: -0.025em;
}

.welcome-panel__description {
  max-width: 720px;
  margin: 14px 0 0;

  color: var(--st-text-secondary);

  font-size: 15px;
  line-height: 1.65;
}

.workspace-badge {
  min-width: 180px;
  padding: 14px 16px;

  display: grid;
  gap: 4px;

  background: var(--st-primary-soft);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-lg);
}

.workspace-badge__label {
  color: var(--st-text-secondary);
  font-size: 12px;
}

.workspace-badge__value {
  color: var(--st-primary-soft-text);
  font-size: 15px;
}

.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.65fr);
  gap: var(--st-space-section);
}

.home-panel {
  min-width: 0;
  padding: var(--st-space-card);

  display: grid;
  align-content: start;
  gap: 16px;

  color: var(--st-text);
  background: var(--st-surface);

  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.home-panel--workspace {
  min-height: 100%;
}

.home-panel__header h2,
.home-panel__description,
.home-panel__hint {
  margin: 0;
}

.home-panel__header h2 {
  font-size: 20px;
}

.home-panel__description,
.home-panel__hint {
  color: var(--st-text-secondary);
  line-height: 1.6;
}

.home-panel__description {
  font-size: 14px;
}

.home-panel__hint {
  padding: 12px 14px;

  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-md);

  font-size: 13px;
}

.workspace-areas {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.account-summary {
  margin: 0;
  display: grid;
  gap: 12px;
}

.account-summary__item {
  padding-bottom: 12px;

  display: grid;
  gap: 4px;

  border-bottom: 1px solid var(--st-border);
}

.account-summary__item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.account-summary dt {
  color: var(--st-text-muted);
  font-size: 12px;
}

.account-summary dd {
  margin: 0;
  overflow-wrap: anywhere;

  color: var(--st-text);
  font-size: 14px;
  font-weight: 700;
}

.home-panel__actions {
  margin-top: auto;
  padding-top: 4px;
}

@media (max-width: 860px) {
  .welcome-panel,
  .home-grid {
    grid-template-columns: 1fr;
  }

  .workspace-badge {
    min-width: 0;
  }
}

@media (max-width: 480px) {
  .home-view {
    gap: 14px;
  }

  .welcome-panel,
  .home-panel {
    border-radius: var(--st-radius-lg);
  }
}
</style>
