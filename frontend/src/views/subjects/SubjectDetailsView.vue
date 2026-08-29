<script setup>
import {
  computed,
  onMounted,
  ref,
  watch,
} from 'vue'

import {
  useRoute,
} from 'vue-router'

import {
  getApiErrorMessage,
  subjectsApi,
} from '@/api'

import SubjectsPageShell from '@/components/subjects/SubjectsPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

const route = useRoute()

const authStore =
  useAuthStore()

const loading = ref(false)
const error = ref('')
const subject = ref(null)

const subjectId = computed(() => {
  return Number(
    route.params.subjectId
  )
})

const pageTitle = computed(() => {
  return (
    subject.value?.name ||
    (
      subjectId.value
        ? `Предмет #${subjectId.value}`
        : 'Карточка предмета'
    )
  )
})

const pageSubtitle = computed(() => {
  if (
    authStore.isTeacher ||
    authStore.isAdmin
  ) {
    return (
      'Управление лекциями ' +
      'и тематикой предмета.'
    )
  }

  if (authStore.isStudent) {
    return (
      'Маршрут студента: ' +
      'предмет, лекции, тесты.'
    )
  }

  return (
    'Краткая информация о предмете ' +
    'и доступные действия.'
  )
})

const backRoute = computed(() => {
  const query = {}

  if (route.query.facultyId) {
    query.facultyId =
      route.query.facultyId
  }

  return {
    name: 'subjects',
    query,
  }
})

const studentLecturesRoute =
  computed(() => ({
    name: 'subject-lectures',

    params: {
      subjectId:
        subjectId.value,
    },

    query: {
      ...(route.query.facultyId
        ? {
            facultyId:
              route.query.facultyId,
          }
        : {}),
    },
  }))

const teacherLecturesRoute =
  computed(() => ({
    name: 'teacher-lectures',

    query: {
      subjectId:
        subjectId.value,
    },
  }))

const teacherTopicsRoute =
  computed(() => ({
    name: 'teacher-topics',

    query: {
      subjectId:
        subjectId.value,
    },
  }))

async function loadSubject() {
  if (
    !Number.isFinite(
      subjectId.value
    ) ||
    subjectId.value <= 0
  ) {
    subject.value = null
    error.value =
      'Не указан корректный subjectId.'

    return
  }

  loading.value = true
  error.value = ''

  try {
    const response =
      await subjectsApi.getById(
        subjectId.value
      )

    subject.value =
      response.data ?? null
  } catch (requestError) {
    subject.value = null

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить предмет.'
      )
  } finally {
    loading.value = false
  }
}

watch(
  subjectId,
  loadSubject
)

onMounted(loadSubject)
</script>

<template>
  <SubjectsPageShell
    :title="pageTitle"
    :subtitle="pageSubtitle"
    narrow
  >
    <template #actions>
      <UiButton
        :to="backRoute"
      >
        К предметам
      </UiButton>
    </template>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
    />

    <UiEmptyState
      v-if="
        loading &&
        !subject
      "
      description="Загрузка предмета..."
    />

    <UiCard
      v-else-if="subject"
      title="Информация о предмете"
    >
      <dl class="subject-data">
        <div class="subject-data__row">
          <dt>ID</dt>
          <dd>
            {{ subject.id ?? '—' }}
          </dd>
        </div>

        <div class="subject-data__row">
          <dt>Предмет</dt>
          <dd>
            {{ subject.name || '—' }}
          </dd>
        </div>

        <div class="subject-data__row">
          <dt>Описание</dt>
          <dd>
            {{
              subject.description ||
              '—'
            }}
          </dd>
        </div>
      </dl>

      <template #footer>
        <div class="subject-actions">
          <template
            v-if="
              authStore.isTeacher ||
              authStore.isAdmin
            "
          >
            <UiButton
              variant="primary"
              :to="teacherLecturesRoute"
            >
              Лекции
            </UiButton>

            <UiButton
              :to="teacherTopicsRoute"
            >
              Тематики
            </UiButton>
          </template>

          <UiButton
            v-else
            variant="primary"
            :to="studentLecturesRoute"
          >
            Лекции
          </UiButton>

          <UiButton
            :to="backRoute"
          >
            Вернуться к списку
          </UiButton>
        </div>
      </template>
    </UiCard>
  </SubjectsPageShell>
</template>

<style scoped>
.subject-data {
  margin: 0;

  display: grid;
}

.subject-data__row {
  padding: 12px 0;

  display: grid;
  grid-template-columns:
    minmax(120px, 180px)
    minmax(0, 1fr);
  gap: 14px;

  border-bottom:
    1px solid var(--border);
}

.subject-data__row:first-child {
  padding-top: 0;
}

.subject-data__row:last-child {
  padding-bottom: 0;

  border-bottom: 0;
}

.subject-data dt {
  color: var(--text-secondary);

  font-size: 13px;
  font-weight: 700;
}

.subject-data dd {
  margin: 0;

  color: var(--text);

  overflow-wrap: anywhere;
}

.subject-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 560px) {
  .subject-data__row {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .subject-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .subject-actions :deep(.ui-button) {
    width: 100%;
  }
}
</style>
