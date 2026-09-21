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
} from '@/api'

import LecturesPageShell from '@/components/lectures/LecturesPageShell.vue'

import {
  useBreadcrumbContext,
} from '@/navigation'

import {
  UiAlert,
  UiButton,
  UiEmptyState,
  UiTag,
} from '@/components/ui'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const route = useRoute()

const subject = ref(null)
const lectures = ref([])

const loading = ref(false)
const error = ref('')

const lecturesRequest =
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
  if (subject.value?.name) {
    return (
      `Лекции: ` +
      subject.value.name
    )
  }

  return 'Лекции'
})

const pageSubtitle = computed(() => {
  if (!subject.value) {
    return (
      'Доступные лекции предмета.'
    )
  }

  const subjectName =
    subject.value.name ||
    `Предмет #${subject.value.id}`

  return (
    `${subjectName}. ` +
    `Лекций: ${lectures.value.length}`
  )
})

function lectureRoute(lecture) {
  return {
    name: 'lecture-details',

    params: {
      lectureId: lecture.id,
    },

    query: {
      subjectId:
        subjectId.value,

      ...(route.query.facultyId
        ? {
            facultyId:
              route.query.facultyId,
          }
        : {}),
    },
  }
}

async function loadLectures() {
  const requestId =
    lecturesRequest.begin()

  const requestedSubjectId =
    Number(subjectId.value)

  if (
    !Number.isFinite(
      requestedSubjectId
    ) ||
    requestedSubjectId <= 0
  ) {
    error.value =
      'Не указан корректный subjectId.'

    subject.value = null
    lectures.value = []
    loading.value = false

    return
  }

  loading.value = true
  error.value = ''

  try {
    const [
      subjectResponse,
      lecturesResponse,
    ] = await Promise.all([
      learningApi.getSubject(
        requestedSubjectId
      ),

      learningApi
        .getSubjectLectures(
          requestedSubjectId
        ),
    ])

    if (
      !lecturesRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    subject.value =
      subjectResponse.data ?? null

    lectures.value =
      listFromResponse(
        lecturesResponse
      )
  } catch (requestError) {
    if (
      !lecturesRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    subject.value = null
    lectures.value = []

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить список лекций.'
      )
  } finally {
    if (
      lecturesRequest.isCurrent(
        requestId
      )
    ) {
      loading.value = false
    }
  }
}

watch(
  subjectId,
  loadLectures
)

onMounted(loadLectures)
</script>

<template>
  <LecturesPageShell :title="pageTitle" :subtitle="pageSubtitle">
    <template #actions>
      <UiButton :loading="loading" loading-text="Обновление..." @click="loadLectures">
        Обновить
      </UiButton>
    </template>

    <UiAlert v-if="error" variant="danger" :message="error" />

    <UiEmptyState
      v-if="loading && !lectures.length"
      title="Загружаем лекции"
      description="Получаем доступные материалы предмета."
    />

    <UiEmptyState
      v-else-if="!error && !lectures.length"
      title="Лекций пока нет"
      description="Для этого предмета ещё не опубликованы доступные лекции."
    />

    <div v-else class="lectures-grid">
      <article v-for="lecture in lectures" :key="lecture.id" class="lecture-tile">
        <div class="lecture-tile__top">
          <div class="lecture-tile__ordinal">{{ lecture.ordinal ?? '—' }}</div>
          <div class="lecture-tile__copy">
            <h2>{{ lecture.title || `Лекция #${lecture.id}` }}</h2>
            <div class="lecture-tile__meta">
              <UiTag v-if="lecture.courseName" :value="lecture.courseName" />
              <UiTag
                v-if="lecture.versionNumber != null"
                variant="info"
                :value="`Версия ${lecture.versionNumber}`"
              />
            </div>
          </div>
        </div>

        <UiButton variant="primary" :to="lectureRoute(lecture)">
          Открыть лекцию
        </UiButton>
      </article>
    </div>
  </LecturesPageShell>
</template>

<style scoped>
.lectures-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 300px), 1fr));
  gap: 14px;
}

.lecture-tile {
  min-width: 0;
  padding: 18px;
  display: grid;
  align-content: space-between;
  gap: 20px;
  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-card);
}

.lecture-tile__top {
  min-width: 0;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 13px;
  align-items: start;
}

.lecture-tile__ordinal {
  min-width: 44px;
  height: 44px;
  padding: 0 10px;
  display: grid;
  place-items: center;
  color: var(--st-primary-soft-text);
  background: var(--st-primary-soft);
  border-radius: 12px;
  font-weight: 800;
}

.lecture-tile__copy { min-width: 0; display: grid; gap: 10px; }
.lecture-tile h2 { margin: 0; color: var(--st-text); font-size: 17px; line-height: 1.35; overflow-wrap: anywhere; }
.lecture-tile__meta { display: flex; flex-wrap: wrap; gap: 6px; }
.lecture-tile :deep(.st-ui-link-button) { justify-self: start; }

@media (max-width: 480px) {
  .lecture-tile :deep(.st-ui-link-button) { width: 100%; justify-content: center; }
}
</style>
