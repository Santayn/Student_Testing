<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  resultsApi,
} from '@/api'

import ResultAttemptCard from '@/components/results/ResultAttemptCard.vue'
import ResultsPageShell from '@/components/results/ResultsPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
  UiSelect,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  listFromResponse,
} from '@/utils/apiData'

const authStore =
  useAuthStore()

const loadingInitial = ref(false)
const loadingResults = ref(false)
const loadingOptions = ref(false)

const error = ref('')

const subjects = ref([])
const lectures = ref([])
const tests = ref([])
const groups = ref([])
const students = ref([])

const subjectId = ref('')
const lectureId = ref('')
const testId = ref('')
const groupId = ref('')
const studentId = ref('')

const resultData = ref(null)

const teacherMode = computed(() => {
  return (
    authStore.isTeacher ||
    authStore.isAdmin
  )
})

const resultMode = computed(() => {
  return teacherMode.value
    ? 'teacher'
    : 'student'
})

const pageSubtitle = computed(() => {
  if (teacherMode.value) {
    return (
      'Выберите предмет, лекцию, тест, группу и студента, ' +
      'чтобы получить результаты в виде раскрывающихся попыток.'
    )
  }

  return (
    'Здесь отображаются только ваши собственные результаты тестирования. ' +
    'Можно выбрать предмет и конкретный тест; итог теста считается по лучшей попытке.'
  )
})

const attempts = computed(() => {
  const value =
    resultData.value?.attempts

  return Array.isArray(value)
    ? value
    : []
})

function numericStat(
  attempt,
  key
) {
  const value =
    Number(
      attempt?.stats?.[key]
    )

  return Number.isFinite(value)
    ? value
    : 0
}

function completedTime(attempt) {
  const time =
    new Date(
      attempt?.completedAt ?? 0
    ).getTime()

  return Number.isFinite(time)
    ? time
    : 0
}

function isBetterAttempt(
  candidate,
  current
) {
  if (!current) {
    return true
  }

  const candidatePercent =
    numericStat(
      candidate,
      'percent'
    )

  const currentPercent =
    numericStat(
      current,
      'percent'
    )

  if (
    candidatePercent !==
    currentPercent
  ) {
    return (
      candidatePercent >
      currentPercent
    )
  }

  const candidateRight =
    numericStat(
      candidate,
      'right'
    )

  const currentRight =
    numericStat(
      current,
      'right'
    )

  if (
    candidateRight !==
    currentRight
  ) {
    return (
      candidateRight >
      currentRight
    )
  }

  const candidateTime =
    completedTime(candidate)

  const currentTime =
    completedTime(current)

  if (
    candidateTime !==
    currentTime
  ) {
    return (
      candidateTime >
      currentTime
    )
  }

  return (
    Number(
      candidate?.attemptOrdinal
    ) || 0
  ) > (
    Number(
      current?.attemptOrdinal
    ) || 0
  )
}

const studentBestAttempt = computed(() => {
  if (
    teacherMode.value ||
    !attempts.value.length
  ) {
    return null
  }

  const testIds =
    new Set(
      attempts.value
        .map(
          (attempt) =>
            Number(
              attempt.testId
            )
        )
        .filter(
          (id) =>
            Number.isInteger(id) &&
            id > 0
        )
    )

  /*
   * Не сравниваем между собой результаты разных тестов.
   * Без выбранного testId сводка показывается только если
   * backend вернул попытки ровно одного теста.
   */
  if (
    !testId.value &&
    testIds.size > 1
  ) {
    return null
  }

  return attempts.value.reduce(
    (best, attempt) =>
      isBetterAttempt(
        attempt,
        best
      )
        ? attempt
        : best,
    null
  )
})

const stats = computed(() => {
  if (teacherMode.value) {
    return (
      resultData.value?.stats ?? {
        total: 0,
        right: 0,
        percent: 0,
      }
    )
  }

  return (
    studentBestAttempt.value
      ?.stats ?? {
      total: 0,
      right: 0,
      percent: 0,
    }
  )
})

const attemptCount = computed(() => {
  return (
    resultData.value
      ?.attemptCount ??
    attempts.value.length
  )
})

const breadcrumbs = computed(() => {
  const parts = []

  if (
    !teacherMode.value &&
    subjectId.value
  ) {
    const subject =
      subjects.value.find(
        (item) =>
          String(item.id) ===
          String(subjectId.value)
      )

    if (subject) {
      parts.push(
        `Предмет: ${
          subject.name ||
          `#${subject.id}`
        }`
      )
    }
  }

  if (
    resultData.value
      ?.selectedTestName
  ) {
    parts.push(
      `Тест: ${
        resultData.value
          .selectedTestName
      }`
    )
  }

  if (
    resultData.value
      ?.selectedGroupName
  ) {
    parts.push(
      `Группа: ${
        resultData.value
          .selectedGroupName
      }`
    )
  }

  if (
    teacherMode.value &&
    resultData.value
      ?.selectedStudentName
  ) {
    parts.push(
      `Студент: ${
        resultData.value
          .selectedStudentName
      }`
    )
  }

  if (!teacherMode.value) {
    parts.push(
      'Режим студента: только ваши результаты'
    )
  }

  return parts.join(' · ')
})

const statsMessage = computed(() => {
  if (!resultData.value) {
    return teacherMode.value
      ? 'Выберите фильтры и нажмите «Показать результаты».'
      : 'Загрузка ваших результатов...'
  }

  if (teacherMode.value) {
    return (
      `Найдено попыток: ${attemptCount.value}. ` +
      `Правильных ответов: ${stats.value.right ?? 0} ` +
      `из ${stats.value.total ?? 0} ` +
      `(${stats.value.percent ?? 0}%).`
    )
  }

  if (!attempts.value.length) {
    return 'Завершённых попыток по выбранным фильтрам нет.'
  }

  if (!studentBestAttempt.value) {
    return (
      `Найдено попыток: ${attemptCount.value}. ` +
      'Выберите конкретный тест — итог будет показан по его лучшей попытке, ' +
      'а не как сумма результатов разных попыток и тестов.'
    )
  }

  return (
    `Лучшая попытка: №${
      studentBestAttempt.value.attemptOrdinal ?? '—'
    }. ` +
    `${stats.value.right ?? 0} из ${stats.value.total ?? 0} ` +
    `(${stats.value.percent ?? 0}%). ` +
    `Всего завершённых попыток по тесту: ${attemptCount.value}.`
  )
})

function subjectLabel(subject) {
  return (
    subject.name ||
    `Предмет #${subject.id}`
  )
}

function lectureLabel(lecture) {
  const ordinal =
    lecture.ordinal ??
    '—'

  const title =
    lecture.title ||
    `Лекция #${lecture.id}`

  const course =
    lecture.courseName ||
    'курс не указан'

  const version =
    lecture.versionNumber ??
    '—'

  return (
    `${ordinal}. ${title} ` +
    `(${course}, v${version})`
  )
}

function testLabel(test) {
  return (
    test.title ||
    `Тест #${test.id}`
  )
}

function groupLabel(group) {
  return (
    group.name ||
    group.code ||
    `Группа #${group.id}`
  )
}

function studentLabel(student) {
  return (
    student.fullName ||
    `Студент #${student.id}`
  )
}

function studentTestOptionsFromData(data) {
  const source =
    Array.isArray(data?.attempts)
      ? data.attempts
      : []

  const unique = new Map()

  source.forEach((attempt) => {
    const id =
      Number(attempt.testId)

    if (
      !Number.isInteger(id) ||
      id <= 0 ||
      unique.has(id)
    ) {
      return
    }

    unique.set(id, {
      id,
      title:
        attempt.testName ||
        `Тест #${id}`,
    })
  })

  return Array.from(
    unique.values()
  ).sort(
    (left, right) =>
      String(left.title).localeCompare(
        String(right.title),
        'ru',
        {
          sensitivity: 'base',
        }
      )
  )
}

function selectedStudentTestIsValid() {
  if (!testId.value) {
    return true
  }

  return tests.value.some(
    (test) =>
      String(test.id) ===
      String(testId.value)
  )
}

function applyStudentResultData(data) {
  resultData.value =
    data ?? {
      stats: {
        total: 0,
        right: 0,
        percent: 0,
      },
      attempts: [],
    }
}

function resetAfterSubject() {
  lectureId.value = ''
  testId.value = ''
  groupId.value = ''
  studentId.value = ''

  lectures.value = []
  tests.value = []
  groups.value = []
  students.value = []
}

function resetAfterLecture() {
  testId.value = ''
  groupId.value = ''
  studentId.value = ''

  tests.value = []
  groups.value = []
  students.value = []
}

function resetAfterTest() {
  groupId.value = ''
  studentId.value = ''

  groups.value = []
  students.value = []
}

function resetAfterGroup() {
  studentId.value = ''
  students.value = []
}

async function loadTeacherSubjects() {
  const response =
    await resultsApi
      .getTeacherSubjects()

  subjects.value =
    listFromResponse(response)
}

async function loadStudentSubjects() {
  const response =
    await resultsApi
      .getStudentSubjects()

  subjects.value =
    listFromResponse(response)
}

async function onSubjectChange() {
  error.value = ''

  if (!teacherMode.value) {
    testId.value = ''
    tests.value = []

    await loadStudentContextResults()
    return
  }

  resetAfterSubject()

  if (!subjectId.value) {
    return
  }

  loadingOptions.value = true

  try {
    const response =
      await resultsApi
        .getTeacherLectures(
          subjectId.value
        )

    lectures.value =
      listFromResponse(response)
  } catch (requestError) {
    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить лекции.'
      )
  } finally {
    loadingOptions.value = false
  }
}

async function onLectureChange() {
  resetAfterLecture()
  error.value = ''

  if (!lectureId.value) {
    return
  }

  loadingOptions.value = true

  try {
    const response =
      await resultsApi
        .getTeacherTests(
          lectureId.value
        )

    tests.value =
      listFromResponse(response)
  } catch (requestError) {
    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить тесты.'
      )
  } finally {
    loadingOptions.value = false
  }
}

async function onTestChange() {
  resetAfterTest()
  error.value = ''

  if (!testId.value) {
    return
  }

  loadingOptions.value = true

  try {
    const response =
      await resultsApi
        .getTeacherGroups(
          testId.value
        )

    groups.value =
      listFromResponse(response)
  } catch (requestError) {
    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить группы.'
      )
  } finally {
    loadingOptions.value = false
  }
}

async function onGroupChange() {
  resetAfterGroup()
  error.value = ''

  if (!groupId.value) {
    return
  }

  loadingOptions.value = true

  try {
    const response =
      await resultsApi
        .getTeacherStudents(
          groupId.value
        )

    students.value =
      listFromResponse(response)
  } catch (requestError) {
    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить студентов.'
      )
  } finally {
    loadingOptions.value = false
  }
}

function teacherParams() {
  const params = {}

  if (subjectId.value) {
    params.subjectId =
      subjectId.value
  }

  if (lectureId.value) {
    params.lectureId =
      lectureId.value
  }

  if (testId.value) {
    params.testId =
      testId.value
  }

  if (groupId.value) {
    params.groupId =
      groupId.value
  }

  if (studentId.value) {
    params.studentId =
      studentId.value
  }

  return params
}

function studentParams() {
  const params = {}

  if (subjectId.value) {
    params.subjectId =
      subjectId.value
  }

  if (testId.value) {
    params.testId =
      testId.value
  }

  return params
}

async function loadStudentContextResults() {
  loadingResults.value = true
  error.value = ''

  try {
    const params =
      subjectId.value
        ? {
            subjectId:
              subjectId.value,
          }
        : {}

    const response =
      await resultsApi
        .getStudentData(params)

    applyStudentResultData(
      response.data
    )

    tests.value =
      studentTestOptionsFromData(
        resultData.value
      )
  } catch (requestError) {
    resultData.value = null
    tests.value = []

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить результаты тестирования.'
      )
  } finally {
    loadingResults.value = false
  }
}

async function onStudentTestChange() {
  error.value = ''

  if (!testId.value) {
    await loadStudentContextResults()
    return
  }

  /*
   * Backend при subjectId + testId фактически отдаёт приоритет testId.
   * Поэтому обычный UI разрешает запрос только для testId,
   * который был получен из собственных результатов текущего
   * student/subject context.
   */
  if (!selectedStudentTestIsValid()) {
    error.value =
      'Выбранный тест не относится к текущему списку ваших результатов.'

    testId.value = ''
    return
  }

  await loadResults()
}

async function loadResults() {
  loadingResults.value = true
  error.value = ''

  try {
    if (
      !teacherMode.value &&
      testId.value &&
      !selectedStudentTestIsValid()
    ) {
      throw new Error(
        'Выбранный testId отсутствует в текущем списке результатов студента.'
      )
    }

    const response =
      teacherMode.value
        ? await resultsApi
            .getTeacherData(
              teacherParams()
            )
        : await resultsApi
            .getStudentData(
              studentParams()
            )

    resultData.value =
      response.data ?? {
        stats: {
          total: 0,
          right: 0,
          percent: 0,
        },
        attempts: [],
      }
  } catch (requestError) {
    resultData.value = null

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить результаты тестирования.'
      )
  } finally {
    loadingResults.value = false
  }
}

async function init() {
  loadingInitial.value = true
  error.value = ''

  try {
    if (teacherMode.value) {
      await loadTeacherSubjects()
    } else {
      await loadStudentSubjects()
      await loadStudentContextResults()
    }
  } catch (requestError) {
    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось инициализировать страницу результатов.'
      )
  } finally {
    loadingInitial.value = false
  }
}

onMounted(init)
</script>

<template>
  <ResultsPageShell
    title="Результаты тестов"
    :subtitle="pageSubtitle"
  >
    <template #actions>
      <UiButton
        :loading="
          loadingInitial ||
          loadingResults
        "
        loading-text="Обновление..."
        @click="init"
      >
        Обновить
      </UiButton>
    </template>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
    />

    <UiCard
      title="Фильтры"
      :description="
        teacherMode
          ? 'Фильтры преподавателя применяются последовательно.'
          : 'Можно выбрать предмет и тест. Сводка выбранного теста строится по лучшей попытке.'
      "
    >
      <div
        class="results-filters"
        :class="{
          'results-filters--student':
            !teacherMode,
        }"
      >
        <UiSelect
          v-model="subjectId"
          label="Предмет"
          :placeholder="
            teacherMode
              ? '-- выберите предмет --'
              : '-- все предметы --'
          "
          :options="subjects"
          :option-label="subjectLabel"
          option-value="id"
          :disabled="
            loadingInitial ||
            loadingOptions
          "
          @change="onSubjectChange"
        />

        <UiSelect
          v-if="!teacherMode"
          v-model="testId"
          label="Тест"
          placeholder="-- все тесты --"
          :options="tests"
          :option-label="testLabel"
          option-value="id"
          :disabled="
            loadingInitial ||
            loadingOptions ||
            loadingResults ||
            !tests.length
          "
          @change="onStudentTestChange"
        />

        <template v-if="teacherMode">
          <UiSelect
            v-model="lectureId"
            label="Лекция"
            placeholder="-- выберите лекцию --"
            :options="lectures"
            :option-label="lectureLabel"
            option-value="id"
            :disabled="
              !subjectId ||
              loadingOptions
            "
            @change="onLectureChange"
          />

          <UiSelect
            v-model="testId"
            label="Тест"
            placeholder="-- выберите тест --"
            :options="tests"
            :option-label="testLabel"
            option-value="id"
            :disabled="
              !lectureId ||
              loadingOptions
            "
            @change="onTestChange"
          />

          <UiSelect
            v-model="groupId"
            label="Группа"
            placeholder="-- выберите группу --"
            :options="groups"
            :option-label="groupLabel"
            option-value="id"
            :disabled="
              !testId ||
              loadingOptions
            "
            @change="onGroupChange"
          />

          <UiSelect
            v-model="studentId"
            label="Студент"
            placeholder="-- выберите студента --"
            :options="students"
            :option-label="studentLabel"
            option-value="id"
            :disabled="
              !groupId ||
              loadingOptions
            "
          />
        </template>

        <UiButton
          class="results-filters__submit"
          variant="primary"
          size="lg"
          :loading="loadingResults"
          :loading-text="
            teacherMode
              ? 'Загрузка результатов...'
              : 'Загрузка...'
          "
          @click="loadResults"
        >
          {{
            teacherMode
              ? 'Показать результаты'
              : 'Показать мои результаты'
          }}
        </UiButton>
      </div>
    </UiCard>

    <UiCard
      title="Сводка"
    >
      <div
        v-if="breadcrumbs"
        class="results-breadcrumbs"
      >
        {{ breadcrumbs }}
      </div>

      <UiAlert
        variant="info"
        :message="statsMessage"
      />

      <div
        v-if="
          resultData &&
          (teacherMode || studentBestAttempt)
        "
        class="results-stat-grid"
      >
        <div class="results-stat">
          <span>
            {{
              teacherMode
                ? 'Попыток'
                : 'Попыток по тесту'
            }}
          </span>

          <strong>
            {{ attemptCount }}
          </strong>
        </div>

        <div class="results-stat">
          <span>Правильных</span>

          <strong>
            {{ stats.right ?? 0 }}
          </strong>
        </div>

        <div class="results-stat">
          <span>Всего ответов</span>

          <strong>
            {{ stats.total ?? 0 }}
          </strong>
        </div>

        <div class="results-stat">
          <span>Процент</span>

          <strong>
            {{ stats.percent ?? 0 }}%
          </strong>
        </div>
      </div>
    </UiCard>

    <UiCard
      title="Попытки"
      :description="
        teacherMode
          ? 'Каждую попытку можно раскрыть и посмотреть ответы.'
          : 'Показаны все ваши завершённые попытки; лучшая попытка выбранного теста отмечена отдельно.'
      "
    >
      <UiEmptyState
        v-if="
          loadingResults &&
          !resultData
        "
        description="Загрузка результатов..."
      />

      <UiEmptyState
        v-else-if="
          resultData &&
          !attempts.length
        "
        :description="
          teacherMode
            ? 'По выбранным фильтрам данных не найдено.'
            : 'У вас пока нет завершённых попыток тестирования.'
        "
      />

      <div
        v-else-if="attempts.length"
        class="results-attempts"
      >
        <ResultAttemptCard
          v-for="(attempt, index) in attempts"
          :key="
            attempt.attemptId ??
            `${attempt.testId}-${attempt.studentId}-${index}`
          "
          :attempt="attempt"
          :mode="resultMode"
          :best="
            !teacherMode &&
            studentBestAttempt &&
            String(attempt.attemptId) ===
              String(studentBestAttempt.attemptId)
          "
          :open="
            attempts.length === 1 &&
            index === 0
          "
        />
      </div>

      <UiEmptyState
        v-else
        description="Выберите фильтры и загрузите результаты."
      />
    </UiCard>
  </ResultsPageShell>
</template>

<style scoped>
.results-filters {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: end;
}

.results-filters--student {
  grid-template-columns:
    repeat(2, minmax(0, 1fr))
    minmax(180px, 260px);
}

.results-filters__submit {
  align-self: end;
}

.results-breadcrumbs {
  margin-bottom: 12px;

  color: var(--text-secondary);

  font-size: 13px;
  line-height: 1.5;
}

.results-stat-grid {
  margin-top: 14px;

  display: grid;
  grid-template-columns:
    repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.results-stat {
  padding: 12px;

  display: grid;
  gap: 5px;

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 9px;
}

.results-stat span {
  color: var(--text-secondary);

  font-size: 12px;
  font-weight: 700;
}

.results-stat strong {
  font-size: 20px;
}

.results-attempts {
  display: grid;
  gap: 12px;
}

@media (max-width: 900px) {
  .results-stat-grid {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .results-filters,
  .results-filters--student {
    grid-template-columns: 1fr;
  }

  .results-filters__submit {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .results-stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
