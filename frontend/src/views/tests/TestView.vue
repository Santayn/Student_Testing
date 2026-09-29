<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'

import {
  onBeforeRouteLeave,
  useRoute,
} from 'vue-router'

import TestsPageShell from '@/components/tests/TestsPageShell.vue'
import StudentMatchingQuestion from '@/components/tests/StudentMatchingQuestion.vue'
import ResultMatchingPairs from '@/components/results/ResultMatchingPairs.vue'

import {
  useBreadcrumbContext,
} from '@/navigation'

import {
  parseMatchingDisplay,
} from '@/utils/matchingPairs'

import {
  isQuestionAnswered,
  testAnswerProgress,
} from '@/utils/testAnswerProgress'

import {
  useAttemptDraft,
} from '@/composables/tests/useAttemptDraft'

import {
  useTestAttemptLifecycle,
} from '@/composables/tests/useTestAttemptLifecycle'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiDialog,
  UiEmptyState,
  UiLoadingState,
  UiInput,
  UiRadio,
  UiStat,
  UiStatGrid,
  UiTag,
} from '@/components/ui'

const route = useRoute()

let attemptDraft = null

const singleAnswers =
  reactive({})

const multipleAnswers =
  reactive({})

const textAnswers =
  reactive({})

const matchingAnswers =
  reactive({})

const showIncompleteSubmitDialog = ref(false)

const testId = computed(() => {
  const value =
    Number(
      route.params.testId
    )

  return (
    Number.isFinite(value) &&
    value > 0
  )
    ? value
    : null
})

const assignmentId = computed(() => {
  const value =
    Number(
      route.query.assignmentId
    )

  return (
    Number.isFinite(value) &&
    value > 0
  )
    ? value
    : null
})

const {
  loading,
  submitting,
  submitOutcomeUnknown,
  error,
  test,
  questions,
  attemptId,
  resultData,
  submitted,
  loadTest,
  submitTest,
  leaveCurrentAttempt,
  disposeAttemptLifecycle,
} = useTestAttemptLifecycle({
  getRouteContext: currentRouteContext,
  isSameRouteContext,
  resetAnswers,
  initializeAttemptAnswers: (context) => (
    attemptDraft?.initializeAttemptAnswers(
      context
    )
  ),
  buildSubmission,
  beforeSubmit: () => (
    attemptDraft?.flushScheduledDraftPersistence()
  ),
  clearAttemptDraft: (context) => (
    attemptDraft?.clearDraftForContext(
      context
    )
  ),
  cancelDraftPersistence: () => (
    attemptDraft?.cancelScheduledDraftPersistence()
  ),
})

attemptDraft = useAttemptDraft({
  getRouteContext: currentRouteContext,
  getAttemptId: () => attemptId.value,
  getQuestions: () => questions.value,
  isSubmitted: () => submitted.value,
  singleAnswers,
  multipleAnswers,
  textAnswers,
  matchingAnswers,
  initializeAnswers,
})

const {
  flushScheduledDraftPersistence,
} = attemptDraft

useBreadcrumbContext(() => ({
  testId: testId.value,
  testTitle:
    Number(test.value?.id) ===
    testId.value
      ? test.value?.title
      : null,
  lectureId:
    Number(route.query.lectureId) ||
    null,
  subjectId:
    Number(route.query.subjectId) ||
    null,
}))

const pageTitle = computed(() => {
  return (
    test.value?.title ||
    (
      testId.value
        ? 'Тест без названия'
        : 'Прохождение теста'
    )
  )
})

const pageSubtitle = computed(() => {
  if (!test.value) {
    return (
      'Загрузка теста...'
    )
  }

  const questionCount =
    submitted.value
      ? (
          resultData.value?.totalCount ??
          questions.value.length
        )
      : questions.value.length

  return (
    `Вопросов: ${questionCount}, ` +
    `разрешено попыток: ${
      test.value.attemptsAllowed ??
      '—'
    }`
  )
})

function clearObject(object) {
  Object.keys(object).forEach(
    (key) => {
      delete object[key]
    }
  )
}

function resetAnswers() {
  clearObject(singleAnswers)
  clearObject(multipleAnswers)
  clearObject(textAnswers)
  clearObject(matchingAnswers)
}

function currentRouteContext() {
  return {
    testId: testId.value,
    assignmentId: assignmentId.value,
  }
}

function isSameRouteContext(context) {
  return (
    context?.testId === testId.value &&
    context?.assignmentId === assignmentId.value
  )
}

function questionType(question) {
  return Number(
    question.type
  )
}

function questionText(question) {
  return (
    question.text ||
    question.question ||
    'Вопрос без текста'
  )
}

function questionTypeLabel(question) {
  const type =
    questionType(question)

  if (type === 1) {
    return 'Один вариант'
  }

  if (type === 2) {
    return 'Несколько вариантов'
  }

  if (type === 3) {
    return 'Сопоставление'
  }

  return 'Свободный ответ'
}

function optionText(option) {
  return (
    option.text ||
    option.label ||
    'Вариант ответа'
  )
}

function choiceOptions(question) {
  return Array.isArray(
    question.options
  )
    ? question.options
    : []
}

function hasChoiceOptions(question) {
  const type =
    questionType(question)

  return (
    (type === 1 || type === 2) &&
    choiceOptions(question).length > 0
  )
}

const answerState = computed(() => ({
  single: singleAnswers,
  multiple: multipleAnswers,
  text: textAnswers,
  matching: matchingAnswers,
}))

const progress = computed(() => (
  testAnswerProgress(
    questions.value,
    answerState.value
  )
))

function questionAnswered(question) {
  return isQuestionAnswered(
    question,
    answerState.value
  )
}

async function requestSubmit() {
  if (
    submitting.value ||
    submitted.value ||
    submitOutcomeUnknown.value ||
    !questions.value.length
  ) {
    return
  }

  if (progress.value.unanswered > 0) {
    showIncompleteSubmitDialog.value = true
    return
  }

  await submitTest()
}

async function confirmIncompleteSubmit() {
  showIncompleteSubmitDialog.value = false
  await submitTest()
}

const lectureRoute = computed(() => {
  const lectureId = Number(route.query.lectureId)

  if (!Number.isFinite(lectureId) || lectureId <= 0) {
    return null
  }

  return {
    name: 'lecture-details',
    params: { lectureId },
    query: {
      ...(route.query.subjectId
        ? { subjectId: route.query.subjectId }
        : {}),
      ...(route.query.facultyId
        ? { facultyId: route.query.facultyId }
        : {}),
    },
  }
})

function matchingPrompts(question) {
  return Array.isArray(
    question.matchingPrompts
  )
    ? question.matchingPrompts
    : []
}

function matchingOptions(question) {
  return Array.isArray(
    question.matchingOptions
  )
    ? question.matchingOptions
    : []
}

function initializeAnswers() {
  resetAnswers()

  questions.value.forEach(
    (question) => {
      const id =
        String(question.id)

      const type =
        questionType(
          question
        )

      if (
        type === 1 &&
        hasChoiceOptions(question)
      ) {
        singleAnswers[id] =
          null
        return
      }

      if (
        type === 2 &&
        hasChoiceOptions(question)
      ) {
        multipleAnswers[id] =
          []
        return
      }

      if (type === 3) {
        matchingAnswers[id] =
          matchingOptions(
            question
          ).map(() => '')
        return
      }

      textAnswers[id] = ''
    }
  )
}

function serializeMatchingAnswer(
  question
) {
  const prompts =
    matchingPrompts(
      question
    )

  const promptByOrdinal =
    new Map(
      prompts.map(
        (prompt) => [
          Number(
            prompt.ordinal
          ),

          prompt.text ||
          '',
        ]
      )
    )

  const selections =
    matchingAnswers[
      String(question.id)
    ] ?? []

  const pairs =
    matchingOptions(
      question
    ).map(
      (
        right,
        index
      ) => {
        const ordinal =
          Number(
            selections[
              index
            ] || 0
          )

        return {
          ordinal,

          left:
            promptByOrdinal.get(
              ordinal
            ) || '',

          right:
            String(
              right ?? ''
            ),
        }
      }
    )

  return JSON.stringify({
    pairs,
  })
}

function buildSubmission() {
  const questionIds = []
  const answers = []
  const selectedOptionIds = []

  questions.value.forEach(
    (question) => {
      const id =
        String(question.id)

      const type =
        questionType(
          question
        )

      questionIds.push(
        Number(question.id)
      )

      if (
        type === 1 &&
        hasChoiceOptions(question)
      ) {
        answers.push('')

        const selected =
          singleAnswers[id]

        selectedOptionIds.push(
          selected === null ||
          selected === undefined ||
          selected === ''
            ? []
            : [
                Number(
                  selected
                ),
              ]
        )

        return
      }

      if (
        type === 2 &&
        hasChoiceOptions(question)
      ) {
        answers.push('')

        selectedOptionIds.push(
          (
            multipleAnswers[
              id
            ] ?? []
          ).map(Number)
        )

        return
      }

      if (type === 3) {
        answers.push(
          serializeMatchingAnswer(
            question
          )
        )

        selectedOptionIds.push(
          []
        )

        return
      }

      answers.push(
        textAnswers[id] ??
        ''
      )

      selectedOptionIds.push(
        []
      )
    }
  )

  return {
    questionIds,
    answers,
    selectedOptionIds,
  }
}

function isMatchingSubmitDetail(detail) {
  return parseMatchingDisplay(
    detail?.givenAnswer
  ).length > 0
}

onBeforeRouteLeave(() => {
  flushScheduledDraftPersistence()
  leaveCurrentAttempt()
})

watch(
  () => [
    route.params.testId,
    route.query.assignmentId,
  ],
  () => {
    flushScheduledDraftPersistence()
    loadTest()
  }
)

onMounted(() => {
  loadTest()
})

onBeforeUnmount(() => {
  disposeAttemptLifecycle()
})
</script>

<template>
  <TestsPageShell
    :title="pageTitle"
    :subtitle="pageSubtitle"
    narrow
  >
    <template #actions>
      <UiButton
        variant="primary"
        :loading="submitting"
        loading-text="Отправка..."
        :disabled="
          loading ||
          submitted ||
          submitOutcomeUnknown ||
          !questions.length
        "
        @click="requestSubmit"
      >
        Завершить тест
      </UiButton>
    </template>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
    />

    <UiLoadingState
      v-if="loading"
      label="Загрузка теста..."
    />

    <UiEmptyState
      v-else-if="
        test &&
        !questions.length
      "
      description="В этом тесте пока нет вопросов."
    />

    <template v-else-if="questions.length">
      <section
        v-if="!submitted"
        class="test-progress"
        aria-label="Прогресс теста"
      >
        <div class="test-progress__copy">
          <span>Прогресс</span>
          <strong>{{ progress.answered }} из {{ progress.total }} отвечено</strong>
        </div>

        <div
          class="test-progress__track"
          role="progressbar"
          :aria-valuenow="progress.answered"
          aria-valuemin="0"
          :aria-valuemax="progress.total"
          :aria-label="`${progress.answered} из ${progress.total} вопросов отвечено`"
        >
          <span :style="{ width: `${progress.percent}%` }" />
        </div>
      </section>

      <div class="test-questions">
      <UiCard
        v-for="(question, index) in questions"
        :key="question.id"
        class="test-question-card"
        :class="{
          'test-question-card--answered': questionAnswered(question),
        }"
        compact
      >
        <div class="test-question">
          <div class="test-question__meta">
            <UiTag :value="`Вопрос ${index + 1}`" />
            <UiTag
              variant="info"
              :value="`${question.points ?? 0} балл.`"
            />
            <UiTag
              :value="questionTypeLabel(question)"
            />
            <UiTag
              v-if="!submitted"
              :variant="questionAnswered(question) ? 'success' : 'secondary'"
              :value="questionAnswered(question) ? 'Ответ дан' : 'Нет ответа'"
            />
          </div>

          <h2 class="test-question__title">
            {{
              questionText(
                question
              )
            }}
          </h2>

          <div
            v-if="
              questionType(question) === 1 &&
              hasChoiceOptions(question)
            "
            class="test-question__options"
          >
            <UiRadio
              v-for="option in choiceOptions(question)"
              :key="option.id"
              v-model="
                singleAnswers[
                  String(question.id)
                ]
              "
              :id="
                `question-${question.id}-option-${option.id}`
              "
              :value="option.id"
              :name="
                `question-${question.id}`
              "
              :label="
                optionText(option)
              "
              :disabled="submitted"
            />
          </div>

          <div
            v-else-if="
              questionType(question) === 2 &&
              hasChoiceOptions(question)
            "
            class="test-question__options"
          >
            <UiCheckbox
              mode="multiple"
              v-for="option in choiceOptions(question)"
              :key="option.id"
              v-model="
                multipleAnswers[
                  String(question.id)
                ]
              "
              :id="
                `question-${question.id}-option-${option.id}`
              "
              :value="option.id"
              :label="
                optionText(option)
              "
              :disabled="submitted"
            />
          </div>

          <div
            v-else-if="
              questionType(question) === 3
            "
          >
            <UiAlert
              v-if="
                !matchingPrompts(question).length ||
                !matchingOptions(question).length
              "
              variant="danger"
              message="Для вопроса на сопоставление не заданы обе колонки."
            />

            <StudentMatchingQuestion
              v-else
              v-model="
                matchingAnswers[
                  String(question.id)
                ]
              "
              :prompts="matchingPrompts(question)"
              :options="matchingOptions(question)"
              :disabled="submitted"
            />
          </div>

          <UiInput
            v-else
            v-model="
              textAnswers[
                String(question.id)
              ]
            "
            label="Ваш ответ"
            placeholder="Введите ответ"
            :disabled="submitted"
          />
        </div>
      </UiCard>

      </div>

      <div
        v-if="!submitted"
        class="test-submit-bar"
      >
        <div class="test-submit-bar__copy">
          <strong>{{ progress.answered }} из {{ progress.total }} отвечено</strong>
          <span v-if="progress.unanswered">
            Без ответа: {{ progress.unanswered }}
          </span>
          <span v-else>Все вопросы заполнены</span>
        </div>

        <UiButton
          variant="primary"
          size="lg"
          :loading="submitting"
          loading-text="Отправка ответов..."
          :disabled="submitOutcomeUnknown"
          @click="requestSubmit"
        >
          Завершить тест
        </UiButton>
      </div>
    </template>

    <section
      v-if="resultData"
      id="test-result"
      class="test-result"
    >
      <UiCard title="Результат">
        <UiAlert
          variant="success"
          message="Тест завершён. Результат сохранён."
        />

        <UiStatGrid class="test-result__summary">
          <UiStat
            label="Правильных ответов"
            :value="`${resultData.correctCount ?? 0} из ${resultData.totalCount ?? 0}`"
          />
          <UiStat
            label="Итоговый балл"
            :value="resultData.score ?? 0"
          />
        </UiStatGrid>

        <div class="test-result__actions">
          <UiButton :to="{ name: 'results' }" variant="primary">
            К моим результатам
          </UiButton>
          <UiButton
            v-if="lectureRoute"
            :to="lectureRoute"
            variant="secondary"
          >
            Вернуться к лекции
          </UiButton>
        </div>

        <div
          v-if="
            Array.isArray(
              resultData.details
            ) &&
            resultData.details.length
          "
          class="test-result__details"
        >
          <article
            v-for="(detail, index) in resultData.details"
            :key="
              detail.questionId ??
              index
            "
            class="test-result-detail"
          >
            <div class="test-result-detail__header">
              <strong>
                {{
                  detail.questionText ||
                  `Вопрос ${index + 1}`
                }}
              </strong>
            </div>

            <dl class="test-result-detail__data">
              <div>
                <dt>Ваш ответ</dt>

                <dd>
                  <ResultMatchingPairs
                    v-if="isMatchingSubmitDetail(detail)"
                    :given-answer="detail.givenAnswer"
                  />

                  <template v-else>
                    {{
                      detail.givenAnswer ||
                      '—'
                    }}
                  </template>
                </dd>
              </div>
            </dl>
          </article>
        </div>
      </UiCard>
    </section>

    <UiDialog
      v-model="showIncompleteSubmitDialog"
      title="Есть вопросы без ответа"
      width="30rem"
    >
      <p class="test-submit-dialog__text">
        Без ответа осталось: <strong>{{ progress.unanswered }}</strong>.
        Можно вернуться к вопросам или завершить тест сейчас.
      </p>

      <template #footer>
        <div class="test-submit-dialog__actions">
          <UiButton
            variant="secondary"
            @click="showIncompleteSubmitDialog = false"
          >
            Вернуться к вопросам
          </UiButton>
          <UiButton
            variant="primary"
            :loading="submitting"
            loading-text="Отправка..."
            @click="confirmIncompleteSubmit"
          >
            Всё равно завершить
          </UiButton>
        </div>
      </template>
    </UiDialog>
  </TestsPageShell>
</template>

<style scoped>
.test-questions {
  display: grid;
  gap: 14px;
}


.test-progress {
  padding: 14px 16px;

  display: grid;
  gap: 10px;

  background: var(--st-surface);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
}

.test-progress__copy {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.test-progress__copy span {
  color: var(--st-text-secondary);
  font-size: var(--st-font-sm);
}

.test-progress__track {
  height: 8px;
  overflow: hidden;

  background: var(--st-surface-muted);
  border-radius: 999px;
}

.test-progress__track span {
  display: block;
  height: 100%;

  background: var(--st-primary);
  border-radius: inherit;
  transition: width 160ms ease;
}

.test-question-card {
  scroll-margin-top: 96px;
}

.test-question-card--answered {
  border-color: color-mix(in srgb, var(--st-success) 32%, var(--st-border));
}

.test-submit-bar {
  position: sticky;
  bottom: 12px;
  z-index: 5;

  padding: 12px 14px;

  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  background: color-mix(in srgb, var(--st-surface) 94%, transparent);
  border: 1px solid var(--st-border);
  border-radius: var(--st-radius-card);
  box-shadow: var(--st-shadow-elevated);
  backdrop-filter: blur(12px);
}

.test-submit-bar__copy {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.test-submit-bar__copy span {
  color: var(--st-text-secondary);
  font-size: var(--st-font-sm);
}

.test-result__summary {
  margin-top: 14px;
}

.test-result__actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.test-submit-dialog__text {
  margin: 0;
  color: var(--st-text-secondary);
  line-height: var(--st-line-relaxed);
}

.test-submit-dialog__actions {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.test-question {
  display: grid;
  gap: 15px;
}

.test-question__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
}


.test-question__title {
  margin: 0;

  color: var(--st-text);

  font-size: var(--st-font-lg);
  line-height: var(--st-line-normal);
}

.test-question__options {
  display: grid;
  gap: 8px;
}

.test-result {
  scroll-margin-top: 18px;
}

.test-result__details {
  margin-top: 14px;

  display: grid;
  gap: 10px;
}

.test-result-detail {
  padding: 13px;

  color: var(--st-text);
  background: var(--st-surface-muted);

  border: 1px solid var(--st-border);
  border-radius: 9px;
}

.test-result-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}


.test-result-detail__data {
  margin: 10px 0 0;

  display: grid;
  gap: 8px;
}

.test-result-detail__data > div {
  display: grid;
  gap: 3px;
}

.test-result-detail__data dt {
  color: var(--st-text-secondary);

  font-size: var(--st-font-xs);
  font-weight: 700;
}

.test-result-detail__data dd {
  margin: 0;

  overflow-wrap: anywhere;

  font-size: var(--st-font-sm);
  line-height: var(--st-line-normal);
}

@media (max-width: 560px) {
  .test-progress__copy,
  .test-submit-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .test-submit-bar {
    bottom: max(8px, env(safe-area-inset-bottom));
  }

  .test-submit-bar :deep(.st-ui-button),
  .test-result__actions :deep(.st-ui-button),
  .test-result__actions :deep(.st-ui-link-button),
  .test-submit-dialog__actions :deep(.st-ui-button) {
    width: 100%;
  }

  .test-result-detail__header {
    flex-direction: column;
  }
}
</style>
