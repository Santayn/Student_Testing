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
  learningApi,
  subjectsApi,
} from '@/api'

import SubjectsPageShell from '@/components/subjects/SubjectsPageShell.vue'

import {
  useBreadcrumbContext,
} from '@/navigation'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const route = useRoute()

const authStore =
  useAuthStore()

const loading = ref(false)
const error = ref('')
const subject = ref(null)

const subjectRequest =
  createLatestRequestGuard()

const subjectId = computed(() => {
  return Number(
    route.params.subjectId
  )
})

useBreadcrumbContext(() => ({
  subjectId: subjectId.value,
  subjectName:
    Number(subject.value?.id) ===
    subjectId.value
      ? subject.value?.name
      : null,
}))

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
    authStore.isTeacherMode ||
    authStore.isAdminMode
  ) {
    return (
      'Управление лекциями ' +
      'и тематикой предмета.'
    )
  }

  if (authStore.isStudentMode) {
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
  const requestId =
    subjectRequest.begin()

  const requestedSubjectId =
    Number(subjectId.value)
  const requestedStudentMode =
    authStore.isStudentMode

  if (
    !Number.isFinite(
      requestedSubjectId
    ) ||
    requestedSubjectId <= 0
  ) {
    subject.value = null
    error.value =
      'Не указан корректный subjectId.'
    loading.value = false

    return
  }

  loading.value = true
  error.value = ''

  try {
    const response =
      requestedStudentMode
        ? await learningApi.getSubject(
            requestedSubjectId
          )
        : await subjectsApi.getById(
            requestedSubjectId
          )

    if (
      !subjectRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    subject.value =
      response.data ?? null
  } catch (requestError) {
    if (
      !subjectRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    subject.value = null

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить предмет.'
      )
  } finally {
    if (
      subjectRequest.isCurrent(
        requestId
      )
    ) {
      loading.value = false
    }
  }
}

watch(
  [
    subjectId,
    () => authStore.isStudentMode,
  ],
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
              authStore.isTeacherMode ||
              authStore.isAdminMode
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
