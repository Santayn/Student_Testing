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
  UiCard,
  UiEmptyState,
  UiTag,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  listFromResponse,
} from '@/utils/apiData'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

const route = useRoute()
const authStore = useAuthStore()

const lecture = ref(null)
const materials = ref([])
const tests = ref([])

const loading = ref(false)
const error = ref('')

const lectureRequest =
  createLatestRequestGuard()

const downloadingMaterialId =
  ref(null)

const canTakeTests = computed(() => {
  return authStore.isStudentMode
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

useBreadcrumbContext(() => {
  const current = lecture.value
  const currentLectureId =
    lectureId.value

  const matchesCurrentLecture =
    Number(current?.id) ===
    currentLectureId

  const loadedSubjectId =
    matchesCurrentLecture
      ? Number(current?.subjectId)
      : null

  return {
    lectureId: currentLectureId,
    lectureTitle:
      matchesCurrentLecture
        ? current?.title
        : null,
    subjectId:
      Number.isFinite(
        loadedSubjectId
      ) && loadedSubjectId > 0
        ? loadedSubjectId
        : subjectId.value,
  }
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

function testStatusVariant(test) {
  if (test.canResume || test.canStartNew) {
    return 'success'
  }

  if (test.availabilityCheckFailed) {
    return 'warning'
  }

  if (test.attemptsChecked && test.attemptsLeft <= 0) {
    return 'danger'
  }

  return 'secondary'
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

  if (subjectId.value) {
    query.subjectId =
      subjectId.value
  }

  if (lectureId.value) {
    query.lectureId =
      lectureId.value
  }

  if (route.query.facultyId) {
    query.facultyId =
      route.query.facultyId
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
  const requestId =
    lectureRequest.begin()

  const requestedLectureId =
    Number(lectureId.value)

  if (
    !Number.isFinite(
      requestedLectureId
    ) ||
    requestedLectureId <= 0
  ) {
    error.value =
      'Не указан корректный lectureId.'

    lecture.value = null
    materials.value = []
    tests.value = []
    loading.value = false

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
        requestedLectureId
      ),

      learningApi
        .getLectureMaterials(
          requestedLectureId
        ),

      learningApi.getLectureTests(
        requestedLectureId
      ),
    ])

    if (
      !lectureRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

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
    if (
      !lectureRequest.isCurrent(
        requestId
      )
    ) {
      return
    }

    lecture.value = null
    materials.value = []
    tests.value = []

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить данные лекции.'
      )
  } finally {
    if (
      lectureRequest.isCurrent(
        requestId
      )
    ) {
      loading.value = false
    }
  }
}

watch(
  lectureId,
  loadLecture
)

onMounted(loadLecture)
</script>

<template>
  <LecturesPageShell :title="pageTitle" :subtitle="lectureMeta" narrow>
    <template #actions>
      <UiButton :loading="loading" loading-text="Обновление..." @click="loadLecture">
        Обновить
      </UiButton>
    </template>

    <UiAlert v-if="error" variant="danger" :message="error" />

    <UiEmptyState
      v-if="loading && !lecture"
      title="Загружаем лекцию"
      description="Получаем описание, материалы и доступные тесты."
    />

    <template v-else-if="lecture">
      <UiCard compact>
        <div class="lecture-overview">
          <div class="lecture-overview__description">
            <span class="lecture-overview__eyebrow">О лекции</span>
            <p>{{ lecture.description || 'Описание лекции пока не заполнено.' }}</p>
          </div>

          <div class="lecture-overview__meta">
            <UiTag v-if="lecture.courseName" :value="lecture.courseName" />
            <UiTag
              v-if="lecture.versionNumber != null"
              variant="info"
              :value="`Версия ${lecture.versionNumber}`"
            />
            <UiTag
              v-if="lecture.ordinal != null"
              :value="`Лекция ${lecture.ordinal}`"
            />
          </div>
        </div>
      </UiCard>

      <UiCard
        title="Материалы"
        :description="materials.length ? `Файлов: ${materials.length}` : 'Прикреплённые материалы лекции'"
      >
        <UiEmptyState
          v-if="!materials.length"
          compact
          description="Для этой лекции пока нет прикреплённых материалов."
        />

        <div v-else class="lecture-materials">
          <article v-for="material in materials" :key="material.id" class="lecture-material">
            <div class="lecture-material__icon" aria-hidden="true"><i class="pi pi-file" /></div>
            <div class="lecture-material__copy">
              <strong>{{ material.fileName || `Материал #${material.id}` }}</strong>
              <UiTag :value="material.contentType || 'Файл'" />
            </div>
            <UiButton
              size="sm"
              :loading="downloadingMaterialId === material.id"
              loading-text="Скачивание..."
              @click="downloadMaterial(material)"
            >
              Скачать
            </UiButton>
          </article>
        </div>
      </UiCard>

      <UiCard
        title="Тесты"
        :description="tests.length ? `Доступно тестов: ${tests.length}` : 'Опубликованные тесты лекции'"
      >
        <UiEmptyState
          v-if="!tests.length"
          compact
          description="Для этой лекции пока нет опубликованных тестов."
        />

        <div v-else class="lecture-tests">
          <article v-for="testItem in tests" :key="testItem.id" class="lecture-test-card">
            <div class="lecture-test-card__header">
              <div class="lecture-test-card__copy">
                <h3>{{ testItem.title || `Тест #${testItem.id}` }}</h3>
                <p v-if="testItem.description">{{ testItem.description }}</p>
              </div>
              <UiTag
                :variant="testStatusVariant(testItem)"
                :value="testStatusTitle(testItem)"
              />
            </div>

            <div class="lecture-test-card__meta">
              <span><strong>{{ testItem.questionCount ?? '—' }}</strong> вопросов</span>
              <span><strong>{{ attemptsText(testItem) }}</strong></span>
            </div>

            <p class="lecture-test-card__status">{{ testStatusDescription(testItem) }}</p>

            <UiButton
              v-if="testItem.canResume || testItem.canStartNew"
              variant="primary"
              :to="testRoute(testItem)"
            >
              {{ testItem.canResume ? 'Продолжить тест' : 'Пройти тест' }}
            </UiButton>

            <UiButton v-else disabled>
              {{ testStatusTitle(testItem) }}
            </UiButton>
          </article>
        </div>
      </UiCard>
    </template>
  </LecturesPageShell>
</template>

<style scoped>
.lecture-overview { display: grid; gap: 14px; }
.lecture-overview__description { min-width: 0; display: grid; gap: 7px; }
.lecture-overview__eyebrow { color: var(--st-text-muted); font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: .04em; }
.lecture-overview p { margin: 0; color: var(--st-text-secondary); line-height: 1.65; overflow-wrap: anywhere; }
.lecture-overview__meta { display: flex; flex-wrap: wrap; gap: 7px; }

.lecture-materials { display: grid; gap: 10px; }
.lecture-material {
  min-width: 0;
  padding: 12px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-control);
}
.lecture-material__icon { width: 40px; height: 40px; display: grid; place-items: center; color: var(--st-primary-soft-text); background: var(--st-primary-soft); border-radius: 10px; }
.lecture-material__copy { min-width: 0; display: grid; justify-items: start; gap: 7px; }
.lecture-material__copy strong { max-width: 100%; color: var(--st-text); overflow-wrap: anywhere; }

.lecture-tests { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 310px), 1fr)); gap: 12px; }
.lecture-test-card {
  min-width: 0;
  padding: 16px;
  display: grid;
  align-content: start;
  gap: 14px;
  background: var(--st-surface-muted);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
}
.lecture-test-card__header { min-width: 0; display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.lecture-test-card__copy { min-width: 0; display: grid; gap: 6px; }
.lecture-test-card h3, .lecture-test-card p { margin: 0; }
.lecture-test-card h3 { color: var(--st-text); font-size: 16px; line-height: 1.35; overflow-wrap: anywhere; }
.lecture-test-card__copy p, .lecture-test-card__status { color: var(--st-text-secondary); font-size: 13px; line-height: 1.5; overflow-wrap: anywhere; }
.lecture-test-card__meta { display: flex; flex-wrap: wrap; gap: 8px 14px; color: var(--st-text-secondary); font-size: 12px; }
.lecture-test-card__meta strong { color: var(--st-text); }
.lecture-test-card :deep(.st-ui-link-button), .lecture-test-card :deep(.st-ui-button) { justify-self: start; }

@media (max-width: 560px) {
  .lecture-material { grid-template-columns: auto minmax(0, 1fr); }
  .lecture-material :deep(.st-ui-button) { grid-column: 1 / -1; width: 100%; }
  .lecture-test-card__header { flex-direction: column; }
  .lecture-test-card :deep(.st-ui-link-button), .lecture-test-card :deep(.st-ui-button) { width: 100%; justify-content: center; }
}
</style>
