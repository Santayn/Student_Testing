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

import SubjectsPageShell from '@/components/subjects/SubjectsPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiTable,
} from '@/components/ui'

import {
  listFromResponse,
} from '@/utils/apiData'

const route = useRoute()

const subject = ref(null)
const lectures = ref([])

const loading = ref(false)
const error = ref('')

const subjectId = computed(() => {
  return Number(
    route.params.subjectId
  )
})

const pageTitle = computed(() => {
  if (subject.value?.name) {
    return `Лекции: ${subject.value.name}`
  }

  return 'Лекции'
})

const pageSubtitle = computed(() => {
  if (!subject.value) {
    return (
      'Доступные лекции предмета.'
    )
  }

  return (
    `${subject.value.name ||
      `Предмет #${subject.value.id}`}. ` +
    `Лекций: ${lectures.value.length}`
  )
})

const backRoute = computed(() => ({
  name: 'subject-details',

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

const columns = [
  {
    key: 'ordinal',
    label: '№',
  },
  {
    key: 'title',
    label: 'Лекция',
    value: (row) =>
      row.title ||
      `Лекция #${row.id}`,
  },
  {
    key: 'courseName',
    label: 'Курс',
  },
  {
    key: 'versionNumber',
    label: 'Версия',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

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
  if (
    !Number.isFinite(
      subjectId.value
    ) ||
    subjectId.value <= 0
  ) {
    error.value =
      'Не указан корректный subjectId.'

    subject.value = null
    lectures.value = []

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
        subjectId.value
      ),

      learningApi
        .getSubjectLectures(
          subjectId.value
        ),
    ])

    subject.value =
      subjectResponse.data ?? null

    lectures.value =
      listFromResponse(
        lecturesResponse
      )
  } catch (requestError) {
    subject.value = null
    lectures.value = []

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить список лекций.'
      )
  } finally {
    loading.value = false
  }
}

watch(
  subjectId,
  loadLectures
)

onMounted(loadLectures)
</script>

<template>
  <SubjectsPageShell
    :title="pageTitle"
    :subtitle="pageSubtitle"
  >
    <template #actions>
      <UiButton
        :to="backRoute"
      >
        К предмету
      </UiButton>

      <UiButton
        :loading="loading"
        loading-text="Обновление..."
        @click="loadLectures"
      >
        Обновить
      </UiButton>
    </template>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
    />

    <UiCard title="Лекции">
      <UiTable
        :columns="columns"
        :rows="lectures"
        :loading="loading"
        loading-message="Загрузка лекций..."
        empty-message="Для этого предмета пока нет доступных лекций."
        :default-sort="{
          key: 'ordinal',
          direction: 'asc',
        }"
      >
        <template #cell-title="{ row }">
          <strong>
            {{
              row.title ||
              `Лекция #${row.id}`
            }}
          </strong>
        </template>

        <template #cell-courseName="{ row }">
          {{ row.courseName || '—' }}
        </template>

        <template #cell-versionNumber="{ row }">
          {{
            row.versionNumber ??
            '—'
          }}
        </template>

        <template #cell-actions="{ row }">
          <UiButton
            size="sm"
            variant="primary"
            :to="lectureRoute(row)"
          >
            Открыть
          </UiButton>
        </template>
      </UiTable>
    </UiCard>
  </SubjectsPageShell>
</template>
