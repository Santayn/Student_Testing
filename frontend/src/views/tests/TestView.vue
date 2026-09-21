<script setup>
import {
  computed,
  nextTick,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'

import {
  onBeforeRouteLeave,
  useRoute,
  useRouter,
} from 'vue-router'

import {
  getApiErrorMessage,
  learningApi,
} from '@/api'

import TestsPageShell from '@/components/tests/TestsPageShell.vue'

import {
  clearCompletedTestSession,
  readCompletedTestSession,
  saveCompletedTestSession,
} from '@/utils/completedTestSession'

import {
  clearTestAttemptDraft,
  readTestAttemptDraft,
  saveTestAttemptDraft,
} from '@/utils/testAttemptDraft'

import {
  sanitizeStudentSubmitResult,
} from '@/utils/resultContracts'

import {
  createLatestRequestGuard,
} from '@/utils/latestRequest'

import {
  UiAlert,
  UiButton,
  UiCard,
  UiCheckbox,
  UiEmptyState,
  UiInput,
  UiRadio,
  UiSelect,
} from '@/components/ui'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const submitOutcomeUnknown = ref(false)

const error = ref('')

const test = ref(null)
const questions = ref([])

const attemptId = ref(null)
const resultData = ref(null)

const loadRequest = createLatestRequestGuard()
const submitRequest = createLatestRequestGuard()

let draftPersistencePaused = false

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

const submitted = computed(() => {
  return resultData.value !== null
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

function resetTestState() {
  submitOutcomeUnknown.value = false
  test.value = null
  questions.value = []
  attemptId.value = null
  resultData.value = null
  resetAnswers()
}

function assertStartAttemptContext(data, context) {
  const responseAssignmentId =
    Number(data?.assignmentId)

  const responseTestId =
    Number(data?.test?.id)

  const nestedAssignmentId =
    data?.test?.assignmentId == null
      ? null
      : Number(data.test.assignmentId)

  const responseAttemptId =
    Number(data?.attemptId)

  if (
    !Number.isFinite(responseAttemptId) ||
    responseAttemptId <= 0 ||
    responseAssignmentId !== context.assignmentId ||
    responseTestId !== context.testId ||
    (
      nestedAssignmentId !== null &&
      nestedAssignmentId !== responseAssignmentId
    )
  ) {
    throw new Error(
      'Backend вернул попытку, которая не соответствует открытому тесту.'
    )
  }
}


function hasUnknownRequestOutcome(errorValue) {
  if (!errorValue) {
    return false
  }

  if (
    errorValue.code === 'ECONNABORTED' ||
    errorValue.code === 'ERR_NETWORK'
  ) {
    return true
  }

  const looksLikeAxiosError =
    errorValue.isAxiosError === true ||
    Boolean(errorValue.config)

  return (
    looksLikeAxiosError &&
    !errorValue.response
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

function matchingOrdinalOptions(
  question
) {
  return matchingPrompts(
    question
  ).map(
    (prompt) => ({
      label:
        String(
          prompt.ordinal
        ),

      value:
        Number(
          prompt.ordinal
        ),
    })
  )
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

function applyDraftAnswers(draft) {
  if (!draft) {
    return
  }

  Object.entries(
    draft.singleAnswers ?? {}
  ).forEach(([id, value]) => {
    singleAnswers[id] = value
  })

  Object.entries(
    draft.multipleAnswers ?? {}
  ).forEach(([id, value]) => {
    multipleAnswers[id] =
      Array.isArray(value)
        ? [...value]
        : []
  })

  Object.entries(
    draft.textAnswers ?? {}
  ).forEach(([id, value]) => {
    textAnswers[id] =
      String(value ?? '')
  })

  Object.entries(
    draft.matchingAnswers ?? {}
  ).forEach(([id, value]) => {
    matchingAnswers[id] =
      Array.isArray(value)
        ? [...value]
        : []
  })
}

function restoreCurrentAttemptDraft(
  context
) {
  const draft =
    readTestAttemptDraft({
      testId: context.testId,
      assignmentId:
        context.assignmentId,
      attemptId:
        attemptId.value,
      questions:
        questions.value,
    })

  applyDraftAnswers(draft)
}

function persistCurrentAttemptDraft() {
  if (
    draftPersistencePaused ||
    submitted.value ||
    !attemptId.value ||
    !questions.value.length
  ) {
    return
  }

  const context =
    currentRouteContext()

  if (
    !context.testId ||
    !context.assignmentId
  ) {
    return
  }

  saveTestAttemptDraft({
    testId: context.testId,
    assignmentId:
      context.assignmentId,
    attemptId:
      attemptId.value,
    questions:
      questions.value,
    singleAnswers,
    multipleAnswers,
    textAnswers,
    matchingAnswers,
  })
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

async function loadTest() {
  const context =
    currentRouteContext()

  const requestId =
    loadRequest.begin()

  submitRequest.invalidate()
  submitting.value = false

  if (!context.assignmentId || !context.testId) {
    resetTestState()
    loading.value = false
    error.value =
      'Не указано назначение теста. Откройте тест со страницы лекции.'

    return
  }

  loading.value = true
  error.value = ''
  resetTestState()

  const completedSession =
    readCompletedTestSession(
      context.testId,
      context.assignmentId
    )

  if (completedSession) {
    if (
      !loadRequest.isCurrent(requestId) ||
      !isSameRouteContext(context)
    ) {
      return
    }

    clearTestAttemptDraft(
      context.testId,
      context.assignmentId
    )

    attemptId.value =
      completedSession.attemptId

    test.value =
      completedSession.test ??
      null

    questions.value = []
    resultData.value =
      sanitizeStudentSubmitResult(
        completedSession.resultData
      )

    resetAnswers()
    loading.value = false
    return
  }

  try {
    const response =
      await learningApi
        .startAttempt(
          context.assignmentId
        )

    if (
      !loadRequest.isCurrent(requestId) ||
      !isSameRouteContext(context)
    ) {
      return
    }

    const data =
      response.data ?? {}

    assertStartAttemptContext(
      data,
      context
    )

    attemptId.value =
      Number(data.attemptId)

    test.value =
      data.test ??
      null

    questions.value =
      Array.isArray(
        data.questions
      )
        ? data.questions
        : []

    draftPersistencePaused = true

    try {
      initializeAnswers()
      restoreCurrentAttemptDraft(
        context
      )
    } finally {
      draftPersistencePaused = false
    }
  } catch (requestError) {
    if (
      !loadRequest.isCurrent(requestId) ||
      !isSameRouteContext(context)
    ) {
      return
    }

    resetTestState()

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось загрузить тест.'
      )
  } finally {
    if (
      loadRequest.isCurrent(requestId) &&
      isSameRouteContext(context)
    ) {
      loading.value = false
    }
  }
}

async function submitTest() {
  if (
    submitting.value ||
    submitOutcomeUnknown.value ||
    submitted.value ||
    !questions.value.length ||
    !attemptId.value
  ) {
    return
  }

  const context = {
    ...currentRouteContext(),
    attemptId: Number(attemptId.value),
  }

  if (
    !context.testId ||
    !context.assignmentId ||
    !Number.isFinite(context.attemptId) ||
    context.attemptId <= 0
  ) {
    error.value =
      'Контекст попытки устарел. Откройте тест заново со страницы лекции.'
    return
  }

  const requestId =
    submitRequest.begin()

  submitting.value = true
  error.value = ''

  try {
    const payload =
      buildSubmission()

    const response =
      await learningApi
        .submitAttempt(
          context.attemptId,
          payload
        )

    if (
      !submitRequest.isCurrent(requestId) ||
      !isSameRouteContext(context) ||
      Number(attemptId.value) !== context.attemptId
    ) {
      return
    }

    resultData.value =
      sanitizeStudentSubmitResult(
        response.data
      )

    clearTestAttemptDraft(
      context.testId,
      context.assignmentId
    )

    saveCompletedTestSession({
      testId: context.testId,
      assignmentId: context.assignmentId,
      attemptId: context.attemptId,
      test: test.value,
      resultData: resultData.value,
    })

    await nextTick()

    if (
      !submitRequest.isCurrent(requestId) ||
      !isSameRouteContext(context) ||
      Number(attemptId.value) !== context.attemptId
    ) {
      return
    }

    document
      .getElementById(
        'test-result'
      )
      ?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
  } catch (requestError) {
    if (
      !submitRequest.isCurrent(requestId) ||
      !isSameRouteContext(context) ||
      Number(attemptId.value) !== context.attemptId
    ) {
      return
    }

    if (hasUnknownRequestOutcome(requestError)) {
      submitOutcomeUnknown.value = true
      error.value =
        'Связь с сервером прервалась во время отправки. ' +
        'Результат попытки неизвестен, поэтому повторная отправка заблокирована. ' +
        'Откройте результаты или вернитесь к тесту позже, чтобы не отправить попытку повторно.'
      return
    }

    error.value =
      getApiErrorMessage(
        requestError,
        'Не удалось отправить ответы на тест.'
      )
  } finally {
    if (
      submitRequest.isCurrent(requestId) &&
      isSameRouteContext(context) &&
      Number(attemptId.value) === context.attemptId
    ) {
      submitting.value = false
    }
  }
}

function goBack() {
  router.back()
}

onBeforeRouteLeave(() => {
  loadRequest.invalidate()
  submitRequest.invalidate()

  if (testId.value && assignmentId.value) {
    clearCompletedTestSession(
      testId.value,
      assignmentId.value
    )
  }
})

watch(
  singleAnswers,
  persistCurrentAttemptDraft,
  { deep: true, flush: 'sync' }
)

watch(
  multipleAnswers,
  persistCurrentAttemptDraft,
  { deep: true, flush: 'sync' }
)

watch(
  textAnswers,
  persistCurrentAttemptDraft,
  { deep: true, flush: 'sync' }
)

watch(
  matchingAnswers,
  persistCurrentAttemptDraft,
  { deep: true, flush: 'sync' }
)

watch(
  () => [
    route.params.testId,
    route.query.assignmentId,
  ],
  loadTest
)

onMounted(loadTest)
</script>

<template>
  <TestsPageShell
    :title="pageTitle"
    :subtitle="pageSubtitle"
  >
    <template #actions>
      <UiButton
        @click="goBack"
      >
        Назад
      </UiButton>

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
            <span>
              Вопрос
              {{ index + 1 }}
            </span>

            <span>
              {{
                question.points ??
                0
              }}
              балл.
            </span>

            <span>
              {{
                questionTypeLabel(
                  question
                )
              }}
            </span>
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
            class="test-matching"
          >
            <UiAlert
              v-if="
                !matchingPrompts(question).length ||
                !matchingOptions(question).length
              "
              variant="danger"
              message="Для вопроса на сопоставление не заданы обе колонки."
            />

            <template v-else>
              <section class="test-matching__column">
                <h3 class="test-matching__title">
                  Колонка А
                </h3>

                <ol class="test-matching__list">
                  <li
                    v-for="prompt in matchingPrompts(question)"
                    :key="prompt.ordinal"
                    class="test-matching__row"
                  >
                    <span class="test-matching__number">
                      {{ prompt.ordinal }}
                    </span>

                    <span>
                      {{ prompt.text }}
                    </span>
                  </li>
                </ol>
              </section>

              <section class="test-matching__column">
                <h3 class="test-matching__title">
                  Колонка Б
                </h3>

                <ul class="test-matching__list">
                  <li
                    v-for="(option, optionIndex) in matchingOptions(question)"
                    :key="
                      `${question.id}-${optionIndex}`
                    "
                    class="test-matching__row"
                  >
                    <UiSelect
                      v-model="
                        matchingAnswers[
                          String(question.id)
                        ][optionIndex]
                      "
                      class="test-matching__select"
                      placeholder="№"
                      :options="
                        matchingOrdinalOptions(
                          question
                        )
                      "
                      option-label="label"
                      option-value="value"
                      size="sm"
                      :disabled="submitted"
                      :aria-label="
                        `Номер соответствия для варианта ${optionIndex + 1}`
                      "
                    />

                    <span>
                      {{ option }}
                    </span>
                  </li>
                </ul>

                <p class="test-matching__hint">
                  Укажите возле каждого варианта
                  из колонки Б номер подходящего
                  элемента из колонки А.
                </p>
              </section>
            </template>
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
                  {{
                    detail.givenAnswer ||
                    '—'
                  }}
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

.test-question__meta span {
  padding: 4px 7px;

  color: var(--text-secondary);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 999px;

  font-size: 11px;
  font-weight: 700;
}

.test-question__title {
  margin: 0;

  color: var(--text);

  font-size: 17px;
  line-height: 1.45;
}

.test-question__options {
  display: grid;
  gap: 8px;
}

.test-matching {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.test-matching__column {
  padding: 14px;

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
  border-radius: 10px;
}

.test-matching__title {
  margin: 0 0 10px;

  font-size: 14px;
}

.test-matching__list {
  margin: 0;
  padding: 0;

  display: grid;
  gap: 9px;

  list-style: none;
}

.test-matching__row {
  min-height: 40px;

  display: grid;
  grid-template-columns:
    58px
    minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.test-matching__number {
  width: 34px;
  height: 34px;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  color: var(--text);
  background: var(--surface);

  border: 1px solid var(--border);
  border-radius: 999px;

  font-weight: 800;
}

.test-matching__select {
  width: 58px;
  min-width: 58px;
}

.test-matching__hint {
  margin: 10px 0 0;

  color: var(--text-secondary);

  font-size: 12px;
  line-height: 1.45;
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

  color: var(--text);
  background: var(--surface-secondary);

  border: 1px solid var(--border);
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
  color: var(--text-secondary);

  font-size: 11px;
  font-weight: 700;
}

.test-result-detail__data dd {
  margin: 0;

  overflow-wrap: anywhere;

  font-size: 13px;
  line-height: 1.45;
}

@media (max-width: 760px) {
  .test-matching {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .test-result-detail__header {
    flex-direction: column;
  }
}
</style>
