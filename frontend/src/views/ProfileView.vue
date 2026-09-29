<script setup>
import {
  computed,
  onMounted,
} from 'vue'

import {
  useRouter,
} from 'vue-router'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiDialog,
  UiEmptyState,
  UiInput,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  usePasswordChange,
} from '@/composables/profile/usePasswordChange'

import {
  useProfileContext,
} from '@/composables/profile/useProfileContext'

const router = useRouter()
const authStore = useAuthStore()
const {
  profileLoading,
  studentLoading,
  teacherLoading,
  profileError,
  studentError,
  teacherError,
  studentInfo,
  teacherInfo,
  loadProfile,
} = useProfileContext({
  authStore,
  router,
})

const user = computed(() => authStore.user ?? {})
const person = computed(() => user.value.person ?? {})

const firstName = computed(() => {
  return person.value.firstName ?? user.value.firstName ?? '-'
})

const lastName = computed(() => {
  return person.value.lastName ?? user.value.lastName ?? '-'
})

const phoneNumber = computed(() => {
  return (
    person.value.phone ??
    person.value.phoneNumber ??
    user.value.phone ??
    user.value.phoneNumber ??
    '-'
  )
})

const loginName = computed(() => user.value.login ?? '-')

const email = computed(() => {
  return user.value.email ?? person.value.email ?? '-'
})

const dateOfBirth = computed(() => {
  return formatDateOfBirth(
    person.value.dateOfBirth ?? user.value.dateOfBirth
  )
})

const fullName = computed(() => {
  const value = [
    lastName.value !== '-' ? lastName.value : '',
    firstName.value !== '-' ? firstName.value : '',
  ]
    .filter(Boolean)
    .join(' ')
    .trim()

  return value || authStore.fullName || loginName.value
})

const roleNames = computed(() => {
  return authStore.roles
    .map(roleLabel)
    .filter(Boolean)
})

const rolesText = computed(() => {
  return roleNames.value.length
    ? roleNames.value.join(', ')
    : '-'
})

const workspaceRoleText = computed(() => {
  return workspaceRoleLabel(authStore.workspaceRole)
})

const {
  passwordDialogOpen,
  passwordTouched,
  passwordForm,
  currentPasswordError,
  newPasswordError,
  confirmPasswordError,
  canChangePassword,
  openPasswordDialog,
  setPasswordDialogVisible,
  submitPasswordChange,
} = usePasswordChange({
  authStore,
  router,
})

function roleLabel(role) {
  const labels = {
    ADMIN: 'Администратор',
    TEACHER: 'Преподаватель',
    STUDENT: 'Студент',
  }

  const value =
    typeof role === 'string'
      ? role
      : role?.name ?? role?.code ?? role?.authority ?? ''

  return labels[value] ?? value
}

function workspaceRoleLabel(role) {
  const labels = {
    ADMIN: 'Администратор',
    TEACHER: 'Преподаватель',
    STUDENT: 'Студент',
  }

  return labels[role] ?? 'Не выбран'
}

function formatDateOfBirth(value) {
  if (!value) {
    return '-'
  }

  const match = String(value).match(/^(\d{4})-(\d{2})-(\d{2})/)

  if (match) {
    return `${match[3]}.${match[2]}.${match[1]}`
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return String(value)
  }

  return new Intl.DateTimeFormat('ru-RU').format(date)
}

function subjectLabel(subject) {
  return subject?.name ?? subject?.title ?? 'Предмет без названия'
}

function groupLabel(group) {
  return group?.name ?? group?.code ?? 'Группа без названия'
}

function facultyLabel(faculty) {
  return faculty?.name ?? faculty?.code ?? 'Факультет без названия'
}

onMounted(() => loadProfile())
</script>

<template>
  <div class="profile-view">
    <header class="profile-header">
      <div class="profile-header__copy">
        <p class="profile-header__eyebrow">
          Личный кабинет
        </p>

        <h1>{{ fullName }}</h1>

        <p>
          Личные данные, параметры учётной записи и учебный контекст.
        </p>
      </div>

      <UiButton
        class="profile-refresh"
        type="button"
        :loading="profileLoading"
        loading-text="Обновление..."
        @click="loadProfile({ synchronizeIdentity: true })"
      >
        Обновить данные
      </UiButton>
    </header>

    <UiAlert
      v-if="profileError"
      variant="danger"
      :message="profileError"
    />

    <section class="profile-grid" aria-label="Данные профиля">
      <UiCard
        title="Личные данные"
        description="Информация связанного профиля Person. Редактирование выполняет администратор."
      >
        <dl class="data-grid">
          <div class="data-item">
            <dt>Фамилия</dt>
            <dd>{{ lastName }}</dd>
          </div>

          <div class="data-item">
            <dt>Имя</dt>
            <dd>{{ firstName }}</dd>
          </div>

          <div class="data-item">
            <dt>Дата рождения</dt>
            <dd>{{ dateOfBirth }}</dd>
          </div>

          <div class="data-item">
            <dt>Email</dt>
            <dd>{{ email }}</dd>
          </div>

          <div class="data-item">
            <dt>Телефон</dt>
            <dd>{{ phoneNumber }}</dd>
          </div>
        </dl>
      </UiCard>

      <UiCard
        title="Учётная запись"
        description="Параметры входа и текущий рабочий режим."
      >
        <dl class="data-grid">
          <div class="data-item">
            <dt>Логин</dt>
            <dd>{{ loginName }}</dd>
          </div>

          <div class="data-item">
            <dt>Роли</dt>
            <dd>{{ rolesText }}</dd>
          </div>

          <div class="data-item">
            <dt>Рабочий режим</dt>
            <dd>{{ workspaceRoleText }}</dd>
          </div>
        </dl>
      </UiCard>
    </section>

    <UiCard
      title="Безопасность"
      description="После смены пароля все refresh-сессии отзываются, поэтому потребуется войти снова."
    >
      <div class="security-row">
        <div>
          <strong>Пароль</strong>
          <p>
            Используйте отдельный пароль и не передавайте его другим пользователям.
          </p>
        </div>

        <UiButton
          type="button"
          @click="openPasswordDialog"
        >
          Изменить пароль
        </UiButton>
      </div>
    </UiCard>

    <UiCard
      v-if="authStore.isStudent"
      title="Учебный контекст студента"
      description="Активные группы, факультеты и доступные предметы."
    >
      <UiAlert
        v-if="studentError"
        variant="warning"
        :message="studentError"
      />

      <UiEmptyState
        v-if="studentLoading"
        description="Загрузка данных студента..."
        compact
      />

      <template v-else>
        <dl class="data-grid data-grid--context">
          <div class="data-item">
            <dt>Группы</dt>
            <dd>
              {{
                studentInfo.groups.length
                  ? studentInfo.groups.map(groupLabel).join(', ')
                  : 'Нет активной группы'
              }}
            </dd>
          </div>

          <div class="data-item">
            <dt>Факультеты</dt>
            <dd>
              {{
                studentInfo.faculties.length
                  ? studentInfo.faculties.map(facultyLabel).join(', ')
                  : '-'
              }}
            </dd>
          </div>
        </dl>

        <div class="profile-list-block">
          <h3>Предметы</h3>

          <ul
            v-if="studentInfo.subjects.length"
            class="entity-list"
          >
            <li
              v-for="subject in studentInfo.subjects"
              :key="subject.id"
            >
              <RouterLink
                :to="{
                  name: 'subject-details',
                  params: { subjectId: subject.id },
                }"
              >
                {{ subjectLabel(subject) }}
              </RouterLink>
            </li>
          </ul>

          <UiEmptyState
            v-else
            description="Предметы не найдены."
            compact
          />
        </div>
      </template>
    </UiCard>

    <UiCard
      v-if="authStore.isTeacher"
      title="Учебный контекст преподавателя"
      description="Предметы и группы из текущей учебной нагрузки."
    >
      <UiAlert
        v-if="teacherError"
        variant="warning"
        :message="teacherError"
      />

      <UiEmptyState
        v-if="teacherLoading"
        description="Загрузка данных преподавателя..."
        compact
      />

      <div v-else class="teacher-grid">
        <div class="profile-list-block">
          <h3>Предметы</h3>

          <ul
            v-if="teacherInfo.subjects.length"
            class="entity-list"
          >
            <li
              v-for="subject in teacherInfo.subjects"
              :key="subject.id"
            >
              <RouterLink
                :to="{
                  name: 'subject-details',
                  params: { subjectId: subject.id },
                }"
              >
                {{ subjectLabel(subject) }}
              </RouterLink>
            </li>
          </ul>

          <UiEmptyState
            v-else
            description="Предметы не найдены."
            compact
          />
        </div>

        <div class="profile-list-block">
          <h3>Группы из нагрузки</h3>

          <ul
            v-if="teacherInfo.groups.length"
            class="entity-list"
          >
            <li
              v-for="group in teacherInfo.groups"
              :key="group.id"
            >
              <span>{{ groupLabel(group) }}</span>
            </li>
          </ul>

          <UiEmptyState
            v-else
            description="Группы не найдены."
            compact
          />
        </div>
      </div>
    </UiCard>

    <UiDialog
      :model-value="passwordDialogOpen"
      title="Изменить пароль"
      width="30rem"
      :closable="!authStore.changingPassword"
      :close-on-escape="!authStore.changingPassword"
      @update:model-value="setPasswordDialogVisible"
    >
      <form
        id="change-password-form"
        class="password-form"
        @submit.prevent="submitPasswordChange"
      >
        <p class="password-form__intro">
          После успешного изменения пароля текущая сессия завершится.
        </p>

        <UiInput
          v-model="passwordForm.currentPassword"
          label="Текущий пароль"
          type="password"
          autocomplete="current-password"
          minlength="6"
          maxlength="200"
          :error="currentPasswordError"
          :disabled="authStore.changingPassword"
          required
          @blur="passwordTouched.current = true"
        />

        <UiInput
          v-model="passwordForm.newPassword"
          label="Новый пароль"
          type="password"
          autocomplete="new-password"
          minlength="6"
          maxlength="200"
          :error="newPasswordError"
          :disabled="authStore.changingPassword"
          required
          @blur="passwordTouched.next = true"
        />

        <UiInput
          v-model="passwordForm.confirmPassword"
          label="Повторите новый пароль"
          type="password"
          autocomplete="new-password"
          minlength="6"
          maxlength="200"
          :error="confirmPasswordError"
          :disabled="authStore.changingPassword"
          required
          @blur="passwordTouched.confirm = true"
        />

        <UiAlert
          v-if="authStore.passwordError"
          variant="danger"
          :message="authStore.passwordError"
        />
      </form>

      <template #footer>
        <UiButton
          type="button"
          :disabled="authStore.changingPassword"
          @click="setPasswordDialogVisible(false)"
        >
          Отмена
        </UiButton>

        <UiButton
          variant="primary"
          type="button"
          :disabled="!canChangePassword"
          :loading="authStore.changingPassword"
          loading-text="Изменение..."
          @click="submitPasswordChange"
        >
          Изменить пароль
        </UiButton>
      </template>
    </UiDialog>
  </div>
</template>

<style scoped>
.profile-view {
  display: grid;
  gap: var(--st-space-section);
}

.profile-header {
  padding: clamp(20px, 3vw, 28px);
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  color: var(--st-text);
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.profile-header__copy {
  min-width: 0;
}

.profile-header__eyebrow,
.profile-header h1,
.profile-header p {
  margin: 0;
}

.profile-header__eyebrow {
  margin-bottom: 6px;
  color: var(--st-primary);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.profile-header h1 {
  font-size: clamp(26px, 4vw, 36px);
  line-height: 1.15;
}

.profile-header p:not(.profile-header__eyebrow) {
  margin-top: 8px;
  color: var(--st-text-secondary);
  font-size: 14px;
  line-height: 1.55;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--st-space-section);
}

.data-grid {
  margin: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.data-grid--context {
  margin-bottom: 20px;
}

.data-item {
  min-width: 0;
  padding: 12px;
  display: grid;
  gap: 4px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-control);
}

.data-item dt {
  color: var(--st-text-secondary);
  font-size: 12px;
}

.data-item dd {
  margin: 0;
  overflow-wrap: anywhere;
  color: var(--st-text);
  font-size: 14px;
  font-weight: 650;
}

.security-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.security-row strong,
.security-row p {
  margin: 0;
}

.security-row p {
  margin-top: 5px;
  color: var(--st-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.teacher-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-list-block {
  min-width: 0;
}

.profile-list-block h3 {
  margin: 0 0 10px;
  color: var(--st-text);
  font-size: 15px;
}

.entity-list {
  margin: 0;
  padding: 0;
  display: grid;
  gap: 7px;
  list-style: none;
}

.entity-list li {
  min-height: 42px;
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--st-text);
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-control);
  font-size: 13px;
}

.entity-list a {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--st-primary);
  font-weight: 700;
  text-decoration: none;
}

.entity-list a:hover {
  text-decoration: underline;
}

.password-form {
  display: grid;
  gap: 16px;
}

.password-form__intro {
  margin: 0;
  color: var(--st-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 820px) {
  .profile-grid,
  .teacher-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .profile-header,
  .security-row {
    flex-direction: column;
  }

  .profile-refresh,
  .security-row :deep(.st-ui-button) {
    width: 100%;
  }

  .data-grid {
    grid-template-columns: 1fr;
  }
}
</style>
