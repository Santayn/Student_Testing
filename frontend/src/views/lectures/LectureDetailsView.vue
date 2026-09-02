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
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
  UiTable,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  listFromResponse,
} from '@/utils/apiData'

const route = useRoute()
const authStore = useAuthStore()

const lecture = ref(null)
const materials = ref([])
const tests = ref([])

const loading = ref(false)
const error = ref('')

const downloadingMaterialId =
  ref(null)

const canTakeTests = computed(() => {
  return (
    authStore.isStudent ||
    authStore.isAdmin
  )
})

const lectureId = computed(() => {
  return Number(
    route.params.lectureId
  )
})

const subjectId = computed(() => {
  const value =
    Number(
      route.query.subjectId
    )

  return (
    Number.isFinite(value) &&
    value > 0
  )
    ? value
    : null
})

const pageTitle = computed(() => {
  return (
    lecture.value?.title ||
    (
      lectureId.value
        ? `Лекция #${lectureId.value}`
        : 'Лекция'
    )
  )
})

const lectureMeta = computed(() => {
  const current =
    lecture.value

  if (!current) {
    return (
      'Материалы и тесты лекции.'
    )
  }

  const parts = []

  if (current.courseName) {
    parts.push(
      current.courseName
    )
  }

  if (
    Number(
      current.versionNumber
    ) > 0
  ) {
    parts.push(
      `версия ${current.versionNumber}`
    )
  }

  if (
    current.ordinal !==
      null &&
    current.ordinal !==
      undefined
  ) {
    parts.push(
      `лекция ${current.ordinal}`
    )
  }

  return (
    parts.join(', ') ||
    'Материалы и тесты лекции.'
  )
})

const backRoute = computed(() => {
  if (subjectId.value) {
    return {
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
    }
  }

  return {
    name: 'subjects',
  }
})

const materialColumns = [
  {
    key: 'fileName',
    label: 'Файл',
    value: (row) =>
      row.fileName ||
      `Материал #${row.id}`,
  },
  {
    key: 'contentType',
    label: 'Тип',
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

const testColumns = [
  {
    key: 'title',
    label: 'Тест',
    value: (row) =>
      row.title ||
      `Тест #${row.id}`,
  },
  {
    key: 'questionCount',
    label: 'Вопросов',
  },
  {
    key: 'attemptsAllowed',
    label: 'Попытки',
    value: (row) =>
      attemptsText(row),
    sortValue: (row) =>
      row.attemptsLeft ?? -1,
  },
  {
    key: 'availability',
    label: 'Доступность',
    value: (row) =>
      testStatusTitle(row),
    sortValue: (row) =>
      row.canResume ||
      row.canStartNew
        ? 1
        : 0,
  },
  {
    key: 'actions',
    label: 'Действия',
    sortable: false,
  },
]

function validPositiveId(value) {
  const id = Number(value)

  return (
    Number.isInteger(id) &&
    id > 0
  )
    ? id
    : null
}

function attemptsText(test) {
  const allowed =
    Number(test.attemptsAllowed) ||
    0

  if (!test.attemptsChecked) {
    return `— / ${allowed}`
  }

  return (
    `${test.attemptsUsed} / ${allowed}` +
    ` · осталось ${test.attemptsLeft}`
  )
}

function testStatusTitle(test) {
  if (!canTakeTests.value) {
    return 'Недоступен для роли'
  }

  if (!test.available) {
    return 'Недоступен'
  }

  if (test.availabilityCheckFailed) {
    return 'Проверка недоступна'
  }

  if (test.canResume) {
    return 'Можно продолжить'
  }

  if (test.canStartNew) {
    return 'Доступен'
  }

  if (test.attemptsChecked && test.attemptsLeft <= 0) {
    return 'Попытки закончились'
  }

  return 'Недоступен'
}

function testStatusDescription(test) {
  if (!canTakeTests.value) {
    return 'Для текущей роли прохождение тестов недоступно.'
  }

  if (!test.available) {
    return (
      test.statusMessage ||
      'Тест сейчас недоступен.'
    )
  }

  if (
    test.assignmentId === null ||
    test.assignmentId === undefined
  ) {
    return 'Для теста отсутствует доступное назначение.'
  }

  if (test.availabilityCheckFailed) {
    return (
      'Не удалось проверить использованные попытки. ' +
      'Запуск временно заблокирован.'
    )
  }

  if (test.canResume) {
    return (
      'Есть незавершённая попытка. ' +
      `Осталось новых: ${test.attemptsLeft}.`
    )
  }

  if (test.attemptsChecked && test.attemptsLeft <= 0) {
    return 'Доступные попытки закончились.'
  }

  if (test.canStartNew) {
    return `Осталось попыток: ${test.attemptsLeft}.`
  }

  return 'Тест сейчас недоступен.'
}

function withClientAvailability(source) {
  if (!Array.isArray(source)) {
    return []
  }

  return source.map((test) => {
      const assignmentId =
        validPositiveId(
          test.assignmentId
        )

      const attemptsAllowed =
        Math.max(
          0,
          Number(
            test.attemptsAllowed
          ) || 0
        )

      const attemptsLeft =
        Math.max(
          0,
          Number(
            test.attemptsRemaining
          ) || 0
        )

      const attemptsUsed =
        Math.max(
          0,
          attemptsAllowed -
            attemptsLeft
        )

      const canResume =
        Boolean(
          test.available &&
          test.canResume
        )

      const base = {
        ...test,
        attemptsChecked: true,
        attemptsUsed,
        attemptsLeft,
        canResume,
        canStartNew:
          Boolean(
            test.available &&
            !canResume &&
            attemptsLeft > 0
          ),
        availabilityCheckFailed: false,
      }

      /*
       * TEACHER не имеет tests.take в текущей ролевой модели.
       * USER вообще не должен попадать на lecture route.
       */
      if (!canTakeTests.value) {
        return {
          ...base,
          canResume: false,
          canStartNew: false,
        }
      }

      if (!test.available || !assignmentId) {
        return {
          ...base,
          canResume: false,
          canStartNew: false,
        }
      }

      return base
    })
}

function testRoute(test) {
  const query = {}

  if (
    test.assignmentId !==
      null &&
    test.assignmentId !==
      undefined
  ) {
    query.assignmentId =
      test.assignmentId
  }

  return {
    name: 'test',

    params: {
      testId: test.id,
    },

    query,
  }
}

function responseFilename(
  response,
  fallback
) {
  const disposition =
    response.headers?.[
      'content-disposition'
    ]

  if (!disposition) {
    return fallback
  }

  const encodedMatch =
    disposition.match(
      /filename\*=UTF-8''([^;]+)/i
    )

  if (encodedMatch?.[1]) {
    try {
      return decodeURIComponent(
        encodedMatch[1]
      )
    } catch {
      return encodedMatch[1]
    }
  }

  const simpleMatch =
    disposition.match(
      /filename="?([^";]+)"?/i
    )

  return (
    simpleMatch?.[1] ||
    fallback
  )
}

async function downloadMaterial(
  material
) {
  if (
    downloadingMaterialId.value
  ) {
    return
  }

  downloadingMaterialId.value =
    material.id

  error.value = ''

  try {
    const response =
      await learningApi
        .downloadMaterial(
          lectureId.value,
          material.id
        )

    const blob =
      response.data instanceof Blob
        ? response.data
        : new Blob(
            [response.data],
            {
              type:
                material.contentType ||
                undefined,
            }
          )

    const url =
      URL.createObjectURL(
        blob
      )

    const link =
      document.createElement(
        'a'
      )

    link.href = url

    link.download =
      responseFilename(
        response,
        material.fileName ||
          'lecture-material'
      )

    document.body.appendChild(
      link
    )

    link.click()
    link.remove()

    URL.revokeObjectURL(url)
  } catch (requestError) {
    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось скачать материал лекции.'
      )
  } finally {
    downloadingMaterialId.value =
      null
  }
}

async function loadLecture() {
  if (
    !Number.isFinite(
      lectureId.value
    ) ||
    lectureId.value <= 0
  ) {
    error.value =
      'Не указан корректный lectureId.'

    lecture.value = null
    materials.value = []
    tests.value = []

    return
  }

  loading.value = true
  error.value = ''

  try {
    const [
      lectureResponse,
      materialsResponse,
      testsResponse,
    ] = await Promise.all([
      learningApi.getLecture(
        lectureId.value
      ),

      learningApi
        .getLectureMaterials(
          lectureId.value
        ),

      learningApi.getLectureTests(
        lectureId.value
      ),
    ])

    lecture.value =
      lectureResponse.data ?? null

    materials.value =
      listFromResponse(
        materialsResponse
      )

    tests.value =
      withClientAvailability(
        listFromResponse(
          testsResponse
        )
      )
  } catch (requestError) {
    lecture.value = null
    materials.value = []
    tests.value = []

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить данные лекции.'
      )
  } finally {
    loading.value = false
  }
}

watch(
  lectureId,
  loadLecture
)

onMounted(loadLecture)
</script>

<template>
  <LecturesPageShell
    :title="pageTitle"
    :subtitle="lectureMeta"
    narrow
  >
    <template #actions>
      <UiButton
        :to="backRoute"
      >
        К списку лекций
      </UiButton>

      <UiButton
        :loading="loading"
        loading-text="Обновление..."
        @click="loadLecture"
      >
        Обновить
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
        !lecture
      "
      description="Загрузка лекции..."
    />

    <UiCard
      v-else-if="lecture"
      title="О лекции"
    >
      <dl class="lecture-data">
        <div class="lecture-data__row">
          <dt>Описание</dt>

          <dd>
            {{
              lecture.description ||
              '—'
            }}
          </dd>
        </div>

        <div class="lecture-data__row">
          <dt>Курс</dt>

          <dd>
            {{
              lecture.courseName ||
              '—'
            }}
          </dd>
        </div>

        <div class="lecture-data__row">
          <dt>Версия</dt>

          <dd>
            {{
              lecture.versionNumber ??
              '—'
            }}
          </dd>
        </div>

        <div class="lecture-data__row">
          <dt>Номер лекции</dt>

          <dd>
            {{
              lecture.ordinal ??
              '—'
            }}
          </dd>
        </div>
      </dl>
    </UiCard>

    <UiCard
      title="Материалы лекции"
      :description="
        materials.length
          ? `Файлов: ${materials.length}`
          : 'Прикреплённые материалы'
      "
    >
      <UiTable
        :columns="materialColumns"
        :rows="materials"
        :loading="loading"
        loading-message="Загрузка материалов..."
        empty-message="Для этой лекции пока нет прикреплённых материалов."
        :default-sort="{
          key: 'fileName',
          direction: 'asc',
        }"
      >
        <template #cell-fileName="{ row }">
          <strong>
            {{
              row.fileName ||
              `Материал #${row.id}`
            }}
          </strong>
        </template>

        <template #cell-contentType="{ row }">
          {{
            row.contentType ||
            'Файл'
          }}
        </template>

        <template #cell-actions="{ row }">
          <UiButton
            size="sm"
            :loading="
              downloadingMaterialId ===
              row.id
            "
            loading-text="Скачивание..."
            @click="
              downloadMaterial(row)
            "
          >
            Скачать
          </UiButton>
        </template>
      </UiTable>
    </UiCard>

    <UiCard
      title="Тесты по лекции"
      :description="
        tests.length
          ? `Тестов: ${tests.length}`
          : 'Опубликованные тесты'
      "
    >
      <UiTable
        :columns="testColumns"
        :rows="tests"
        :loading="loading"
        loading-message="Загрузка тестов..."
        empty-message="Для этой лекции пока нет опубликованных тестов."
        :default-sort="{
          key: 'title',
          direction: 'asc',
        }"
      >
        <template #cell-title="{ row }">
          <div class="lecture-test">
            <strong>
              {{
                row.title ||
                `Тест #${row.id}`
              }}
            </strong>

            <span
              v-if="row.description"
              class="lecture-test__description"
            >
              {{ row.description }}
            </span>
          </div>
        </template>

        <template #cell-questionCount="{ row }">
          {{
            row.questionCount ??
            '—'
          }}
        </template>

        <template #cell-attemptsAllowed="{ row }">
          {{ attemptsText(row) }}
        </template>

        <template #cell-availability="{ row }">
          <div class="lecture-test">
            <strong>
              {{ testStatusTitle(row) }}
            </strong>

            <span class="lecture-test__description">
              {{ testStatusDescription(row) }}
            </span>
          </div>
        </template>

        <template #cell-actions="{ row }">
          <UiButton
            v-if="
              row.canResume ||
              row.canStartNew
            "
            size="sm"
            variant="primary"
            :to="testRoute(row)"
          >
            {{
              row.canResume
                ? 'Продолжить тест'
                : 'Пройти тест'
            }}
          </UiButton>

          <UiButton
            v-else
            size="sm"
            disabled
          >
            {{ testStatusTitle(row) }}
          </UiButton>
        </template>
      </UiTable>
    </UiCard>
  </LecturesPageShell>
</template>

<style scoped>
.lecture-data {
  margin: 0;

  display: grid;
}

.lecture-data__row {
  padding: 12px 0;

  display: grid;
  grid-template-columns:
    minmax(130px, 190px)
    minmax(0, 1fr);
  gap: 14px;

  border-bottom:
    1px solid var(--border);
}

.lecture-data__row:first-child {
  padding-top: 0;
}

.lecture-data__row:last-child {
  padding-bottom: 0;

  border-bottom: 0;
}

.lecture-data dt {
  color: var(--text-secondary);

  font-size: 13px;
  font-weight: 700;
}

.lecture-data dd {
  margin: 0;

  color: var(--text);

  line-height: 1.5;
  overflow-wrap: anywhere;
}

.lecture-test {
  min-width: 0;

  display: grid;
  gap: 4px;
}

.lecture-test__description {
  color: var(--text-secondary);

  font-size: 12px;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

@media (max-width: 560px) {
  .lecture-data__row {
    grid-template-columns: 1fr;
    gap: 4px;
  }
}
</style>
