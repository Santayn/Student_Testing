import {
  ref,
} from 'vue'

import {
  getApiErrorMessage,
  testsApi,
} from '@/api'

import {
  createTestWithAssignments,
  TestCreationFlowError,
} from '@/utils/createTestWithAssignments'

export function useTestEditorSaveFlow({
  form,
  selectedTopicId,
  selectedGroupIds,
  groupTargets,
  validationError,
  ensureSelectedMembershipActive,
  markEditorClean,
  notice,
  api = testsApi,
  createFlow = createTestWithAssignments,
  errorMessage = getApiErrorMessage,
}) {
  const saving = ref(false)

  async function createTest() {
    const validationMessage =
      validationError()

    if (validationMessage) {
      notice.value = {
        type: 'danger',
        message: validationMessage,
      }
      return
    }

    const assignmentIds = [
      ...new Set(
        groupTargets.value
          .filter(
            (target) =>
              selectedGroupIds.value.some(
                (id) =>
                  Number(id) ===
                  Number(target.groupId)
              )
          )
          .flatMap(
            (target) =>
              target.assignmentIds
          )
      ),
    ]

    if (!assignmentIds.length) {
      notice.value = {
        type: 'danger',
        message:
          'Не удалось определить учебные назначения для выбранных групп.',
      }
      return
    }

    saving.value = true

    const availableFromUtc =
      new Date(
        form.value.availableFrom
      ).toISOString()

    const availableUntilUtc =
      new Date(
        form.value.availableUntil
      ).toISOString()

    const testPayload = {
      title: form.value.title.trim(),
      description:
        form.value.description.trim() ||
        null,
      duration: null,
      attemptsAllowed:
        Number(
          form.value.attemptsAllowed
        ) || 1,
      questionCount:
        Number(
          form.value.questionCount
        ),
      selectionRules: [
        {
          courseLectureId: null,
          topicId:
            Number(
              selectedTopicId.value
            ),
          questionCount:
            Number(
              form.value.questionCount
            ),
          textQuestionCount:
            Number(
              form.value
                .textQuestionCount
            ),
          singleAnswerQuestionCount:
            Number(
              form.value
                .singleAnswerQuestionCount
            ),
          multipleAnswerQuestionCount:
            Number(
              form.value
                .multipleAnswerQuestionCount
            ),
          matchingQuestionCount:
            Number(
              form.value
                .matchingQuestionCount
            ),
          ordinal: 1,
        },
      ],
    }

    try {
      await ensureSelectedMembershipActive()

      const { test } =
        await createFlow({
          createTest: (payload) =>
            api.create(payload),
          createAssignment: (
            testId,
            payload
          ) =>
            api.createAssignments(
              testId,
              payload
            ),
          deleteTest: (testId) =>
            api.delete(testId),
          testPayload,
          assignmentIds,
          assignmentPayload: (
            assignmentId
          ) => ({
            scope: 4,
            courseVersionId: null,
            courseLectureId: null,
            teachingAssignmentId:
              Number(assignmentId),
            availableFromUtc,
            availableUntilUtc,
            status:
              Number(form.value.status),
          }),
        })

      notice.value = {
        type: 'success',
        message:
          `Тест #${test.id} создан и назначен выбранным группам.`,
      }

      markEditorClean()
    } catch (error) {
      if (
        error instanceof
        TestCreationFlowError
      ) {
        const assignmentMessage =
          errorMessage(
            error.cause,
            'Не удалось создать назначение теста'
          )

        if (error.rollbackSucceeded) {
          notice.value = {
            type: 'danger',
            message:
              `${assignmentMessage}. Создание теста отменено, частичные назначения удалены.`,
          }
        } else {
          notice.value = {
            type: 'danger',
            message:
              `${assignmentMessage}. Не удалось автоматически удалить незавершённый тест #${error.testId}. Не создавайте тест повторно, пока тест #${error.testId} не будет удалён вручную.`,
          }
        }
      } else {
        notice.value = {
          type: 'danger',
          message: errorMessage(
            error,
            'Не удалось создать тест'
          ),
        }
      }
    } finally {
      saving.value = false
    }
  }

  return {
    saving,
    createTest,
  }
}
