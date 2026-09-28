<script setup>
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
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
  useAttemptDraft,
} from '@/composables/useAttemptDraft'

import {
  useTestAttemptLifecycle,
} from '@/composables/useTestAttemptLifecycle'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiEmptyState,
  UiInput,
  UiRadio,
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
        ? `Тест #${testId.value}`
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
    `попыток: ${
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
    `Вопрос #${question.id}`
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
    `Вариант #${option.id}`
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
        @click="submitTest"
      >
        Завершить тест
      </UiButton>
    </template>

    <UiAlert
      v-if="error"
      variant="danger"
      :message="error"
    />

    <UiEmptyState
      v-if="loading"
      description="Загрузка теста..."
    />

    <UiEmptyState
      v-else-if="
        test &&
        !questions.length
      "
      description="В этом тесте пока нет вопросов."
    />

    <div
      v-else-if="questions.length"
      class="test-questions"
    >
      <UiCard
        v-for="(question, index) in questions"
        :key="question.id"
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

      <UiButton
        variant="primary"
        size="lg"
        block
        :loading="submitting"
        loading-text="Отправка ответов..."
        :disabled="submitted || submitOutcomeUnknown"
        @click="submitTest"
      >
        Завершить тест
      </UiButton>
    </div>

    <section
      v-if="resultData"
      id="test-result"
      class="test-result"
    >
      <UiCard title="Результат">
        <UiAlert
          variant="success"
          :message="
            `Правильных ответов: ${resultData.correctCount ?? 0} ` +
            `из ${resultData.totalCount ?? 0}, ` +
            `итоговый балл: ${resultData.score ?? 0}.`
          "
        />

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
  </TestsPageShell>
</template>

<style scoped>
.test-questions {
  display: grid;
  gap: 14px;
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

  font-size: 17px;
  line-height: 1.45;
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

  font-size: 11px;
  font-weight: 700;
}

.test-result-detail__data dd {
  margin: 0;

  overflow-wrap: anywhere;

  font-size: 13px;
  line-height: 1.45;
}

@media (max-width: 560px) {
  .test-result-detail__header {
    flex-direction: column;
  }
}
</style>
