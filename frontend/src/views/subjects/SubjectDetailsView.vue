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
  UiTag,
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
  <SubjectsPageShell :title="pageTitle" :subtitle="pageSubtitle" narrow>
    <UiAlert v-if="error" variant="danger" :message="error" />

    <UiEmptyState
      v-if="loading && !subject"
      title="Загружаем предмет"
      description="Получаем описание и доступные разделы."
    />

    <template v-else-if="subject">
      <UiCard compact>
        <div class="subject-hero">
          <div class="subject-hero__icon" aria-hidden="true"><i class="pi pi-book" /></div>
          <div class="subject-hero__copy">
            <div class="subject-hero__meta">
              <UiTag :value="`ID ${subject.id ?? '—'}`" />
            </div>
            <p>{{ subject.description || 'Описание предмета пока не заполнено.' }}</p>
          </div>
        </div>
      </UiCard>

      <section class="subject-destinations">
        <article class="subject-destination">
          <div>
            <span class="subject-destination__eyebrow">Учебный раздел</span>
            <h2>Лекции</h2>
            <p>Материалы, опубликованные лекции и доступные тесты по предмету.</p>
          </div>
          <UiButton
            variant="primary"
            :to="authStore.isTeacherMode || authStore.isAdminMode ? teacherLecturesRoute : studentLecturesRoute"
          >
            Открыть лекции
          </UiButton>
        </article>

        <article
          v-if="authStore.isTeacherMode || authStore.isAdminMode"
          class="subject-destination"
        >
          <div>
            <span class="subject-destination__eyebrow">Структура предмета</span>
            <h2>Тематики</h2>
            <p>Управление тематикой, к которой привязываются вопросы и учебный контент.</p>
          </div>
          <UiButton :to="teacherTopicsRoute">Открыть тематики</UiButton>
        </article>
      </section>
    </template>
  </SubjectsPageShell>
</template>

<style scoped>
.subject-hero {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.subject-hero__icon {
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 14px;
  font-size: 21px;
}

.subject-hero__copy {
  min-width: 0;
  display: grid;
  gap: 10px;
}

.subject-hero__meta { display: flex; flex-wrap: wrap; gap: 8px; }
.subject-hero p { margin: 0; color: var(--st-text-secondary); line-height: 1.65; overflow-wrap: anywhere; }

.subject-destinations {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 280px), 1fr));
  gap: 14px;
}

.subject-destination {
  min-width: 0;
  padding: 18px;
  display: grid;
  align-content: space-between;
  gap: 18px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.subject-destination__eyebrow { color: var(--st-text-muted); font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; }
.subject-destination h2 { margin: 5px 0 0; color: var(--st-text); font-size: 19px; }
.subject-destination p { margin: 8px 0 0; color: var(--st-text-secondary); font-size: 13px; line-height: 1.55; }
.subject-destination :deep(.st-ui-link-button) { justify-self: start; }

@media (max-width: 480px) {
  .subject-hero { grid-template-columns: 1fr; }
  .subject-destination :deep(.st-ui-link-button) { width: 100%; justify-content: center; }
}
</style>
