<script setup>
import {
  computed,
  onMounted,
  ref,
} from 'vue'

import ResultAttemptCard from '@/components/results/ResultAttemptCard.vue'
import ResultsPageShell from '@/components/results/ResultsPageShell.vue'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiEmptyState,
  UiLoadingState,
  UiSelect,
  UiStatGrid,
  UiStat,
} from '@/components/ui'

import {
  useAuthStore,
} from '@/stores/auth'

import {
  useResultsFilters,
} from '@/composables/results/useResultsFilters'

import {
  useResultsData,
} from '@/composables/results/useResultsData'

import {
  useResultsContextOptions,
} from '@/composables/results/useResultsContextOptions'

import {
  attemptScoreSummary,
  bestAttempt,
} from '@/utils/resultScoring'

const authStore =
  useAuthStore()

const error = ref('')

const {
  subjects,
  lectures,
  tests,
  groups,
  students,
  subjectId,
  lectureId,
  testId,
  groupId,
  studentId,
  subjectLabel,
  lectureLabel,
  testLabel,
  groupLabel,
  studentLabel,
  resetAfterSubject,
  resetAfterLecture,
  resetAfterTest,
  resetAfterGroup,
  resetStudentSubject,
  resetInvalidStudentTest,
  setStudentTestOptions,
  selectedStudentTestIsValid,
  teacherParams,
  studentParams,
} = useResultsFilters()

const teacherMode = computed(() => {
  return (
    authStore.isTeacherMode ||
    authStore.isAdminMode
  )
})

const {
  loadingResults,
  resultData,
  invalidateResults,
  loadStudentContextResults,
  loadResults,
  onStudentTestChange,
  disposeResultsData,
} = useResultsData({
  teacherMode,
  subjectId,
  testId,
  teacherParams,
  studentParams,
  setStudentTestOptions,
  selectedStudentTestIsValid,
  resetInvalidStudentTest,
  error,
})

const {
  loadingInitial,
  loadingOptions,
  init,
  onSubjectChange,
  onLectureChange,
  onTestChange,
  onGroupChange,
} = useResultsContextOptions({
  authStore,
  teacherMode,
  subjects,
  lectures,
  tests,
  groups,
  students,
  subjectId,
  lectureId,
  testId,
  groupId,
  resetAfterSubject,
  resetAfterLecture,
  resetAfterTest,
  resetAfterGroup,
  resetStudentSubject,
  invalidateResults,
  loadStudentContextResults,
  disposeResultsData,
  error,
})

const resultMode = computed(() => {
  return teacherMode.value
    ? 'teacher'
    : 'student'
})

const pageSubtitle = computed(() => {
  if (teacherMode.value) {
    return (
      authStore.isAdminMode
        ? 'Администратор может выбрать любой предмет, лекцию, тест, группу и студента.'
        : 'Выберите предмет, лекцию, тест, группу и студента, чтобы получить результаты в виде раскрывающихся попыток.'
    )
  }

  return (
    'Ваши завершённые попытки и лучший результат по выбранному тесту.'
  )
})

const attempts = computed(() => {
  const value =
    resultData.value?.attempts

  return Array.isArray(value)
    ? value
    : []
})

const studentComparableAttempts = computed(() => {
  if (teacherMode.value) {
    return []
  }

  if (testId.value) {
    return attempts.value.filter(
      (attempt) =>
        String(attempt.testId) ===
        String(testId.value)
    )
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
   * Не сравниваем результаты разных тестов. Без выбранного testId
   * лучшая попытка определяется только если backend вернул попытки
   * одного теста.
   */
  return testIds.size <= 1
    ? attempts.value
    : []
})

const studentBestAttempt = computed(() => {
  return bestAttempt(
    studentComparableAttempts.value
  )
})

const studentBestScore = computed(() => {
  return attemptScoreSummary(
    studentBestAttempt.value
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

const displayedAttemptCount = computed(() => {
  if (teacherMode.value) {
    return attemptCount.value
  }

  return studentBestAttempt.value
    ? studentComparableAttempts.value.length
    : attemptCount.value
})

const displayedPercent = computed(() => {
  return teacherMode.value
    ? Number(stats.value.percent) || 0
    : studentBestScore.value.percent
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
          'без названия'
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
      `Найдено попыток: ${displayedAttemptCount.value}. ` +
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
    `Баллы: ${formatScoreNumber(studentBestScore.value.score)} ` +
    `из ${formatScoreNumber(studentBestScore.value.maxScore)} ` +
    `(${formatScoreNumber(studentBestScore.value.percent)}%). ` +
    `Полностью верных ответов: ${stats.value.right ?? 0} ` +
    `из ${stats.value.total ?? 0}. ` +
    `Всего завершённых попыток по тесту: ${displayedAttemptCount.value}.`
  )
})

function formatScoreNumber(value) {
  const number = Number(value)

  if (!Number.isFinite(number)) {
    return '0'
  }

  return new Intl.NumberFormat(
    'ru-RU',
    {
      maximumFractionDigits: 2,
    }
  ).format(number)
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
      :title="teacherMode ? 'Фильтры' : 'Фильтр результатов'"
      :description="
        teacherMode
          ? 'Фильтры преподавателя применяются последовательно.'
          : 'Необязательно: сузьте список до предмета или конкретного теста.'
      "
      :class="{ 'results-filter-card--student': !teacherMode }"
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
          :filter="true"
          filter-placeholder="Поиск по предметам"
          :disabled="loadingInitial"
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
          <div class="results-filter-group">
            <h3 class="results-filter-group__title">1. Контекст теста</h3>
            <div class="results-filter-group__controls">
              <UiSelect v-model="lectureId" label="Лекция" placeholder="-- выберите лекцию --" :options="lectures" :option-label="lectureLabel" option-value="id" :disabled="!subjectId || loadingOptions" @change="onLectureChange" />
              <UiSelect v-model="testId" label="Тест" placeholder="-- выберите тест --" :options="tests" :option-label="testLabel" option-value="id" :disabled="!lectureId || loadingOptions" @change="onTestChange" />
            </div>
          </div>
          <div class="results-filter-group">
            <h3 class="results-filter-group__title">2. Аудитория</h3>
            <div class="results-filter-group__controls">
              <UiSelect v-model="groupId" label="Группа" placeholder="-- выберите группу --" :options="groups" :option-label="groupLabel" option-value="id" :filter="true" filter-placeholder="Поиск по группам" :disabled="!testId || loadingOptions" @change="onGroupChange" />
              <UiSelect v-model="studentId" label="Студент" placeholder="-- выберите студента --" :options="students" :option-label="studentLabel" option-value="id" :filter="true" filter-placeholder="Поиск по студентам" :disabled="!groupId || loadingOptions" />
            </div>
          </div>
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
      v-if="!teacherMode || resultData"
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

      <UiStatGrid
        v-if="
          resultData &&
          (teacherMode || studentBestAttempt)
        "
      >
        <UiStat>
          <template #label>
            {{
              teacherMode
                ? 'Попыток'
                : 'Попыток по тесту'
            }}
          </template>

          {{ displayedAttemptCount }}
        </UiStat>

        <UiStat>
          <template #label>Правильных</template>

          <template v-if="teacherMode">
            {{ stats.right ?? 0 }}
          </template>
          <template v-else>
            {{ stats.right ?? 0 }}
            из
            {{ stats.total ?? 0 }}
          </template>
        </UiStat>

        <UiStat>
          <template #label>
            {{
              teacherMode
                ? 'Всего ответов'
                : 'Баллы'
            }}
          </template>

          <template v-if="teacherMode">
            {{ stats.total ?? 0 }}
          </template>
          <template v-else>
            {{ formatScoreNumber(studentBestScore.score) }}
            из
            {{ formatScoreNumber(studentBestScore.maxScore) }}
          </template>
        </UiStat>

        <UiStat label="Процент">
          {{ formatScoreNumber(displayedPercent) }}%
        </UiStat>
      </UiStatGrid>
    </UiCard>

    <UiCard
      title="Попытки"
      :description="
        teacherMode
          ? 'Каждую попытку можно раскрыть и посмотреть ответы.'
          : 'Показаны все ваши завершённые попытки; лучшая попытка выбранного теста отмечена отдельно.'
      "
    >
      <UiLoadingState
        v-if="loadingInitial"
        label="Загрузка страницы результатов..."
      />

      <UiLoadingState
        v-else-if="
          loadingResults &&
          !resultData
        "
        label="Загрузка результатов..."
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
    repeat(2, minmax(0, 1fr));
}

.results-filters--student .results-filters__submit {
  grid-column: 1 / -1;
  justify-self: start;
}

.results-filters__submit {
  align-self: end;
}

.results-breadcrumbs {
  margin-bottom: 12px;

  color: var(--st-text-secondary);

  font-size: 13px;
  line-height: 1.5;
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

  .results-filters__submit,
  .results-filters--student .results-filters__submit {
    width: 100%;
    grid-column: auto;
    justify-self: stretch;
  }
}

@media (max-width: 480px) {
  .results-stat-grid {
    grid-template-columns: 1fr;
  }
}

.results-filter-group { min-width: 0; padding: 12px; display: grid; gap: 10px; background: var(--st-surface-muted); border: 1px solid var(--st-border); border-radius: var(--st-radius-md); }
.results-filter-group__title { margin: 0; color: var(--st-text-secondary); font-size: var(--st-font-sm); line-height: var(--st-line-normal); }
.results-filter-group__controls { min-width: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
@media (max-width: 720px) { .results-filter-group__controls { grid-template-columns: 1fr; } }

</style>
