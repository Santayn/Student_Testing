import {
  describe,
  expect,
  it,
  vi,
} from 'vitest'
import { ref } from 'vue'

import {
  useTestEditorSaveFlow,
} from '@/composables/tests/useTestEditorSaveFlow'
import {
  TestCreationFlowError,
} from '@/utils/createTestWithAssignments'

function fixture(overrides = {}) {
  const form = ref({
    title: '  Итоговый тест  ',
    description: ' Описание ',
    attemptsAllowed: 2,
    questionCount: 5,
    textQuestionCount: 1,
    singleAnswerQuestionCount: 2,
    multipleAnswerQuestionCount: 1,
    matchingQuestionCount: 1,
    status: 2,
    availableFrom: '2026-09-28T10:00',
    availableUntil: '2026-10-28T10:00',
  })
  const selectedTopicId = ref('30')
  const selectedGroupIds = ref([2, 3])
  const groupTargets = ref([
    {
      groupId: 2,
      assignmentIds: [101, 102],
    },
    {
      groupId: 3,
      assignmentIds: [102, 103],
    },
  ])
  const notice = ref({ type: 'info', message: '' })
  const validationError = vi.fn(() => '')
  const ensureSelectedMembershipActive = vi.fn()
    .mockResolvedValue({ id: 10 })
  const markEditorClean = vi.fn()
  const api = {
    create: vi.fn(),
    createAssignments: vi.fn(),
    delete: vi.fn(),
  }
  const createFlow = vi.fn().mockResolvedValue({
    test: { id: 77 },
  })

  const flow = useTestEditorSaveFlow({
    form,
    selectedTopicId,
    selectedGroupIds,
    groupTargets,
    validationError,
    ensureSelectedMembershipActive,
    markEditorClean,
    notice,
    api,
    createFlow,
    errorMessage: (_error, fallback) => fallback,
    ...overrides,
  })

  return {
    flow,
    form,
    selectedTopicId,
    selectedGroupIds,
    groupTargets,
    notice,
    validationError,
    ensureSelectedMembershipActive,
    markEditorClean,
    api,
    createFlow,
  }
}

describe('useTestEditorSaveFlow', () => {
  it('revalidates membership and creates one assignment per unique target', async () => {
    const f = fixture()

    await f.flow.createTest()

    expect(
      f.ensureSelectedMembershipActive
    ).toHaveBeenCalledTimes(1)

    expect(f.createFlow).toHaveBeenCalledTimes(1)
    const args = f.createFlow.mock.calls[0][0]

    expect(args.assignmentIds).toEqual([
      101,
      102,
      103,
    ])
    expect(args.testPayload).toMatchObject({
      title: 'Итоговый тест',
      description: 'Описание',
      attemptsAllowed: 2,
      questionCount: 5,
    })
    expect(args.testPayload.selectionRules[0]).toMatchObject({
      topicId: 30,
      questionCount: 5,
      textQuestionCount: 1,
      singleAnswerQuestionCount: 2,
      multipleAnswerQuestionCount: 1,
      matchingQuestionCount: 1,
    })

    expect(f.notice.value).toEqual({
      type: 'success',
      message:
        'Тест #77 создан и назначен выбранным группам.',
    })
    expect(f.markEditorClean).toHaveBeenCalledTimes(1)
    expect(f.flow.saving.value).toBe(false)
  })

  it('does not mutate when validation fails', async () => {
    const f = fixture({
      validationError: vi.fn(() => 'Ошибка формы'),
    })

    await f.flow.createTest()

    expect(f.createFlow).not.toHaveBeenCalled()
    expect(
      f.ensureSelectedMembershipActive
    ).not.toHaveBeenCalled()
    expect(f.notice.value).toEqual({
      type: 'danger',
      message: 'Ошибка формы',
    })
  })

  it('does not mark editor clean after rolled-back assignment failure', async () => {
    const cause = new Error('assignment failed')
    const f = fixture({
      createFlow: vi.fn().mockRejectedValue(
        new TestCreationFlowError({
          cause,
          testId: 77,
          createdAssignmentCount: 1,
          rollbackSucceeded: true,
        })
      ),
    })

    await f.flow.createTest()

    expect(f.markEditorClean).not.toHaveBeenCalled()
    expect(f.notice.value.type).toBe('danger')
    expect(f.notice.value.message).toContain(
      'Создание теста отменено'
    )
  })

  it('surfaces the unfinished test id when rollback fails', async () => {
    const cause = new Error('assignment failed')
    const f = fixture({
      createFlow: vi.fn().mockRejectedValue(
        new TestCreationFlowError({
          cause,
          testId: 88,
          createdAssignmentCount: 0,
          rollbackSucceeded: false,
          rollbackError: new Error('delete failed'),
        })
      ),
    })

    await f.flow.createTest()

    expect(f.markEditorClean).not.toHaveBeenCalled()
    expect(f.notice.value.message).toContain('тест #88')
    expect(f.notice.value.message).toContain(
      'Не создавайте тест повторно'
    )
  })
})
