<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

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

const commonActions = computed(() => [
  {
    title: 'Профиль',
    description: 'Личные данные и информация об учётной записи.',
    route: { name: 'profile' },
  },
  {
    title: 'Предметы',
    description: 'Открыть доступные учебные предметы.',
    route: { name: 'subjects' },
  },
  {
    title: 'Результаты',
    description: 'Просмотреть результаты тестирования.',
    route: { name: 'results' },
  },
])

const teacherActions = [
  {
    title: 'Вопросы',
    description: 'Банк вопросов для тестов.',
    route: { name: 'teacher-questions' },
  },
  {
    title: 'Создать тест',
    description: 'Собрать новый тест и назначить его.',
    route: { name: 'teacher-test-create' },
  },
  {
    title: 'Лекции',
    description: 'Управление лекциями и материалами.',
    route: { name: 'teacher-lectures' },
  },
  {
    title: 'Темы',
    description: 'Работа с библиотекой учебных тем.',
    route: { name: 'teacher-topics' },
  },
]

const adminActions = [
  {
    title: 'Пользователи',
    description: 'Учётные записи, роли и доступ.',
    route: { name: 'admin-users' },
  },
  {
    title: 'Факультеты',
    description: 'Управление факультетами.',
    route: { name: 'admin-faculties' },
  },
  {
    title: 'Группы',
    description: 'Учебные группы и их состав.',
    route: { name: 'admin-groups' },
  },
  {
    title: 'Предметы',
    description: 'Администрирование учебных предметов.',
    route: { name: 'admin-subjects' },
  },
]

async function refreshUser() {
  try {
    await authStore.loadCurrentUser()
  } catch {
    // authStore уже содержит состояние ошибки сессии/API.
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
          Здесь собраны предметы, тесты, результаты
          и основные рабочие разделы платформы.
        </p>
      </div>

      <div class="user-summary">
        <div class="user-summary__item">
          <span class="user-summary__label">
            Пользователь
          </span>

          <strong class="user-summary__value">
            {{ displayName }}
          </strong>
        </div>

        <div class="user-summary__item">
          <span class="user-summary__label">
            Роль
          </span>

          <strong class="user-summary__value">
            {{ roleText }}
          </strong>
        </div>

        <button
          class="user-summary__refresh"
          type="button"
          :disabled="authStore.loading"
          @click="refreshUser"
        >
          {{
            authStore.loading
              ? 'Обновление...'
              : 'Обновить данные'
          }}
        </button>
      </div>
    </section>

    <section class="home-section">
      <div class="home-section__header">
        <div>
          <h2>Основные разделы</h2>

          <p>
            Быстрый доступ к наиболее часто используемым страницам.
          </p>
        </div>
      </div>

      <div class="action-grid">
        <RouterLink
          v-for="item in commonActions"
          :key="item.title"
          class="action-card"
          :to="item.route"
        >
          <strong class="action-card__title">
            {{ item.title }}
          </strong>

          <span class="action-card__description">
            {{ item.description }}
          </span>

          <span class="action-card__open">
            Открыть →
          </span>
        </RouterLink>
      </div>
    </section>

    <section
      v-if="authStore.isTeacher || authStore.isAdmin"
      class="home-section"
    >
      <div class="home-section__header">
        <div>
          <h2>Работа преподавателя</h2>

          <p>
            Тесты, вопросы, лекции и учебные материалы.
          </p>
        </div>
      </div>

      <div class="action-grid">
        <RouterLink
          v-for="item in teacherActions"
          :key="item.title"
          class="action-card"
          :to="item.route"
        >
          <strong class="action-card__title">
            {{ item.title }}
          </strong>

          <span class="action-card__description">
            {{ item.description }}
          </span>

          <span class="action-card__open">
            Открыть →
          </span>
        </RouterLink>
      </div>
    </section>

    <section
      v-if="authStore.isAdmin"
      class="home-section"
    >
      <div class="home-section__header">
        <div>
          <h2>Администрирование</h2>

          <p>
            Основные справочники и управление платформой.
          </p>
        </div>
      </div>

      <div class="action-grid">
        <RouterLink
          v-for="item in adminActions"
          :key="item.title"
          class="action-card"
          :to="item.route"
        >
          <strong class="action-card__title">
            {{ item.title }}
          </strong>

          <span class="action-card__description">
            {{ item.description }}
          </span>

          <span class="action-card__open">
            Открыть →
          </span>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home-view {
  display: grid;
  gap: 22px;
}

.welcome-panel {
  padding: 28px;

  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, 340px);
  gap: 28px;

  color:
    var(--text, #111827);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 16px;

  box-shadow:
    var(--shadow, 0 8px 24px rgb(0 0 0 / 6%));
}

.welcome-panel__content {
  align-self: center;
}

.welcome-panel__eyebrow {
  margin: 0 0 8px;

  color:
    var(--brand, #2563eb);

  font-size: 13px;
  font-weight: 800;

  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.welcome-panel__title {
  max-width: 720px;

  margin: 0;

  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.08;
  letter-spacing: -0.025em;
}

.welcome-panel__description {
  max-width: 680px;

  margin: 14px 0 0;

  color:
    var(--text-secondary, #6b7280);

  font-size: 16px;
  line-height: 1.65;
}

.user-summary {
  padding: 18px;

  align-self: stretch;

  display: grid;
  align-content: center;
  gap: 14px;

  background:
    var(--surface-secondary, #f7f8fa);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 12px;
}

.user-summary__item {
  display: grid;
  gap: 3px;
}

.user-summary__label {
  color:
    var(--text-secondary, #6b7280);

  font-size: 12px;
}

.user-summary__value {
  overflow-wrap: anywhere;

  font-size: 14px;
}

.user-summary__refresh {
  min-height: 38px;

  margin-top: 4px;
  padding: 7px 11px;

  color:
    var(--text, #111827);

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 8px;

  font: inherit;
  font-size: 13px;
  font-weight: 600;

  cursor: pointer;
}

.user-summary__refresh:hover:not(:disabled) {
  background:
    var(--surface-secondary, #f3f4f6);
}

.user-summary__refresh:disabled {
  opacity: 0.55;
  cursor: default;
}

.home-section {
  padding: 22px;

  display: grid;
  gap: 16px;

  background:
    var(--surface, #ffffff);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 14px;
}

.home-section__header h2,
.home-section__header p {
  margin: 0;
}

.home-section__header h2 {
  color:
    var(--text, #111827);

  font-size: 20px;
}

.home-section__header p {
  margin-top: 5px;

  color:
    var(--text-secondary, #6b7280);

  font-size: 14px;
}

.action-grid {
  display: grid;
  grid-template-columns:
    repeat(auto-fit, minmax(210px, 1fr));
  gap: 12px;
}

.action-card {
  min-height: 138px;

  padding: 16px;

  display: flex;
  flex-direction: column;

  color:
    var(--text, #111827);

  background:
    var(--surface-secondary, #f8fafc);

  border: 1px solid
    var(--border, #e5e7eb);

  border-radius: 11px;

  text-decoration: none;

  transition:
    border-color 0.15s ease,
    transform 0.15s ease,
    box-shadow 0.15s ease;
}

.action-card:hover {
  transform: translateY(-2px);

  border-color:
    var(--brand, #2563eb);

  box-shadow:
    0 8px 18px rgb(0 0 0 / 7%);
}

.action-card__title {
  font-size: 16px;
}

.action-card__description {
  margin-top: 7px;

  color:
    var(--text-secondary, #6b7280);

  font-size: 13px;
  line-height: 1.5;
}

.action-card__open {
  margin-top: auto;
  padding-top: 14px;

  color:
    var(--brand, #2563eb);

  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 760px) {
  .welcome-panel {
    padding: 20px;

    grid-template-columns: 1fr;
    gap: 18px;
  }

  .home-section {
    padding: 18px;
  }
}

@media (max-width: 480px) {
  .home-view {
    gap: 14px;
  }

  .welcome-panel,
  .home-section {
    border-radius: 12px;
  }

  .action-grid {
    grid-template-columns: 1fr;
  }

  .action-card {
    min-height: 120px;
  }
}
</style>
